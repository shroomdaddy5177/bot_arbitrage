package co.codingnomads.bot.arbitrage.service.arbitrage;

import co.codingnomads.bot.arbitrage.exception.ExchangeDataException;
import co.codingnomads.bot.arbitrage.model.exchange.ActivatedExchange;
import co.codingnomads.bot.arbitrage.model.ticker.TickerData;
import co.codingnomads.bot.arbitrage.action.arbitrage.selection.ArbitrageActionSelection;
import co.codingnomads.bot.arbitrage.action.arbitrage.ArbitragePrintAction;
import co.codingnomads.bot.arbitrage.action.arbitrage.ArbitrageTradingAction;
import co.codingnomads.bot.arbitrage.action.arbitrage.ArbitrageEmailAction;
import co.codingnomads.bot.arbitrage.exception.EmailLimitException;
import co.codingnomads.bot.arbitrage.exchange.ExchangeSpecs;
import co.codingnomads.bot.arbitrage.service.general.*;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * Created by Thomas Leruth on 12/14/17
 * <p>
 * the arbitrage bot class
 */


@Service
public class Arbitrage {

    BalanceCalc balanceCalc = new BalanceCalc();

    ExchangeDataGetter exchangeDataGetter = new ExchangeDataGetter();

    DataUtil dataUtil = new DataUtil();

    ExchangeGetter exchangeGetter = new ExchangeGetter();


    int timeIntervalRepeater;

    int loopIterations;

    public int getTimeIntervalRepeater() {
        return timeIntervalRepeater;
    }

    public void setTimeIntervalRepeater(int timeIntervalRepeater) {
        this.timeIntervalRepeater = timeIntervalRepeater;
    }

    public int getLoopIterations() {
        return loopIterations;
    }

    public void setLoopIterations(int loopIterations) {
        this.loopIterations = loopIterations;
    }

