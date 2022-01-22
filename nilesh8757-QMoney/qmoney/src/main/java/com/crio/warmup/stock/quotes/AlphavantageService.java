
package com.crio.warmup.stock.quotes;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {
  private final String API_KEY = "UDED8WYJS4BR79GY";
  private RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonMappingException, JsonProcessingException, StockQuoteServiceException {
    // TODO Auto-generated method stub
    String url = buildUri(symbol, from, to);
    List<Candle> candles = new ArrayList<>();
    AlphavantageDailyResponse alphavantageDailyResponse = null;
    String response;

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    if (to.isBefore(from))
      throw new RuntimeException();

    try {
      response = this.restTemplate.getForObject(url, String.class);
      alphavantageDailyResponse = objectMapper.readValue(response, AlphavantageDailyResponse.class);

      Map<LocalDate, AlphavantageCandle> responseCandles = alphavantageDailyResponse.getCandles();

      for (Map.Entry<LocalDate, AlphavantageCandle> entry : responseCandles.entrySet()) {
        LocalDate keyDate = entry.getKey();
        if ((keyDate.isEqual(from) || keyDate.isEqual(to)) || (keyDate.isAfter(from) && keyDate.isBefore(to))) {
          AlphavantageCandle candle = entry.getValue();
          candle.setDate(keyDate);
          candles.add(candle);
        }
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw new StockQuoteServiceException("something broke!");
    }

    Collections.sort(candles, this.customComparator());

    return candles;
  }

  private Comparator<Candle> customComparator() {
    return Comparator.comparing(Candle::getDate);
  }

  @Override
  public String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    // TODO Auto-generated method stub
    String urlTemplate = String.format(
        "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&outputsize=full&apikey=%s",
        symbol, this.API_KEY);

    return urlTemplate;
  }

}
