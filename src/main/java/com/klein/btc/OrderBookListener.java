package com.klein.btc;

public interface OrderBookListener {
    void onAskChanged(OrderBook orderBook, float price, float size);
    void onBidChanged(OrderBook orderBook, float price, float size);
}
