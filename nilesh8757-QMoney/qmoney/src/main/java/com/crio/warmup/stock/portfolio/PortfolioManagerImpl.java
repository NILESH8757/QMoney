
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
  @Deprecated
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws Exception {

    List<Candle> result = stockQuotesService.getStockQuote(symbol, from, to);
    return result;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) {
    // TODO Auto-generated method stub
    List<AnnualizedReturn> allTradesAnnualizedReturns = new ArrayList<AnnualizedReturn>();
    for (PortfolioTrade trade : portfolioTrades) {
      List<Candle> candles = new ArrayList<Candle>();
      try {
        candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      } catch (Exception e) {
        e.printStackTrace();
      }
      double buyPrice = getOpeningPriceOnStartDate(candles);
      double sellPrice = getClosingPriceOnEndDate(candles);
      Double totalReturns = getTotalReturns(buyPrice, sellPrice);
      double totalYears = getTotalYears(trade.getPurchaseDate(), endDate);
      double exponent = 1.0 / totalYears;
      double annualizedReturn = Math.pow((1 + totalReturns), exponent) - 1.0;
      AnnualizedReturn annualizedReturnObj = new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalYears);
      allTradesAnnualizedReturns.add(annualizedReturnObj);
    }
    Collections.sort(allTradesAnnualizedReturns, getComparator());
    return allTradesAnnualizedReturns;
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

  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    Collections.sort(candles, (x, y) -> x.getDate().compareTo(y.getDate()));
    return candles.get(0).getOpen();
  }

  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    Collections.sort(candles, (x, y) -> x.getDate().compareTo(y.getDate()));
    return candles.get(candles.size() - 1).getClose();
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades,
      LocalDate endDate,
      int numThreads) throws InterruptedException,
      StockQuoteServiceException {

    List<AnnualizedReturn> annualizedReturnList = new ArrayList<>();
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    List<Future<AnnualizedReturn>> futureList = new ArrayList<>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      Callable<AnnualizedReturn> callable = new Mycallable(stockQuotesService, portfolioTrade, endDate);

      Future<AnnualizedReturn> future = executorService.submit(callable);
      futureList.add(future);
    }

    for (Future<AnnualizedReturn> future : futureList) {
      try {
        annualizedReturnList.add(future.get());

      } catch (InterruptedException | ExecutionException e) {
        throw new StockQuoteServiceException(
            "Can't process the response from a third-party service or"
                + "response contains an error or is otherwise invalid.");
      }
    }

    // shutting down the executor service
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
        executorService.shutdown();
      }
    } catch (InterruptedException e) {
      executorService.shutdown();
    }

    annualizedReturnList.sort(getComparator());
    return annualizedReturnList;
  }

  static class Mycallable implements Callable<AnnualizedReturn> {

    private StockQuotesService stockQuotesService;
    private PortfolioTrade portfolioTrade;
    private LocalDate endDate;

    Mycallable(StockQuotesService stockQuotesService, PortfolioTrade portfolioTrade, LocalDate endDate) {
      this.stockQuotesService = stockQuotesService;
      this.portfolioTrade = portfolioTrade;
      this.endDate = endDate;
    }

    @Override
    public AnnualizedReturn call() throws StockQuoteServiceException {

      List<Candle> candles = Collections.emptyList();
      try {
        candles = stockQuotesService.getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(),
            endDate);
      } catch (Exception e) {
        throw new StockQuoteServiceException(
            "Can't process the response from a third-party service or"
                + "response contains an error or is otherwise invalid.");
      }

      if (candles != null) {
        double buyPrice = candles.stream()
            .filter(candle -> candle.getDate().equals(portfolioTrade.getPurchaseDate()))
            .findFirst().get()
            .getOpen();

        double sellPrice = candles.get(candles.size() - 1).getClose();

        AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endDate, portfolioTrade, buyPrice, sellPrice);
        return annualizedReturn;

      } else {
        throw new StockQuoteServiceException(
            "Can't process the response from a third-party service or"
                + "response contains an error or is otherwise invalid.");
      }

    }
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    long days = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    double years = (double) days / 365;
    double annualizedReturns = Math.pow((1 + totalReturns), (1 / years)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }

}
