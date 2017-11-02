package com.klein.btc.gdax;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klein.btc.Product;
import com.klein.ta.Series;
import com.klein.ta.Timeframe;
import com.klein.websocket.CustomWebsocketClient;
import com.klein.websocket.WebSocketListener;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class GdaxBot implements WebSocketListener {
    private static final Logger LOG= LoggerFactory.getLogger("tradelog.gdax");

    private Map<Product, OrderBook> orderBooks=new HashMap<>();

    private ObjectMapper mapper = new ObjectMapper();
    private TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
    private Product pair;
    private List<String> l2subscriptions =new ArrayList<>();
    private Timeframe timeframe;
    private Map<Integer, Series> series=new HashMap<>();
    private double balance=1000;
    private double orderSize=0.01;
    private double position=0.0;
    private double openPrice=0.0;
    private CustomWebsocketClient websocketClient;
    private double orderCommission=0.004;

    public GdaxBot(Product product, Timeframe timeframe, double orderSize, double orderCommission) {
        this.pair=product;
        this.orderSize=orderSize;
        this.orderCommission = orderCommission;
        connectToExchange();
    }


    private void connectToExchange() {
        l2subscriptions.clear();
        websocketClient = new CustomWebsocketClient("wss://ws-feed.gdax.com:443", this);
    }


    public static void main(String[] args){
        new GdaxBot(Product.BTCUSD, Timeframe.M5, 0.01, 0.004);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOG.trace("Connected");
        subscribeChannel();
    }

    @Override
    public void onMessage(String message) {
        LOG.trace("onMessage: {}", message);
        try {
            HashMap<String,Object> o = mapper.readValue(message, typeRef);
            if (LOG.isTraceEnabled())
                LOG.trace("Parsed message: {}", Arrays.toString(o.entrySet().toArray()));
            processMessage(o);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public OrderBook getOrderBook(Product product){
        OrderBook orderBook = orderBooks.get(product);
        if (orderBook==null) {
            orderBook=new OrderBook(product);
            this.orderBooks.put(product, orderBook);
        }
        return orderBook;
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

    private Series getSeries(int channelId) {
        return series.get(channelId);
    }

    private void processMessage(HashMap<String, Object> event) {
        String eventType= (String) event.get("type");
        if (eventType.equals("snapshot")){
            String productId = (String) event.get("product_id");
            Product product=Product.getProductForGdaxCode(productId);
            OrderBook orderBook = getOrderBook(product);

            List<List<String>> bids= (List<List<String>>) event.get("bids");
            for (List<String> bid : bids) {
                float price=Float.parseFloat((String)bid.get(0));
                float size=Float.parseFloat((String)bid.get(1));

                orderBook.addBid(price, size);
            }
            List<List<String>> asks= (List<List<String>>) event.get("asks");
            for (List<String> ask : asks) {
                float price=Float.parseFloat((String) ask.get(0));
                float size=Float.parseFloat((String) ask.get(1));

                orderBook.addAsk(price, size);
            }

            orderBook.dump();
        } else if (eventType.equals("l2update")){
            String productId = (String) event.get("product_id");
            Product product=Product.getProductForGdaxCode(productId);
            OrderBook orderBook = getOrderBook(product);

            List<List<String>> changes = (List<List<String>>) event.get("changes");
            for (List<String> change : changes) {
                String type=change.get(0);
                float price=Float.parseFloat((String) change.get(1));
                float size=Float.parseFloat((String) change.get(2));
                if (size>0){
                    if (type.equals("buy")){
                        orderBook.addBid(price, size);
                    } else {
                        orderBook.addAsk(price, size);
                    }
                } else {
                    if (type.equals("buy")){
                        orderBook.removeBid(price);
                    } else {
                        orderBook.removeAsk(price);
                    }
                }
            }
            orderBook.dump();
        } else if (eventType.equals("subscriptions")){
            List<Map> channels= (List) event.get("channels");
            for (Map channel : channels) {
                if (channel.get("name").equals("level2")){
                    List<String> productIds= (List) channel.get("product_ids");
                    for (String productId : productIds) {
                        l2subscriptions.add(productId);
                    }
                }

            }
//            l2subscriptions.add(channelId);
//            if (!series.containsKey(channelId))
//                series.put(channelId, new Series(pair, timeframe, maxBars+10));
        }
    }

    private void subscribeChannel() {
        SubscribeRequest msg=new SubscribeRequest();
        msg.setProduct_ids(pair.getGdaxCode());
        msg.setChannels("level2");
        send(msg);
    }

    private void send(Request msg) {
        try {
            String message=mapper.writeValueAsString(msg);
            LOG.trace("Sending: {}", message);
            if (websocketClient!=null)
                websocketClient.send(message);
            else {
                throw new RuntimeException("WebSocket Client isn't initialized");
            }
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
}
