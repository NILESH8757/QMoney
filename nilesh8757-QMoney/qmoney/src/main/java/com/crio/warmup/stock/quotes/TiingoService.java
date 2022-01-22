
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {

    try {
      String response = restTemplate.getForObject(buildUri(symbol, from, to), String.class);
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      Candle[] result = objectMapper.readValue(response, TiingoCandle[].class);
      return Arrays.asList(result);
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw new StockQuoteServiceException("something wrong!");
    }
  }

  @Override
  public String buildUri(String symbol, LocalDate from, LocalDate to) {
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
        + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

    String token = "32ef891977cbd70b594c17c64c62dfffe0c1db48";
    String url = uriTemplate.replace("$APIKEY", token).replace("$SYMBOL",
        symbol)
        .replace("$STARTDATE", from.toString())
        .replace("$ENDDATE", to.toString());
    return url;
  }
}
