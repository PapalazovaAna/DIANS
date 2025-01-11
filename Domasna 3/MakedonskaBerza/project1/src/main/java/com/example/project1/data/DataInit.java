package com.example.project1.data;

import com.example.project1.model.StockEntity;
import com.example.project1.repository.StockRepository;
import com.example.project1.repository.StockRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInit {

    private final StockRepository stockRepository;
    private final StockRecordRepository stockRecordRepository;
    private final RestTemplate restTemplate;

    public DataInit(StockRepository stockRepository, StockRecordRepository stockRecordRepository, RestTemplate restTemplate) {
        this.stockRepository = stockRepository;
        this.stockRecordRepository = stockRecordRepository;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    private void initializeData() throws IOException, ParseException {
        long startTime = System.nanoTime();

        ResponseEntity<List<StockEntity>> response1 = restTemplate.exchange(
                "http://filter-one-service/filter-one/run",
                org.springframework.http.HttpMethod.POST,
                null,
                new org.springframework.core.ParameterizedTypeReference<List<StockEntity>>() {}
        );
        List<StockEntity> stockEntities1 = response1.getBody();

        ResponseEntity<List<StockEntity>> response2 = restTemplate.exchange(
                "http://filter-two-service/filter-two/run",
                org.springframework.http.HttpMethod.POST,
                null,
                new org.springframework.core.ParameterizedTypeReference<List<StockEntity>>() {}
        );
        List<StockEntity> stockEntities2 = response2.getBody();

        List<StockEntity> combinedStockEntities = new ArrayList<>();
        if (stockEntities1 != null) combinedStockEntities.addAll(stockEntities1);
        if (stockEntities2 != null) combinedStockEntities.addAll(stockEntities2);

        ResponseEntity<List<StockEntity>> response3 = restTemplate.exchange(
                "http://filter-three-service/filter-three/run",
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(combinedStockEntities),
                new org.springframework.core.ParameterizedTypeReference<List<StockEntity>>() {}
        );
        List<StockEntity> finalStockEntities = response3.getBody();

        if (finalStockEntities != null) {
            stockRepository.saveAll(finalStockEntities);
        }

        long endTime = System.nanoTime();
        long durationInMillis = (endTime - startTime) / 1_000_000;

        long hours = durationInMillis / 3_600_000;
        long minutes = (durationInMillis % 3_600_000) / 60_000;
        long seconds = (durationInMillis % 60_000) / 1_000;

        System.out.printf("Total time for all filters to complete: %02d hours, %02d minutes, %02d seconds%n", hours, minutes, seconds);
    }
}
