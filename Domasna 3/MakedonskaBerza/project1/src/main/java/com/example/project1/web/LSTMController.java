package com.example.project1.web;

import com.example.project1.service.LSTMService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LSTMController {

    private final LSTMService LSTMService;

    @PostMapping("/predict")
    public ResponseEntity<Double> predictPrice(@RequestParam(name = "companyId") Long companyId) {
        double predictedPrice = LSTMService.predictNextMonth(companyId);
        return ResponseEntity.ok(predictedPrice);
    }

    @PostMapping("/predict-indicators-and-signals")
    public ResponseEntity<Map<String, Object>> predictIndicatorsAndSignals(
            @RequestParam(name = "companyId") Long companyId,
            @RequestParam(name = "indicatorId") int indicatorId,
            @RequestParam(name = "days") int days) {

        Map<String, Object> response = LSTMService.predictIndicatorsAndSignals(companyId, indicatorId, days);

        return ResponseEntity.ok(response);
    }
}
