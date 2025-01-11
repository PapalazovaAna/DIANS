package mk.ukim.finki.filterthreemikroservis.web;


import lombok.RequiredArgsConstructor;
import mk.ukim.finki.filterthreemikroservis.model.StockEntity;
import mk.ukim.finki.filterthreemikroservis.service.FilterThreeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/filter-three")
@RequiredArgsConstructor
public class FilterThreeController {

    private final FilterThreeService filterThreeService;

    @PostMapping("/run")
    public ResponseEntity<List<StockEntity>> runFilter(@RequestBody List<StockEntity> input) throws IOException, ParseException {
        List<StockEntity> result = filterThreeService.execute(input);
        return ResponseEntity.ok(result);
    }
}
