
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.URISyntaxException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  static RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from
  // main anymore.
  // Copy your code from Module#3
  // PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and ensure it follows the
  // method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required
  // further as our
  // clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command
  // below:
  // ./gradlew test --tests PortfolioManagerTest

  // CHECKSTYLE:OFF
  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> 
      portfolioTrades, LocalDate endDate) {
    double annualizedReturns = 0.0;
    double totalReturn = 0.0;
    List<AnnualizedReturn> result = new ArrayList<AnnualizedReturn>();
    for (PortfolioTrade pt : portfolioTrades) {
      try {
        Candle cd = getStockQuote(pt.getSymbol(), pt.getPurchaseDate(), endDate);
        totalReturn = (double) ((cd.getClose() - cd.getOpen()) / cd.getOpen());
        long days = ChronoUnit.DAYS.between(pt.getPurchaseDate(), endDate);
        double years = (double) (days) / 365;
        annualizedReturns = Math.pow((1 + totalReturn), (double) (1 / years)) - 1;
        
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }      
      
      


      

      result.add(new AnnualizedReturn(pt.getSymbol(), annualizedReturns, totalReturn));

    }
    result.sort(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed());
    return result;

  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.

  public static Candle getStockQuote(String symbol, LocalDate from, LocalDate to) 
      throws JsonProcessingException {
    String uri = buildUri(symbol, from, to);
    RestTemplate restTemplate = new RestTemplate();
    TiingoCandle[] tc = restTemplate.getForObject(uri, TiingoCandle[].class);
    TiingoCandle candle = new TiingoCandle();
    candle.setClose(tc[tc.length - 1].getClose());
    candle.setOpen(tc[0].getOpen());
    return candle;
    
  }

  protected static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    return "https://api.tiingo.com/tiingo/daily/" + symbol 
      + "/prices?startDate=" + startDate + "&endDate=" + endDate 
      + "&token=8de63665046f5b927d4ff423068838a41bea4f25";
    //return uriTemplate;
  }

  
  
}
