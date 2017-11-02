package com.klein.btc.gdax;

import java.util.Arrays;

public class SubscribeRequest extends Request {
    private final String type="subscribe";
    private String[] product_ids;
    private Object[] channels;

    public String getType() {
        return type;
    }

    public String[] getProduct_ids() {
        return product_ids;
    }

    public void setProduct_ids(String...product_ids) {
        this.product_ids = product_ids;
    }

    public Object[] getChannels() {
        return channels;
    }

    public void setChannels(Object...channels) {
        this.channels = channels;
    }

    public void addChannels(Object...channels) {
        int oldLen=this.channels.length;
        this.channels = Arrays.copyOf(this.channels, this.channels.length+channels.length);
        for(int i=0;i<channels.length;i++){
            this.channels[i+oldLen]=channels[i];
        }
    }
}
