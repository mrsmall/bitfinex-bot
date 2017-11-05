package com.klein.btc.model;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Created by mresc on 05.11.17.
 */
@Embeddable
public class TicksId implements Serializable{
    private Exchange exchange;
    private Product product;
    private long timestamp;

    public TicksId() {
    }

    public TicksId(Exchange exchange, Product product, long timestamp) {
        this();
        this.exchange = exchange;
        this.product = product;
        this.timestamp = timestamp;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public Product getProduct() {
        return product;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TicksId ticksId = (TicksId) o;

        if (timestamp != ticksId.timestamp) return false;
        if (exchange != ticksId.exchange) return false;
        return product == ticksId.product;
    }

    @Override
    public int hashCode() {
        int result = exchange.hashCode();
        result = 31 * result + product.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
