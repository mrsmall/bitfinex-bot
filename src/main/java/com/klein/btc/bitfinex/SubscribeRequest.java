package com.klein.btc.bitfinex;

public class SubscribeRequest extends BitfinexRequest {
    private String event="subscribe";
    private String channel;
    private String symbol;
    private String prec;
    private String freq;
    private String len;

    public SubscribeRequest(String channel, String symbol, String prec, String freq, String len) {
        this.channel = channel;
        this.symbol = symbol;
        this.prec = prec;
        this.freq = freq;
        this.len = len;
    }

    public String getEvent() {
        return event;
    }

    public String getChannel() {
        return channel;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getPrec() {
        return prec;
    }

    public String getFreq() {
        return freq;
    }

    public String getLen() {
        return len;
    }
}
