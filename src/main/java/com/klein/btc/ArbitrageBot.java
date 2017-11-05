package com.klein.btc;

import com.klein.btc.bitfinex.BitfinexBot;
import com.klein.btc.gdax.GdaxBot;
import com.klein.btc.model.*;
import com.klein.btc.repository.TicksRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.ApiContextInitializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArbitrageBot implements OrderBookListener {
    private static final Logger LOG = LoggerFactory.getLogger("tradelog.arbitrage.BTCUSD");
    private final Product product=Product.BTCUSD;
    private Map<Product, SortedSet<OrderBook>> orderBooks=new HashMap<>();

    @Autowired
    private TicksRepository ticksRepository;

    private Map<ExchangePair, ArbitrageOpportunity> opportunities=new ConcurrentHashMap<>(new HashMap<>());
    private TelegramBot telegramBot;
    private FileOutputStream askBidLogFileStream;
    private long lastAskBidLog=System.currentTimeMillis()+10000;
    private float signalLevel=0.7f;
    private Map<String, Ticks> ticks=new HashMap<>();

    public ArbitrageBot() {
        telegramBot=new TelegramBot();
        telegramBot.init();
        BitfinexBot bitfinex = new BitfinexBot(product, this, 0.01, 0.004);
        GdaxBot gdax = new GdaxBot(product, this, 0.01, 0.004);
        File file=new File("ask_bid_"+product.name()+".csv");
        try {
            askBidLogFileStream =new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /*
    public static void main(String[] args){
        ApiContextInitializer.init();
        new ArbitrageBot();
    }
    */

    @Override
    public void onAskChanged(OrderBook orderBook, float price, float size) {
        addOrderBook(orderBook);
        checkDifferences(orderBook.getProduct());
    }

    SimpleDateFormat df=new SimpleDateFormat("dd.MM.yy HH:mm:ss");
    private void checkDifferences(Product product) {
        Set<OrderBook> orderBooks=getOrderBooks(product);
        for (OrderBook ob1 : orderBooks) {
            for (OrderBook ob2 : orderBooks) {
                if (ob1.getExchange()!=ob2.getExchange()){
                    ExchangePair exchanges=ExchangePair.getInstance(ob1.getExchange(), ob2.getExchange());
                    ArbitrageOpportunity opportunity=opportunities.get(exchanges);

                    float diff=Math.abs(ob1.getBestAsk()-ob2.getBestBid());
                    float base=Math.min(ob1.getBestAsk(), ob2.getBestBid());
                    float diffRelative=diff*100/base;
                    LOG.debug("{}<->{} diff: {}%",ob1.getExchange(),ob2.getExchange(), diffRelative);
                    if (diffRelative>signalLevel){
                        if (opportunity==null){
                            opportunity=new ArbitrageOpportunity(exchanges, product);
                            opportunities.put(exchanges, opportunity);
                            opportunity.setDifference(diffRelative);
                            telegramBot.notifyOpportunity(product.name(), ob1.getExchange().name(), ob2.getExchange().name(), diffRelative);
                        } else {
                            if (diffRelative-opportunity.getDifference()>0.1){
                                opportunity.setDifference(diffRelative);
                                telegramBot.notifyOpportunityGotBetter(product.name(), ob1.getExchange().name(), ob2.getExchange().name(), diffRelative);
                            }
                        }
                    } else if (diffRelative<signalLevel*0.95) {
                        if (opportunity!=null){
                            opportunities.remove(exchanges);
                            telegramBot.notifyOpportunityIsGone(product.name(), ob1.getExchange().name(), ob2.getExchange().name(), diffRelative);
                        }
                    }
                }
            }
        }
        if (System.currentTimeMillis() - lastAskBidLog > 1000) {
            for (OrderBook orderBook : orderBooks) {
                addTick(orderBook.getExchange(), orderBook.getProduct(), orderBook.getBestAsk(), orderBook.getBestBid());
            }
        }
    }

    private void addTick(Exchange exchange, Product product, float bestAsk, float bestBid) {
        Ticks ticks=getTicks(exchange, product);
        int millis=(int) (System.currentTimeMillis()-ticks.getId().getTimestamp());
        ticks.addTick(millis,bestAsk, bestBid, 0, 0);
    }

    private Ticks getTicks(Exchange exchange, Product product) {
        String key=exchange.name()+"_"+product.name();
        Ticks ticks=this.ticks.get(key);
        long ts=System.currentTimeMillis();
        ts=ts-ts%60000;
        if (ticks==null){
            ticks=new Ticks();
            ticks.setId(new TicksId(exchange, product, ts));
            this.ticks.put(key, ticks);
        } else {
            if (ticks.getId().getTimestamp()!=ts){
                ticksRepository.save(ticks);

                ticks=new Ticks();
                ticks.setId(new TicksId(exchange, product, ts));
                this.ticks.put(key, ticks);
            }
        }
        return ticks;
    }

    private void addOrderBook(OrderBook orderBook) {
        Product product=orderBook.getProduct();
        Set<OrderBook> orderBooks=getOrderBooks(product);
        orderBooks.add(orderBook);
    }

    private Set<OrderBook> getOrderBooks(Product product) {
        SortedSet<OrderBook> orderBooks=this.orderBooks.get(product);
        if (orderBooks==null){
            orderBooks=new TreeSet<>(new Comparator<OrderBook>() {
                @Override
                public int compare(OrderBook o1, OrderBook o2) {
                    return Integer.compare(o1.getExchange().ordinal(), o2.getExchange().ordinal());
                }
            });
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
