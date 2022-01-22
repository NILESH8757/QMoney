
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.apache.logging.log4j.ThreadContext;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  private static final String token = "32ef891977cbd70b594c17c64c62dfffe0c1db48";
  private static String APIEndPointUrl = "https://api.tiingo.com/tiingo/daily/";

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {

    File jsonFile = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] trades = objectMapper.readValue(jsonFile, PortfolioTrade[].class);
    List<String> symbols = new ArrayList<>();
    for (PortfolioTrade trade : trades) {
      symbols.add(trade.getSymbol());
    }
    return symbols;
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "trades.json";
    String toStringOfObjectMapper = "ObjectMapper";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    // String lineNumberFromTestFileInStackTrace = "";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace /*
                                                                      * ,
                                                                      * lineNumberFromTestFileInStackTrace
                                                                      */ });
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    List<PortfolioTrade> allTrades = readTradesFromJson(args[0]);
    LocalDate endDate = LocalDate.parse(args[1]);
    RestTemplate restTemplate = new RestTemplate();
    List<TotalReturnsDto> priceList = new ArrayList<TotalReturnsDto>();

    for (PortfolioTrade trade : allTrades) {
      String url = prepareUrl(trade, endDate, token);
      TiingoCandle[] fewDaysPrices = restTemplate.getForObject(url, TiingoCandle[].class);
      TiingoCandle closingPriceOnOrBeforeEndDate = getClosingPrice(fewDaysPrices);
      TotalReturnsDto currentTickerPrice = new TotalReturnsDto(trade.getSymbol(),
          closingPriceOnOrBeforeEndDate.getClose());
      priceList.add(currentTickerPrice);
    }

    Collections.sort(priceList, new Comparator<TotalReturnsDto>() {
      @Override
      public int compare(TotalReturnsDto o1, TotalReturnsDto o2) {
        return o1.getClosingPrice().compareTo(o2.getClosingPrice());
      }
    });

    List<String> result = new ArrayList<String>();
    for (TotalReturnsDto obj : priceList) {
      result.add(obj.getSymbol());
    }
    return result;
  }

  private static TiingoCandle getClosingPrice(TiingoCandle[] fewDaysPrices) {
    Arrays.sort(fewDaysPrices, (x, y) -> x.getDate().compareTo(y.getDate()));
    return fewDaysPrices[fewDaysPrices.length - 1];
  }

  public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
    List<PortfolioTrade> allTrades = new ArrayList<>();
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] tradeArray = objectMapper.readValue(resolveFileFromResources(filename), PortfolioTrade[].class);
    for (PortfolioTrade trade : tradeArray) {
      allTrades.add(trade);
    }
    return allTrades;
  }

  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    return APIEndPointUrl + trade.getSymbol() + "/prices?startDate=" + trade.getPurchaseDate() + "&endDate=" + endDate
        + "&token=" + token;
  }

  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    Collections.sort(candles, (x, y) -> x.getDate().compareTo(y.getDate()));
    return candles.get(0).getOpen();
  }

  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    Collections.sort(candles, (x, y) -> x.getDate().compareTo(y.getDate()));
    return candles.get(candles.size() - 1).getClose();
  }

  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    String TiingoAPIurl = prepareUrl(trade, endDate, token);
    RestTemplate restTemplate = new RestTemplate();
    TiingoCandle[] arraysOfCandles = restTemplate.getForObject(TiingoAPIurl, TiingoCandle[].class);
    List<Candle> allCandles = new ArrayList<Candle>();
    for (Candle candle : arraysOfCandles) {
      allCandles.add(candle);
    }
    return allCandles;
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
    List<PortfolioTrade> allTrades = readTradesFromJson(args[0]);
    List<AnnualizedReturn> allTradeReturns = new ArrayList<AnnualizedReturn>();
    LocalDate endDate = LocalDate.parse(args[1]);
    for (PortfolioTrade trade : allTrades) {
      List<Candle> candles = fetchCandles(trade, endDate, getToken());
      double buyPrice = getOpeningPriceOnStartDate(candles);
      double sellPrice = getClosingPriceOnEndDate(candles);
      AnnualizedReturn returns = calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
      allTradeReturns.add(returns);
    }

    Collections.sort(allTradeReturns, (x, y) -> y.getAnnualizedReturn().compareTo(x.getAnnualizedReturn()));

    return allTradeReturns;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {

    Double totalReturns = getTotalReturns(buyPrice, sellPrice);
    double totalYears = getTotalYears(trade.getPurchaseDate(), endDate);
    double exponent = 1.0 / totalYears;
    double annualizedReturns = Math.pow((1 + totalReturns), exponent) - 1.0;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalYears);
  }

  public static double getTotalReturns(Double buyPrice, Double sellPrice) {
    return (sellPrice - buyPrice) / buyPrice;
  }

  public static double getTotalYears(LocalDate startDate, LocalDate endDate) {
    long dateDiff = ChronoUnit.DAYS.between(startDate, endDate);
    Long l = Long.valueOf(dateDiff);
    double totalYears = l.doubleValue();
    return totalYears / 365.0;
  }

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    String contents = readFileAsString(file);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
    return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }

  private static String readFileAsString(String file) throws URISyntaxException, IOException {
    return new String(Files.readAllBytes(resolveFileFromResources(file).toPath()), "UTF-8");
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    // printJsonObject(mainReadFile(args));
    // String[] args1 = {"trades.json", "2020-01-01"};

    // printJsonObject(mainReadQuotes(args));

    // printJsonObject(mainCalculateSingleReturn(args));
    printJsonObject(mainCalculateReturnsAfterRefactor(args));

  }

  public static String getToken() {
    return token;
  }
}