    /**
     * Arbitrage bot with multiple arbitrage action
     *
     * @param currencyPair             the pair selected
     * @param selectedExchanges        an ArrayList of ExchangeEnum that will be looked in
     * @param arbitrageActionSelection A Pojo for selecting which action to be taken after an arbitrage detection
     * @throws IOException
     */
    public void run(CurrencyPair currencyPair,
                    ArrayList<ExchangeSpecs> selectedExchanges,
                    ArbitrageActionSelection arbitrageActionSelection) throws IOException, InterruptedException, EmailLimitException, ExchangeDataException {


        //Determines the arbitrage action being called
        Boolean tradingMode = arbitrageActionSelection instanceof ArbitrageTradingAction;
        Boolean emailMode = arbitrageActionSelection instanceof ArbitrageEmailAction;
        Boolean printMode = arbitrageActionSelection instanceof ArbitragePrintAction;


        //If trading mode and exchange specs are not set up, throw new ExchangeDataException
        if (tradingMode) {

            for (ExchangeSpecs exchange : selectedExchanges) {

                if (exchange.GetSetupedExchange().getApiKey() == null || exchange.GetSetupedExchange().getSecretKey() == null) {

                    throw new ExchangeDataException("You must enter correct exchange specs for " + exchange.GetSetupedExchange().getExchangeName());
                }
            }

        }


        //sets the tradeValueBase as -1, as a precaution to prevent trading, when it is not suppose too, or will lose money.
        double tradeValueBase = -1;

        //sets the tradeValueBase given in the controller for arbitrageTradingAction
        if (tradingMode) tradeValueBase = ((ArbitrageTradingAction) arbitrageActionSelection).getTradeValueBase();

        //convert the double tradeValueBase to a big decimal
        BigDecimal bigDecimalTradeValueBase = new BigDecimal(tradeValueBase);

        BigDecimal valueOfTradeValueBase = BigDecimal.valueOf(tradeValueBase);

        //prints your balance for the chosen currency pair for the activated exchanges(ones where exchange specs are set)
        balanceCalc.Balance(selectedExchanges, currencyPair);

        //create a new array list of Activated Exchanges and sets it equal to the selected exchanges set in the controller
        ArrayList<ActivatedExchange> activatedExchanges = exchangeGetter.getAllSelectedExchangeServices(selectedExchanges, tradingMode);

        //for each activatedExchange if available funds is less than trade value base you can not have the funds to complete the trade on that exchange
        //and removes the exchanges without the founds from activated exchanges
        for(ActivatedExchange activatedExchange : activatedExchanges) {

            if(activatedExchange.getExchange().getMarketDataService().getTicker(currencyPair) == null){

                System.out.println(activatedExchange.getExchange().getMarketDataService().getTicker(currencyPair));
                activatedExchanges.remove(activatedExchange);


            } else {

                BigDecimal availableFunds = activatedExchange.getExchange().getAccountService().getAccountInfo().getWallet().getBalance(currencyPair.base).getAvailable();

          if(availableFunds.compareTo(valueOfTradeValueBase) < 0) {

              System.out.println("#####################################");
              System.out.println("You do not have the base funds to execute this trade on " + activatedExchange.getExchange().getExchangeSpecification().getExchangeName());
              System.out.println("#####################################");
              activatedExchanges.remove(activatedExchange);
              System.out.println("removed " + activatedExchange.getExchange().getExchangeSpecification().getExchangeName() + "from activated exchanges");
          }
          }
        }



        //makes while loop run continuously, if loopIteration is set, and only once is not set
        while (loopIterations >= 0) {


            //Create an ArrrayList of TickerData and set it to the get all ticker data method from the exchange data getter class
            ArrayList<TickerData> listTickerData = exchangeDataGetter.getAllTickerData(

                    activatedExchanges,
                    currencyPair,
                    tradeValueBase);

            //if the list of ticker data is empty the currencypair is not supported on the exchange
            if (listTickerData.size() == 0) {

                throw new ExchangeDataException("Unable to pull exchange data, either the pair " + currencyPair + " is not supported on the exchange/s selected" +
                        " or you do not have a wallet with the needed trade base of " + tradeValueBase + currencyPair.base);
            }

            //find the best sell price
            TickerData highBid = dataUtil.highBidFinder(listTickerData);
            //find the best buy price
            TickerData lowAsk = dataUtil.lowAskFinder(listTickerData);

            //new BigDecimal set to the difference of the best sell price and the best buy price
            BigDecimal difference = highBid.getBid().divide(lowAsk.getAsk(), 5, RoundingMode.HALF_EVEN);

            //if the call is an instance of print action, run the print method form arbitrage print action
            if (printMode) {
                ArbitragePrintAction arbitragePrintAction = (ArbitragePrintAction) arbitrageActionSelection;
                arbitragePrintAction.print(lowAsk, highBid, arbitrageActionSelection.getArbitrageMargin());
            }
            //if the call is an instance of email action, run the email method from the arbitrage email action
            if (emailMode) {
                ArbitrageEmailAction arbitrageEmailAction = (ArbitrageEmailAction) arbitrageActionSelection;
                arbitrageEmailAction.email(arbitrageEmailAction.getEmail(), lowAsk, highBid, difference, arbitrageActionSelection.getArbitrageMargin());

            }
            //if the call is an instance of trading action, run the trade method from the arbitrage trading action
            if (tradingMode) {
                ArbitrageTradingAction arbitrageTradingAction = (ArbitrageTradingAction) arbitrageActionSelection;
                if (arbitrageTradingAction.canTrade(lowAsk, highBid, (ArbitrageTradingAction) arbitrageActionSelection) == true){
                    //arbitrageTradingAction.makeTrade(lowAsk, highBid, (ArbitrageTradingAction) arbitrageActionSelection);
                }

            }
            //if the timeIntervalRepeater is set and is greater than 5 seconds, sleeps the thread that time
            if (timeIntervalRepeater >= 5000) {
                Thread.sleep(timeIntervalRepeater);
                loopIterations--;
            }
            //if the timeIntervalRepeater is  not set  or is set to lower than 5 seconds, sleep the thread 5 seconds
            else {
                Thread.sleep(5000);
                loopIterations--;
            }
        }
    }
}