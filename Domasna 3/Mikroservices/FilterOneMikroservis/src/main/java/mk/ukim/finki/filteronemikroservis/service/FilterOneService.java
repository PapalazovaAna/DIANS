package mk.ukim.finki.filteronemikroservis.service;

import mk.ukim.finki.filteronemikroservis.model.StockEntity;
import mk.ukim.finki.filteronemikroservis.repository.StockRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class FilterOneService {
    private final StockRepository stockRepository;
    private static final String STOCK_MARKET_URL = "https://www.mse.mk/mk/stats/symbolhistory/kmb"; // Замени со точниот URL

    public FilterOneService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<StockEntity> runFilter() throws IOException {
        Document document = Jsoup.connect(STOCK_MARKET_URL).get();
        Element selectMenu = document.select("select#Code").first();

        if (selectMenu != null) {
            Elements options = selectMenu.select("option");
            for (Element option : options) {
                String code = option.attr("value");
                if (!code.isEmpty() && code.matches("^[a-zA-Z]+$")) {
                    if (stockRepository.findByCompanyCode(code).isEmpty()) {
                        stockRepository.save(new StockEntity(code));
                    }
                }
            }
        }

        return stockRepository.findAll();
    }
}
