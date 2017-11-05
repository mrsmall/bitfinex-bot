package com.klein.btc;

import com.klein.btc.model.Exchange;
import com.klein.btc.model.Product;

import java.text.DecimalFormat;
import java.util.*;

public class OrderBook {

    private final Exchange exchange;
    private final Product product;
    private final int maxEntries;
    private SortedMap<Float, Float> ask;
    private SortedMap<Float, Float> bid;
    private float minBid=Float.MAX_VALUE;
    private float minAsk=Float.MAX_VALUE;
    private float maxBid=0;
    private float maxAsk=0;

    private OrderBookListener orderBookListener;

    public OrderBook(Exchange exchange, Product product, int maxEntries, OrderBookListener orderBookListener) {
        this.exchange = exchange;
        this.product = product;
        this.maxEntries = maxEntries;
        this.orderBookListener = orderBookListener;
        if (orderBookListener!=null)
            orderBookListener.onInit(this);

        ask=new TreeMap<>();
        bid=new TreeMap<>();
    }

    public OrderBook(Exchange exchange, Product product, OrderBookListener orderBookListener) {
        this(exchange, product, 10, orderBookListener);
    }

    public Product getProduct() {
        return product;
    }

    public SortedMap<Float, Float> getAsk() {
        return ask;
    }

    public SortedMap<Float, Float> getBid() {
        return bid;
    }

    public void addAsk(float price, float size) {
        ask.put(price, size);
        if (orderBookListener!=null)
            orderBookListener.onAskChanged(this, price, size);

//        System.out.println("Added ASK: "+price);

        if (ask.size()>maxEntries){
            Float[] askPrices = ask.keySet().toArray(new Float[ask.size()]);
            minAsk=askPrices[0];
            maxAsk=askPrices[maxEntries-1];
            for (int i=maxEntries;i<askPrices.length;i++) {
                Float itemPrice = askPrices[i];
                ask.remove(itemPrice);
//                System.out.println("Removed ASK: "+itemPrice);
            }
        } else {
            minAsk=Math.min(minAsk, price);
            maxAsk=Math.max(maxAsk, price);
        }
    }

    public void addBid(float price, float size) {
        bid.put(price, size);
        if (orderBookListener!=null)
            orderBookListener.onBidChanged(this, price, size);

//        System.out.println("Added BID: "+price);

        if (bid.size()>maxEntries){
            Float[] bidPrices = bid.keySet().toArray(new Float[bid.size()]);
            maxBid=bidPrices[bidPrices.length-1];
            minBid=bidPrices[bidPrices.length-maxEntries];
            for (int i=0;i<bidPrices.length-maxEntries;i++) {
                Float itemPrice = bidPrices[i];
                bid.remove(itemPrice);
//                System.out.println("Removed BID: "+itemPrice);
            }
        } else {
            minBid=Math.min(minBid, price);
            maxBid=Math.max(maxBid, price);
        }
    }

    public void removeAsk(float price) {
        ask.remove(price);
    }

    public void removeBid(float price) {
        bid.remove(price);
    }

    public void dump(){
//        echo("\t\t\t");
//        echo(maxAsk);
//        echo("\n");
        Float[] askPrices = ask.keySet().toArray(new Float[ask.keySet().size()]);
        for (int i=askPrices.length-1;i>=0;i--){
            echo("\t");

            float price=askPrices[i];
            float size=ask.get(price);
            echo("\t\t");
            echo(price);
            echo("\t");
            echo(size);
            echo("\n");
        }
//        echo("\t\t\t");
//        echo(minAsk);
//        echo("\n");

//        echo("\t\t\t");
//        echo(maxBid);
//        echo("\n");
        Float[] bidPrices = bid.keySet().toArray(new Float[bid.keySet().size()]);
        for (int i=bidPrices.length-1;i>=0;i--){
            float price=bidPrices[i];
            float size=bid.get(price);
            echo("\t");
            echo(size);
            echo("\t");
            echo(price);
            echo("\n");
        }
//        echo("\t\t\t");
//        echo(minBid);
//        echo("\n");
//        echo("\n");
//        echo("\n");
    }

    public float getBestAsk(){
        return minAsk;
    }

    public float getBestBid(){
        return maxBid;
    }

    private void echo(float v) {
        System.out.print(new DecimalFormat("###0.00").format(v));
    }

    private void echo(String s) {
        System.out.print(s);
    }

    public Exchange getExchange() {
        return exchange;
    }
}
