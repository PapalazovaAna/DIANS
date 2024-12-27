package com.example.project1.service;

import com.example.project1.model.StockRecordEntity;
import com.example.project1.repository.StockRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LSTMService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final StockRecordRepository stockRecordRepository;

    private final String predictionApiUrl = "http://127.0.0.1:8000/predict-next-month-price/";
    private final String predictionApiUrl2 = "http://127.0.0.1:8000/predict-indicators-and-signals/";

    public Double predictNextMonth(Long companyId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<StockRecordEntity> data = stockRecordRepository.findByCompanyIdAndDateBetween(companyId, LocalDate.now().minusMonths(1), LocalDate.now());

        Map<String, Object> requestBody = Map.of("data", mapToRequestData(data));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        Map<String, Double> response = restTemplate.postForObject(predictionApiUrl, requestEntity, Map.class);

        return response != null ? response.get("predicted_next_month_price") : null;
    }

    public Map<String, Object> predictIndicatorsAndSignals(Long companyId, int indicatorId, int days) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Get historical stock data for the last month (or any other period you need)
        List<StockRecordEntity> data = stockRecordRepository.findByCompanyIdAndDateBetween(companyId, LocalDate.now().minusDays(days), LocalDate.now());

        // Map the historical data to the format FastAPI expects
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", mapToRequestData(data));
        requestBody.put("indicator_id", indicatorId);
        requestBody.put("days", days);

        // Create the HttpEntity with the headers and the body
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Make a POST request to the FastAPI endpoint
        Map<String, Object> response = restTemplate.postForObject(predictionApiUrl2, requestEntity, Map.class);

        return response != null ? response : null;
    }

    public static List<Map<String, Object>> mapToRequestData(List<StockRecordEntity> historicalDataEntities) {
        return historicalDataEntities.stream().map(entity -> {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("date", entity.getDate().toString());
            dataMap.put("average_price", entity.getAveragePrice());
            dataMap.put("last_transaction_price", entity.getLastTransactionPrice());
            dataMap.put("max_price", entity.getMaxPrice());
            dataMap.put("min_price", entity.getMinPrice());
            dataMap.put("quantity", entity.getQuantity());
            return dataMap;
        }).collect(Collectors.toList());
    }
}
