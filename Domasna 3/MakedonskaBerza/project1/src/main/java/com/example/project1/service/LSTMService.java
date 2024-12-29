package com.example.project1.service;

import com.example.project1.model.StockRecordEntity;
import com.example.project1.repository.StockRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LSTMService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final StockRecordRepository stockRecordRepository;

    private final String predictionApiUrl = "http://127.0.0.1:8080/predict-next-month-price/";
    private final String predictionApiUrl2 = "http://127.0.0.1:5000/generate_signal";

    public Double predictNextMonth(Long companyId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<StockRecordEntity> data = stockRecordRepository.findByCompanyIdAndDateBetween(companyId, LocalDate.now().minusMonths(1), LocalDate.now());

        Map<String, Object> requestBody = Map.of("data", mapToRequestData(data));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        Map<String, Double> response = restTemplate.postForObject(predictionApiUrl, requestEntity, Map.class);

        System.out.println("Response:" + response);

        return response != null ? response.get("predicted_next_month_price") : null;
    }

    public String predictIndicatorsAndSignals(Long companyId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<StockRecordEntity> data = stockRecordRepository.findByCompanyId(companyId);

        List<Map<String, Object>> requestBody = new ArrayList<>();
        for (StockRecordEntity s : data){
            Map<String, Object> record = new HashMap<>();
            record.put("date", s.getDate().toString());
            record.put("open", (s.getMaxPrice() + s.getMinPrice()) / 2.0);
            record.put("close", s.getLastTransactionPrice());
            record.put("high", s.getMaxPrice());
            record.put("low", s.getMinPrice());
            record.put("volume", s.getQuantity());
            requestBody.add(record);
        }

        // Create the HttpEntity with the headers and the body
        HttpEntity<List<Map<String, Object>>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Make a POST request to the FastAPI endpoint
        ResponseEntity<Map> response = restTemplate.exchange(predictionApiUrl2, HttpMethod.POST, requestEntity, Map.class);

        Map<String, Object> responseBody = response.getBody();

        System.out.println("Response:" + responseBody);

        if (responseBody != null && responseBody.containsKey("signal_result")) {
            return responseBody.get("signal_result").toString();
        } else {
            throw new RuntimeException("Failed to retrieve a valid signal from the Python API.");
        }
    }

    public static List<Map<String, Object>> mapToRequestData(List<StockRecordEntity> historicalDataEntities) {
        return historicalDataEntities.stream().map(entity -> {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("date", entity.getDate().toString());
            dataMap.put("average_price", entity.getAveragePrice());
            return dataMap;
        }).collect(Collectors.toList());
    }
}
