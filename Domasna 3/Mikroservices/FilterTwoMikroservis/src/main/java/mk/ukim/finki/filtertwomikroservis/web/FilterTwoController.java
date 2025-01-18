package mk.ukim.finki.filtertwomikroservis.web;

import mk.ukim.finki.filtertwomikroservis.model.StockEntity;
import mk.ukim.finki.filtertwomikroservis.service.FilterTwoService;
import mk.ukim.finki.filtertwomikroservis.repository.StockRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/filter-two")
public class FilterTwoController {
    private final FilterTwoService filterTwoService;
    private final StockRepository stockRepository;

    public FilterTwoController(FilterTwoService filterTwoService, StockRepository stockRepository) {
        this.filterTwoService = filterTwoService;
        this.stockRepository = stockRepository;
    }

    @PostMapping("/run")
    public ResponseEntity<List<StockEntity>> runFilter() {
        try {
            List<StockEntity> stockEntities = stockRepository.findAll();
            List<StockEntity> filteredStocks = filterTwoService.execute(stockEntities);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return new ResponseEntity<>(filteredStocks, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}