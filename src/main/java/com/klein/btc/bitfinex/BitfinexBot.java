package com.klein.btc.bitfinex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klein.btc.model.Exchange;
import com.klein.btc.OrderBook;
import com.klein.btc.OrderBookListener;
import com.klein.btc.model.Product;
import com.klein.ta.Timeframe;
import com.klein.websocket.CustomWebsocketClient;
import com.klein.websocket.WebSocketListener;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class BitfinexBot implements WebSocketListener {
    private static final Logger LOG= LoggerFactory.getLogger("tradelog.bitfinex");

    private ObjectMapper mapper = new ObjectMapper();
    private TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
    private Product pair;
    private Map<Integer, Product> bookSubscriptions =new HashMap<>();
    private Timeframe timeframe;
    private double balance=1000;
    private double orderSize=0.01;
    private double position=0.0;
    private double openPrice=0.0;
    private CustomWebsocketClient websocketClient;
    private double orderCommission=0.004;

    private Map<Product, OrderBook> orderBooks=new HashMap<>();
    private OrderBookListener orderBookListener;

    public BitfinexBot(Product pair, OrderBookListener orderBookListener, double orderSize, double orderCommission) {
        this.pair=pair;
        this.orderBookListener = orderBookListener;
        this.orderSize=orderSize;
        this.orderCommission = orderCommission;

        connectToExchange();
    }


    private void connectToExchange() {
        bookSubscriptions.clear();
        websocketClient = new CustomWebsocketClient("wss://api.bitfinex.com:443/ws/2", this);
    }


    public static void main(String[] args){
        new BitfinexBot(Product.BTCUSD, null, 0.01, 0.004);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOG.trace("Connected");
    }

    @Override
    public void onMessage(String message) {
        LOG.trace("onMessage: {}", message);
        if (message.charAt(0)=='[') {
            int channelEndPos=message.indexOf(",");
            int channelId=Integer.parseInt(message.substring(1, channelEndPos));
            if (this.bookSubscriptions.containsKey(channelId)){

                String payload=message.substring(channelEndPos+1, message.length()-1);
                LOG.trace("Payload: {}", payload);
                // check if not hb (heart beat) message
                if (!payload.equals("\"hb\"")){
                    payload=payload.substring(1, payload.length()-1);
                    boolean history = false;
                    if (payload.charAt(0)=='['){
                        payload=payload.substring(1, payload.length()-1);
                        history=true;
                    }
                    LOG.trace("Channel update: {}", payload);
                    Product product=bookSubscriptions.get(channelId);
                    OrderBook orderBook = getOrderBook(product);

                    // split candles
                    String[] updates = payload.split("\\],\\[");
                    for (int i=updates.length-1;i>=0;i--) {
                        String candle=updates[i];
                        //split candle fields: timestamp,open,close,high,low,volume
                        String[] parts = candle.split(",");
                        float price=Float.parseFloat(parts[0]);
                        int count=Integer.parseInt(parts[1]);
                        float size=Float.parseFloat(parts[2]);
                        if (count>0){
                            if (size>0){
                                orderBook.addBid(price, size);
                            } else {
                                orderBook.addAsk(price, -size);
                            }
                        } else {
                            if (size>0){
                                orderBook.removeBid(price);
                            } else {
                                orderBook.removeAsk(price);
                            }
                        }
                    }
//                    orderBook.dump();
                }
            }
        } else {
            try {
                HashMap<String,Object> o = mapper.readValue(message, typeRef);
                if (LOG.isTraceEnabled())
                    LOG.trace("Parsed message: {}", Arrays.toString(o.entrySet().toArray()));
                processMessage(o);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void sell(double orderSize, double price) {
        if (position==0){
            openPrice=price;
            balance-= orderSize * price;
            balance-=(orderSize* price)*orderCommission;
        } else if (position<0) {
            openPrice = (openPrice * (-position) + orderSize * price) / (-position + orderSize);
            balance -= orderSize * price;
            balance-=(orderSize* price)*orderCommission;
        } else {
            balance+= orderSize * price;
        }
        position -= orderSize;
        LOG.info("Sell {} {}@{}, new position: {}, new balance: {}", orderSize, pair, price, position, balance);
    }

    private void buy(double orderSize, double price) {
        if (position==0){
            openPrice=price;
            balance-=orderSize* price;
            balance-=(orderSize* price)*orderCommission;
        } else if (position>0){
            openPrice=(openPrice*position+orderSize*price)/(position+orderSize);
            balance-=orderSize* price;
            balance-=(orderSize* price)*orderCommission;
        } else {
            balance+=orderSize* price;
        }
        position+=orderSize;
        LOG.info("Buy {} {}@{}, new position: {}, new balance: {}", orderSize, pair, price, position, balance);
    }

    private void processMessage(HashMap<String, Object> event) {
        String eventType= (String) event.get("event");
        if (eventType.equals("info")){
            if (event.containsKey("version") && !event.get("version").equals(2)){
                throw new RuntimeException("Unexpected version: "+event.get("version"));
            } else {
                subscribeChannel();
            }
        } else if (eventType.equals("subscribed")){
            if (event.get("channel").equals("book") && event.get("symbol").equals("t"+pair.getBitfinexCode())){
                int channelId = (int) event.get("chanId");
                bookSubscriptions.put(channelId, pair);
            }
        }
    }

    private void subscribeChannel() {
        SubscribeRequest request=new SubscribeRequest("book", pair.getBitfinexCode(),"P0", "F0","25");
        send(request);
    }

    private void send(BitfinexRequest msg) {
        try {
            String message=mapper.writeValueAsString(msg);
            LOG.trace("Sending: {}", message);
            websocketClient.send(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOG.trace("onClose, code: {}, reason: {}, remote: {}", code, reason, remote);
        if (remote==true) {
            connectToExchange();
        }
    }

    @Override
    public void onError(Exception ex) {
        LOG.error("onError: "+ex.getMessage(), ex);
    }

    public OrderBook getOrderBook(Product product){
        OrderBook orderBook = orderBooks.get(product);
        if (orderBook==null) {
            orderBook=new OrderBook(Exchange.BITFINEX, product, orderBookListener);
            this.orderBooks.put(product, orderBook);
        }
        return orderBook;
    }


}
