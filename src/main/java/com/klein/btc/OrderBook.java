package com.klein.btc;

import com.klein.btc.Product;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

public class OrderBook {

    private final Product product;
    private SortedMap<Float, Float> ask=new TreeMap<>();
    private SortedMap<Float, Float> bid=new TreeMap<>(new Comparator<Float>() {
        @Override
        public int compare(Float o1, Float o2) {
            return -o1.compareTo(o2);
        }
    });

    public OrderBook(Product product) {
        this.product = product;
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
    }

    public void addBid(float price, float size) {
        bid.put(price, size);
    }

    public void removeAsk(float price) {
        ask.remove(price);
    }

    public void removeBid(float price) {
        bid.remove(price);
    }

    public void dump(){
        Float[] askPrices = ask.keySet().toArray(new Float[ask.keySet().size()]);
        Float[] bidPrices = bid.keySet().toArray(new Float[bid.keySet().size()]);
        int maxIndex=Math.max(askPrices.length, bidPrices.length);
        maxIndex=Math.min(maxIndex, 20);
        for (int i=0;i<maxIndex;i++){
            if (bidPrices.length>i){
                float price=bidPrices[i];
                float size=bid.get(price);
                echo("\t");
                echo(price);
                echo("\t");
                echo(size);
            } else {
                echo("\t\t");
                echo("\t");
                echo("\t");
                echo("\t");
            }
            echo("\t");
            echo("|");
            if (askPrices.length>i){
                float price=askPrices[i];
                float size=ask.get(price);
                echo("\t\t");
                echo(price);
                echo("\t");
                echo(size);
            }
            echo("\n");
        }
        echo("\n");
        echo("\n");
    }

    private void echo(float v) {
        System.out.print(new DecimalFormat("0000.00").format(v));
    }

    private void echo(String s) {
        System.out.print(s);
    }
}
