package com.klein.btc;

import com.klein.btc.model.ExchangePair;
import com.klein.btc.model.Product;

/**
 * Created by mresc on 05.11.17.
 */
public class ArbitrageOpportunity {

    private final ExchangePair exchanges;
    private final Product product;
    private float difference;
    public ArbitrageOpportunity(ExchangePair exchanges, Product product) {
        this.exchanges = exchanges;
        this.product = product;
    }

    public float getDifference() {
        return difference;
    }

    public void setDifference(float difference) {
        this.difference = difference;
    }

    public ExchangePair getExchanges() {
        return exchanges;
    }

    public Product getProduct() {
        return product;
    }
}
