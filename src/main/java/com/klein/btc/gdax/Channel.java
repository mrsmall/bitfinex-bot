package com.klein.btc.gdax;

public class Channel {
    private final String name;
    private final String[] product_ids;

    public Channel(String name, String...product_ids) {
        this.name = name;
        this.product_ids = product_ids;
    }

    public String getName() {
        return name;
    }

    public String[] getProduct_ids() {
        return product_ids;
    }
}
