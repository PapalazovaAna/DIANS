package com.example.project1.web;

import com.example.project1.service.LSTMService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LSTMController {

    private final LSTMService LSTMService;

    @PostMapping("/predict")
    public ResponseEntity<String> generateSignal(@RequestParam(name = "companyId") Long companyId) {
        String response = String.valueOf(LSTMService.predictIndicatorsAndSignals(companyId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/predict-next-month-price")
    public ResponseEntity<Double> predictPrice(@RequestParam(name = "companyId") Long companyId){
        Double response = LSTMService.predictNextMonth(companyId);
        return ResponseEntity.ok(response);
    }

}
