package com.klein.btc.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mresc on 05.11.17.
 */
public class ExchangePair {
    private static final Map<Exchange, Map<Exchange, ExchangePair>> instances=new HashMap<>();
    private final Exchange exchange1;
    private final Exchange exchange2;

    private ExchangePair(Exchange exchange1, Exchange exchange2) {
        this.exchange1=exchange1;
        this.exchange2=exchange2;
    }

    public Exchange getExchange1() {
        return exchange1;
    }

    public Exchange getExchange2() {
        return exchange2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExchangePair that = (ExchangePair) o;

        if (exchange1 != that.exchange1) return false;
        return exchange2 == that.exchange2;
    }

    @Override
    public int hashCode() {
        int result = exchange1.ordinal();
        result = 31 * result + exchange2.ordinal();
        return result;
    }

    public static ExchangePair getInstance(Exchange exchange1, Exchange exchange2){
        Exchange e1, e2;
        if (exchange1.ordinal()>exchange2.ordinal()){
            e1=exchange2;
            e2=exchange1;
        } else {
            e1=exchange1;
            e2=exchange2;
        }

        Map<Exchange, ExchangePair> pairs= instances.get(e1);
        if (pairs==null){
            pairs=new HashMap<>();
            instances.put(e1, pairs);
        }

        ExchangePair pair = pairs.get(e2);
        if (pair==null){
            pair=new ExchangePair(e1,e2);
            pairs.put(e2, pair);
        }

        return pair;
    }
}
