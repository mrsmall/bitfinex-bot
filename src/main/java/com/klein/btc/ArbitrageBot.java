package com.klein.btc;

import com.klein.btc.bitfinex.BitfinexBot;
import com.klein.btc.gdax.GdaxBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ArbitrageBot implements OrderBookListener {
    private static final Logger LOG= LoggerFactory.getLogger("tradelog.arbitrage.BTCUSD");
    private final Product product=Product.BTCUSD;
    private Map<Product, Set<OrderBook>> orderBooks=new HashMap<>();

    private TelegramLongPollingBot bot;

    public ArbitrageBot() {
        Properties props=new Properties();
        FileInputStream fis= null;
        try {
            fis = new FileInputStream("api_key.properties");
            props.load(fis);
            fis.close();

            bot = new TelegramLongPollingBot(){

                @Override
                public void onUpdateReceived(Update update) {
                    LOG.debug("Telegram update from user: {}:{}", update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName());
                }

                @Override
                public void onUpdatesReceived(List<Update> updates) {
                    LOG.debug("Telegram updates: {}", updates.size());
                    for (Update update : updates) {
                        LOG.debug("Telegram update from user: {}:{}", update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName());
                    }
                }


                @Override
                public String getBotUsername() {
                    return null;
                }

                @Override
                public String getBotToken() {
                    return props.getProperty("TELEGRAM_API");
                }

                @Override
                public void onClosing() {
                    LOG.info("Closing telegram bot");
                }
            };
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
        BitfinexBot bitfinex = new BitfinexBot(product, this, 0.01, 0.004);
        GdaxBot gdax = new GdaxBot(product, this, 0.01, 0.004);
    }

    public static void main(String[] args){
        new ArbitrageBot();
    }

    @Override
    public void onAskChanged(OrderBook orderBook, float price, float size) {
        addOrderBook(orderBook);
        checkDiffirences(orderBook.getProduct());
    }

    private void checkDiffirences(Product product) {
        Set<OrderBook> orderBooks=getOrderBooks(product);
        for (OrderBook ob1 : orderBooks) {
            for (OrderBook ob2 : orderBooks) {
                if (ob1.getExchange()!=ob2.getExchange()){
                    float diff=Math.abs(ob1.getBestAsk()-ob2.getBestBid());
                    float base=Math.min(ob1.getBestAsk(), ob2.getBestBid());
                    float diffRelative=diff*100/base;
                    LOG.debug("{}<->{} diff: {}%",ob1.getExchange(),ob2.getExchange(), diffRelative);
                }
            }
        }
    }

    private void addOrderBook(OrderBook orderBook) {
        Product product=orderBook.getProduct();
        Set<OrderBook> orderBooks=getOrderBooks(product);
        orderBooks.add(orderBook);
    }

    private Set<OrderBook> getOrderBooks(Product product) {
        Set<OrderBook> orderBooks=this.orderBooks.get(product);
        if (orderBooks==null){
            orderBooks=new HashSet<>();
            this.orderBooks.put(product, orderBooks);
        }
        return orderBooks;
    }

    @Override
    public void onBidChanged(OrderBook orderBook, float price, float size) {
        Product product=orderBook.getProduct();
        Set<OrderBook> orderBooks=getOrderBooks(product);
        orderBooks.add(orderBook);
    }

    @Override
    public void onInit(OrderBook orderBook) {
        addOrderBook(orderBook);
    }
}
