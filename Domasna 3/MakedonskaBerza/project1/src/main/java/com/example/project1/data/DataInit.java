package com.example.project1.data;

import com.example.project1.model.StockEntity;
import com.example.project1.model.StockRecordEntity;
import com.example.project1.repository.StockRepository;
import com.example.project1.repository.StockRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class DataInit {
    private final StockRepository stockRepository;
    private final StockRecordRepository stockRecordRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DataInit(StockRepository stockRepository,
                    StockRecordRepository stockRecordRepository,
                    RestTemplate restTemplate,
                    ObjectMapper objectMapper) {
        this.stockRepository = stockRepository;
        this.stockRecordRepository = stockRecordRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void initializeData() throws IOException, ParseException {
        try {
            long startTime = System.nanoTime();

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            // First request remains the same
            log.debug("Making first request to filter-one...");
            ResponseEntity<List<StockRecordEntity>> response1 = restTemplate.exchange(
                    "http://127.0.0.1:8081/filter-one/run",
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<StockRecordEntity>>() {}
            );

            List<StockRecordEntity> stockRecords = response1.getBody();
            log.info("Response 1 received with {} records", stockRecords != null ? stockRecords.size() : 0);

            // Modified second request
            log.debug("Making second request to filter-two...");
            try {
                // First try to get the raw response as a String for debugging
                ResponseEntity<String> rawResponse = restTemplate.exchange(
                        "http://127.0.0.1:8082/filter-two/run",
                        HttpMethod.POST,
                        new HttpEntity<>(headers),
                        String.class
                );
                log.debug("Raw response from filter-two: {}", rawResponse.getBody());

                // Then parse it using ObjectMapper
                List<StockEntity> stockEntities = objectMapper.readValue(
                        rawResponse.getBody(),
                        new TypeReference<List<StockEntity>>() {}
                );
                log.info("Successfully parsed {} stock entities", stockEntities.size());

                if (!stockEntities.isEmpty()) {
                    // Third request
                    log.debug("Making third request to filter-three...");
                    HttpEntity<List<StockEntity>> requestEntity = new HttpEntity<>(stockEntities, headers);
                    ResponseEntity<List<StockEntity>> response3 = restTemplate.exchange(
                            "http://127.0.0.1:8083/filter-three/run",
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<List<StockEntity>>() {}
                    );

                    List<StockEntity> finalStockEntities = response3.getBody();
                    if (finalStockEntities != null) {
                        stockRepository.saveAll(finalStockEntities);
                        log.info("Saved {} entities to the database", finalStockEntities.size());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing filter-two response", e);
                throw e;
            }

            long endTime = System.nanoTime();
            long durationInMillis = (endTime - startTime) / 1_000_000;
            log.info("Total execution time: {} ms", durationInMillis);

        } catch (Exception e) {
            log.error("Error during data initialization", e);
            log.error("Error details:", e);
            throw e;
        }
    }
}