package com.klein.btc;

public interface OrderBookListener {
    void onAskChanged(float price, float size);
    void onBidChanged(float price, float size);
}
