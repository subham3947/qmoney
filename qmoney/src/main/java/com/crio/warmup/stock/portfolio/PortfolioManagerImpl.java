
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.URI;
import java.net.URISyntaxException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  RestTemplate restTemplate;
  StockQuotesService stockQuotesService;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!



  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    //this.restTemplate = restTemplate;
    this.stockQuotesService = stockQuotesService;
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
    List<AnnualizedReturn> result = new ArrayList<AnnualizedReturn>();
    for (PortfolioTrade pt : portfolioTrades) {
      AnnualizedReturn annualReturn = getReturn(pt, endDate);
      result.add(annualReturn);
    
    }
    result.sort(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed());
    return result;

  }

  public AnnualizedReturn getReturn(PortfolioTrade portfolioTrade, LocalDate endDate) {
    String symbol = portfolioTrade.getSymbol();
    AnnualizedReturn annualizedReturn;
    LocalDate startDate = portfolioTrade.getPurchaseDate();
    try {
      List<Candle> candle = getStockQuote(symbol, startDate, endDate);
      Candle firstDay = candle.get(0);
      Candle lastDay = candle.get(candle.size() - 1);
      Double buyPrice = firstDay.getOpen();
      Double sellPrice = lastDay.getClose();
      Double totalReturn = (double)((sellPrice - buyPrice) / buyPrice);
      long days = ChronoUnit.DAYS.between(startDate,endDate);
      double years = (double)(days) / 365;
      double annualizedReturns = Math.pow((1 + totalReturn), (double)(1 / years)) - 1;
      annualizedReturn = new AnnualizedReturn(symbol, annualizedReturns, totalReturn); 
    } catch (Exception e) {
      //TODO: handle exception
      annualizedReturn = new AnnualizedReturn(symbol, 0.0, 0.0);
    }
     
    return annualizedReturn;
  }







  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.

  public List<Candle>  getStockQuote(String symbol, LocalDate from, LocalDate to) 
      throws JsonProcessingException {
    TiingoCandle[] tc;
    String uri = buildUri(symbol, from, to);
    RestTemplate restTemplate = new RestTemplate();
    tc = restTemplate.getForObject(uri, TiingoCandle[].class);
    if (tc != null){
      return Arrays.asList(tc);
      } else {
        return Collections.emptyList();
      }
  }
          
    

  protected static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    return "https://api.tiingo.com/tiingo/daily/" + symbol 
      + "/prices?startDate=" + startDate + "&endDate=" + endDate 
      + "&token=8de63665046f5b927d4ff423068838a41bea4f25";

  }

  
  


  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.

}
