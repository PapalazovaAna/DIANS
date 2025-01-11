package mk.ukim.finki.filteronemikroservis.web;

import mk.ukim.finki.filteronemikroservis.model.StockEntity;
import mk.ukim.finki.filteronemikroservis.service.FilterOneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/filter-one")
public class FilterOneController {

    private final FilterOneService filterOneService;

    public FilterOneController(FilterOneService filterOneService) {
        this.filterOneService = filterOneService;
    }

    @PostMapping("/run")
    public ResponseEntity<List<StockEntity>> runFilter() throws IOException {
        List<StockEntity> stockEntities = filterOneService.runFilter();
        return ResponseEntity.ok(stockEntities);  // Враќа ги податоците од филтерот
    }
}
