package co.codingnomads.bot.arbitrage.service;

import co.codingnomads.bot.arbitrage.model.ActivatedExchange;
import co.codingnomads.bot.arbitrage.model.exchange.ExchangeSpecs;
import co.codingnomads.bot.arbitrage.model.exchange.GDAXSpecs;
import co.codingnomads.bot.arbitrage.model.exchange.KrakenSpecs;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.Wallet;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Thomas Leruth on 12/16/17
 */

public class Test {

    public static void main(String[] args) throws IOException {

//        ExchangeSpecs krakenSpecs = new KrakenSpecs(
//                "null",
//                "null");

        ExchangeSpecs gdaxSpecs = new GDAXSpecs(
                null,
                null,
                null);

        // ExchangeSpecification exSpec = new KrakenExchange().getDefaultExchangeSpecification();

        ArrayList<ExchangeSpecs> selected = new ArrayList<>();
//        selected.add(krakenSpecs);
        selected.add(gdaxSpecs);

        ExchangeGetter exchangeGetter = new ExchangeGetter();

        ArrayList<ActivatedExchange> activatedExchanges = exchangeGetter.getAllSelectedExchangeServices(selected);

        for (ActivatedExchange activatedExchange : activatedExchanges) {
            if (activatedExchange.isActivated() && activatedExchange.isTradingMode()) {
                Wallet wallet = activatedExchange.getExchange().getAccountService().getAccountInfo().getWallet();
                System.out.println(wallet);
                System.out.println(wallet.getBalance(Currency.EUR));
                System.out.println(wallet.getBalance(Currency.ETH));
            }
        }

//        AccountService accountService = kraken.getAccountService();
//        Wallet wallet = accountService.getAccountInfo().getWallet();
//        System.out.println(wallet.getBalance(Currency.EUR).getTotal());
//        System.out.println(wallet.getBalance(Currency.ETH).getTotal());
//        System.out.println("break");
    }

}
