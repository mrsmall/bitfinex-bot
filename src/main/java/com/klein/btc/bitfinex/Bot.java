package com.klein.btc.bitfinex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class Bot extends WebSocketClient {
    private static final Logger LOG= LoggerFactory.getLogger(Bot.class);

    private ObjectMapper mapper = new ObjectMapper();
    private TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
    private String pair;
    private List<Integer> subscriptions =new ArrayList<>();
    private String timeframe;


    private Bot(URI serverUri, Map<String,String> httpHeaders) {
        super(serverUri, new Draft_6455(), httpHeaders, 60000);
        try {
            setSocket(SSLSocketFactory.getDefault().createSocket(serverUri.getHost(), serverUri.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            connectBlocking();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Bot(String pair, String timeframe) {
        this(URI.create("wss://api.bitfinex.com:443/ws/2"), new HashMap<>());
        this.pair=pair;
        this.timeframe = timeframe;
    }

    public static void main(String[] args){
        new Bot("BTCUSD", "1m");
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
            if (this.subscriptions.contains(channelId)){
                String payload=message.substring(channelEndPos+1, message.length()-1);
                LOG.trace("Payload: {}", payload);
                // check if not hb (heart beat) message
                if (!payload.equals("\"hb\"")){
                    payload=payload.substring(channelEndPos+2, payload.length()-2);
                    LOG.trace("Channel update: {}", message);
                    // split candles
                    String[] candles = payload.split("\\],\\[");
                    for (String candle : candles) {
                        //split candle fields: timestamp,open,close,high,low,volume
                        String[] parts = candle.split(",");
                        processCandle(channelId, Long.parseLong(parts[0]),Double.parseDouble(parts[1]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[2]),Double.parseDouble(parts[5]));
                    }
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

    /**
     * @param channelId
     * @param ts
     * @param open
     * @param high
     * @param low
     * @param close
     * @param volume
     */
    private void processCandle(int channelId, long ts, double open, double high, double low, double close, double volume) {

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
            if (event.get("key").equals("trade:"+timeframe+":t"+pair)){
                subscriptions.add((int) event.get("chanId"));
            }
        }
    }

    private void subscribeChannel() {
        Map<String, Object> msg=new HashMap<>();
        msg.put("event", "subscribe");
        msg.put("channel", "candles");
        msg.put("key", "trade:"+timeframe+":t"+pair);
        send(msg);
    }

    private void send(Map<String, Object> msg) {
        try {
            String message=mapper.writeValueAsString(msg);
            LOG.trace("Sending: {}", message);
            send(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOG.trace("onClose, code: {}, reason: {}, remote: {}", code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        LOG.error("onError: "+ex.getMessage(), ex);
    }
}
