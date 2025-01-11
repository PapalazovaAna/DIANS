package mk.ukim.finki.filterthreemikroservis.service;

import mk.ukim.finki.filterthreemikroservis.data.DataParser;
import mk.ukim.finki.filterthreemikroservis.model.StockEntity;
import mk.ukim.finki.filterthreemikroservis.model.StockRecordEntity;
import mk.ukim.finki.filterthreemikroservis.repository.StockRecordRepository;
import mk.ukim.finki.filterthreemikroservis.repository.StockRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class FilterThreeService {

    private final StockRepository stockRepository;
    private final StockRecordRepository stockRecordRepository;

    private static final String HISTORICAL_DATA_URL = "https://www.mse.mk/mk/stats/symbolhistory/";

    public FilterThreeService(StockRepository stockRepository, StockRecordRepository stockRecordRepository) {
        this.stockRepository = stockRepository;
        this.stockRecordRepository = stockRecordRepository;
    }

    public List<StockEntity> execute(List<StockEntity> input) throws IOException, ParseException {

        for (StockEntity company : input) {
            LocalDate fromDate = LocalDate.now();
            LocalDate toDate = LocalDate.now().plusYears(1);
            addHistoricalData(company, fromDate, toDate);
        }

        return input; // Assuming you want to return the input list after processing.
    }

    private void addHistoricalData(StockEntity company, LocalDate fromDate, LocalDate toDate) throws IOException {
        Connection.Response response = Jsoup.connect(HISTORICAL_DATA_URL + company.getCompanyCode())
                .data("FromDate", fromDate.toString())
                .data("ToDate", toDate.toString())
                .method(Connection.Method.POST)
                .execute();

        Document document = response.parse();

        Element table = document.select("table#resultsTable").first();

        if (table != null) {
            Elements rows = table.select("tbody tr");

            for (Element row : rows) {
                Elements columns = row.select("td");

                if (columns.size() > 0) {
                    LocalDate date = DataParser.parseDate(columns.get(0).text(), "d.M.yyyy");

                    if (stockRecordRepository.findByDateAndCompany(date, company).isEmpty()) {

                        NumberFormat format = NumberFormat.getInstance(Locale.GERMANY);

                        Double lastTransactionPrice = DataParser.parseDouble(columns.get(1).text(), format);
                        Double maxPrice = DataParser.parseDouble(columns.get(2).text(), format);
                        Double minPrice = DataParser.parseDouble(columns.get(3).text(), format);
                        Double averagePrice = DataParser.parseDouble(columns.get(4).text(), format);
                        Double percentageChange = DataParser.parseDouble(columns.get(5).text(), format);
                        Integer quantity = DataParser.parseInteger(columns.get(6).text(), format);
                        Integer turnoverBest = DataParser.parseInteger(columns.get(7).text(), format);
                        Integer totalTurnover = DataParser.parseInteger(columns.get(8).text(), format);

                        if (maxPrice != null) {

                            if (company.getLastUpdated() == null || company.getLastUpdated().isBefore(date)) {
                                company.setLastUpdated(date);
                            }

                            StockRecordEntity stockRecordEntity = new StockRecordEntity(
                                    date, lastTransactionPrice, maxPrice, minPrice, averagePrice, percentageChange,
                                    quantity, turnoverBest, totalTurnover);
                            stockRecordEntity.setCompany(company);
                            stockRecordRepository.save(stockRecordEntity);
                            company.getHistoricalData().add(stockRecordEntity);
                        }
                    }
                }
            }
        }
        stockRepository.save(company);
    }
}
