
package com.crio.warmup.stock.portfolio;

import org.springframework.web.client.RestTemplate;
import com.crio.warmup.stock.quotes.StockQuoteServiceFactory;
import com.crio.warmup.stock.quotes.StockQuotesService;

public class PortfolioManagerFactory {

  @Deprecated
  public static PortfolioManager getPortfolioManager(RestTemplate restTemplate) {
    return new PortfolioManagerImpl(restTemplate);
  }

  public static PortfolioManager getPortfolioManager(String provider,
      RestTemplate restTemplate) {

    StockQuotesService service = StockQuoteServiceFactory.getService(provider, restTemplate);

    return new PortfolioManagerImpl(service);
  }
}
