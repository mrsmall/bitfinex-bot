package com.klein.btc.bitfinex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klein.ta.Series;
import com.klein.ta.Timeframe;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.joda.time.LocalDateTime;
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
    private Timeframe timeframe;
    private Map<Integer, Series> series=new HashMap<>();
    private int maxBars=10;
    private int fastSmaPeriod=10;
    private int slowSmaPeriod=30;
    private int trendSmaPeriod=60;
    private double balance=1000;
    private double orderSize=1000;
    private double position=0;


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

    public Bot(String pair, Timeframe timeframe, int fastSmaPeriod, int slowSmaPeriod, int trendSmaPeriod, double orderSize) {
        this(URI.create("wss://api.bitfinex.com:443/ws/2"), new HashMap<>());
        this.pair=pair;
        this.timeframe = timeframe;
        this.fastSmaPeriod=fastSmaPeriod;
        this.slowSmaPeriod=slowSmaPeriod;
        this.trendSmaPeriod=trendSmaPeriod;
        this.orderSize=orderSize;
        maxBars=Math.max(fastSmaPeriod, slowSmaPeriod);
        maxBars=Math.max(slowSmaPeriod, trendSmaPeriod);
    }

    public static void main(String[] args){
        new Bot("BTCUSD", Timeframe.M1, 2, 5, 15, 0.01);
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
                    payload=payload.substring(1, payload.length()-1);
                    boolean history = false;
                    if (payload.charAt(0)=='['){
                        payload=payload.substring(1, payload.length()-1);
                        history=true;
                    }
                    LOG.trace("Channel update: {}", payload);
                    // split candles
                    String[] candles = payload.split("\\],\\[");
                    for (int i=candles.length-1;i>=0;i--) {
                        String candle=candles[i];
                        //split candle fields: timestamp,open,close,high,low,volume
                        String[] parts = candle.split(",");
                        processCandle(channelId, Long.parseLong(parts[0]),Double.parseDouble(parts[1]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[2]),Double.parseDouble(parts[5]), history);
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
    private void processCandle(int channelId, long ts, double open, double high, double low, double close, double volume, boolean history) {
        LocalDateTime date = LocalDateTime.fromDateFields(new Date(ts));
        LOG.trace("Candle timestamp: {}", date);

        Series s=getSeries(channelId);
        boolean newBar=s.isNewBar(date);
        s.addBar(date, open, high,low,close,volume);
        LOG.trace("Series: {}, lastIndex: {}", s.getSymbol(), s.getLastIndex());

        if (!history && s.isLastDate(date) && s.getClose().length>maxBars){
            double[] smaFast = s.sma(fastSmaPeriod);
            double[] smaSlow = s.sma(slowSmaPeriod);
            double[] smaTrend= s.sma(trendSmaPeriod);

//            LOG.trace("SMA trend values: {}", Arrays.toString(smaTrend));

            double smaFastLast = smaFast[smaFast.length - 1];
            double smaSlowLast = smaSlow[smaSlow.length - 1];
            double smaFastPrev = smaFast[smaFast.length - 2];
            double smaSlowPrev = smaSlow[smaSlow.length - 2];
            double smaTrendLast = smaSlow[smaSlow.length - 1];
            double smaTrendPrev = smaSlow[smaSlow.length - 2];

            boolean smaTrendIsRising=smaTrendLast>smaTrendPrev;
            LOG.info("SMA trend is rising: {}", smaTrendIsRising);
            boolean smaSlowIsHigherAsTrend=smaSlowLast>smaTrendLast;
            LOG.info("SMA slow is > SMA trend: {}", smaSlowIsHigherAsTrend);
            boolean smaFastIsHigherAsSlow = smaFastLast > smaSlowLast;
            boolean smaFastWasLowOrEqualAsSlow = smaFastPrev<=smaSlowPrev;
            LOG.info("SMA fast is > SMA slow: {}", smaFastIsHigherAsSlow);
            LOG.info("SMA fast was <= SMA slow: {}", smaFastWasLowOrEqualAsSlow);
            if ( smaFastIsHigherAsSlow && smaFastWasLowOrEqualAsSlow && (smaSlowIsHigherAsTrend && smaTrendIsRising)){
                buy(orderSize, close);
            } else if (position>0 && (!smaFastIsHigherAsSlow || !(smaSlowIsHigherAsTrend || smaTrendIsRising))) {
                sell(position, close);
            }
        }
        if (newBar && !history && position!=0){
            double equity=balance+(position>0?s.lastClose()*position:-s.lastClose()*position);
            LOG.info("Equity: {}", equity);
        }
    }

    private void sell(double orderSize, double price) {
        balance+= orderSize * price;
        position -= orderSize;
        LOG.info("Sell {} {}@{}, new balance: {}", orderSize, pair, price, balance);
    }

    private void buy(double orderSize, double price) {
        balance-=orderSize* price;
        position+=orderSize;
        LOG.info("Buy {} {}@{}, new balance: {}", orderSize, pair, price, balance);
    }

    private Series getSeries(int channelId) {
        return series.get(channelId);
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
            if (event.get("key").equals("trade:"+timeframe.getBitfinexCode()+":t"+pair)){
                int channelId = (int) event.get("chanId");
                subscriptions.add(channelId);
                series.put(channelId, new Series(pair, timeframe, maxBars+10));
            }
        }
    }

    private void subscribeChannel() {
        Map<String, Object> msg=new HashMap<>();
        msg.put("event", "subscribe");
        msg.put("channel", "candles");
        msg.put("key", "trade:"+timeframe.getBitfinexCode()+":t"+pair);
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
