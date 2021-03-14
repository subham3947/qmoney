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

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  RestTemplate restTemplate;
  static StockQuotesService stockQuotesService;



  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  
  PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  PortfolioManagerImpl(StockQuotesService stockQuotesService) {
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
      portfolioTrades, LocalDate endDate) throws StockQuoteServiceException {
    if (endDate == null) {
      Collections.emptyList();
    }
    try {
      List<AnnualizedReturn> result = new ArrayList<AnnualizedReturn>();
      for (PortfolioTrade pt : portfolioTrades) {
        AnnualizedReturn annualReturn = getReturn(pt, endDate);
        result.add(annualReturn);
      }
      result.sort(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed());
      return result;
      
    } catch (Exception e) {
      //TODO: handle exception
      throw new StockQuoteServiceException("Error occured from API endpoint",e.getCause());

    }
    

  }

  public  AnnualizedReturn getReturn(PortfolioTrade portfolioTrade, LocalDate endDate) throws 
      StockQuoteServiceException {
    String symbol = portfolioTrade.getSymbol();
    AnnualizedReturn annualizedReturn;
    LocalDate startDate = portfolioTrade.getPurchaseDate();
    try {
      if (startDate.compareTo(endDate) >= 0) { 
        throw new RuntimeException("Failed to get data from service provider");
      }
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
      throw new StockQuoteServiceException("Error occured from API endpoint",e.getCause());
      //annualizedReturn = new AnnualizedReturn(symbol, 0.0, 0.0);
    }
     
    return annualizedReturn;
  }




  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.

  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.

  public List<Candle>  getStockQuote(String symbol, LocalDate from, LocalDate to) 
      throws JsonProcessingException, StockQuoteServiceException {
    return stockQuotesService.getStockQuote(symbol, from, to);
  }
          

  protected static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    return "https://api.tiingo.com/tiingo/daily/" + symbol 
      + "/prices?startDate=" + startDate + "&endDate=" + endDate 
      + "&token=8de63665046f5b927d4ff423068838a41bea4f25";

  }


  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> 
      portfolioTrades, LocalDate endDate, int numThreads) throws 
        InterruptedException, StockQuoteServiceException {
    // TODO Auto-generated method stub
    List<AnnualizedReturn> result = new ArrayList<AnnualizedReturn>();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<CallAnnualized> taskList = new ArrayList<CallAnnualized>();
    for (PortfolioTrade pt : portfolioTrades) {
      taskList.add(new CallAnnualized(pt, endDate));
    }
    List<Future<AnnualizedReturn>> resultList = null;
    resultList = executor.invokeAll(taskList);
    executor.shutdown();
    for (Future<AnnualizedReturn> ar : resultList) {
      try {
        result.add(ar.get());
      } catch (ExecutionException e) {
        throw new StockQuoteServiceException("Error occured from API endpoint",e.getCause());

      } 

    }
    result.sort(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed());
    return result;
  }

  class CallAnnualized implements Callable<AnnualizedReturn> {
    //PortfolioManagerImpl portfolioManagerImpl;
    PortfolioTrade portfolioTrade;
    LocalDate endDate;

    public CallAnnualized(PortfolioTrade portfolioTrade,LocalDate endDate) {
      this.portfolioTrade = portfolioTrade;
      this.endDate = endDate;
    }
    
    public AnnualizedReturn call() throws Exception {
      // TODO Auto-generated method stub
      try {
        return  PortfolioManagerImpl.this.getReturn(portfolioTrade, endDate);
      } catch (Exception e) {
        throw new StockQuoteServiceException("Error occured from API endpoint",e.getCause());

      }
      
      
  
    }

   
    
      
  }
  

}


