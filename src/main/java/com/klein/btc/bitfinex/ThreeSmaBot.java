package com.klein.btc.bitfinex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klein.ta.Series;
import com.klein.ta.Timeframe;
import com.klein.websocket.CustomWebsocketClient;
import com.klein.websocket.WebSocketListener;
import org.java_websocket.handshake.ServerHandshake;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ThreeSmaBot implements WebSocketListener {
    private static final Logger LOG= LoggerFactory.getLogger("tradelog.3sma");

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
    private double orderSize=0.01;
    private double position=0.0;
    private double openPrice=0.0;
    private CustomWebsocketClient websocketClient;
    private double orderCommission=0.004;

    public ThreeSmaBot(String pair, Timeframe timeframe, int fastSmaPeriod, int slowSmaPeriod, int trendSmaPeriod, double orderSize, double orderCommission) {
        this.pair=pair;
        this.timeframe = timeframe;
        this.fastSmaPeriod=fastSmaPeriod;
        this.slowSmaPeriod=slowSmaPeriod;
        this.trendSmaPeriod=trendSmaPeriod;
        this.orderSize=orderSize;
        this.orderCommission = orderCommission;
        maxBars=Math.max(fastSmaPeriod, slowSmaPeriod);
        maxBars=Math.max(slowSmaPeriod, trendSmaPeriod);

        connectToExchange();
    }


    private void connectToExchange() {
        subscriptions.clear();
        websocketClient = new CustomWebsocketClient("wss://api.bitfinex.com:443/ws/2", this);
    }


    public static void main(String[] args){
        new ThreeSmaBot("BTCUSD", Timeframe.M1, 2, 5, 15, 0.01, 0.004);
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
            LOG.trace("SMA trend is rising: {}", smaTrendIsRising);
            boolean smaTrendIsFalling=smaTrendLast<smaTrendPrev;
            LOG.trace("SMA trend is falling: {}", smaTrendIsFalling);
            boolean smaSlowIsHigherAsTrend=smaSlowLast>smaTrendLast;
            LOG.trace("SMA slow is > SMA trend: {}", smaSlowIsHigherAsTrend);
            boolean smaFastIsHigherAsSlow = smaFastLast > smaSlowLast;
            LOG.trace("SMA fast is > SMA slow: {}", smaFastIsHigherAsSlow);
            boolean smaFastWasLowerOrEqualAsSlow = smaFastPrev<=smaSlowPrev;
            LOG.trace("SMA fast was <= SMA slow: {}", smaFastWasLowerOrEqualAsSlow);
            boolean smaSlowIsLowerAsTrend=smaSlowLast<smaTrendLast;
            LOG.trace("SMA slow is < SMA trend: {}", smaSlowIsLowerAsTrend);
            boolean smaFastIsLowerAsSlow = smaFastLast < smaSlowLast;
            LOG.trace("SMA fast is < SMA slow: {}", smaFastIsLowerAsSlow);
            boolean smaFastWasHigherOrEqualAsSlow = smaFastPrev>=smaSlowPrev;
            LOG.trace("SMA fast was >= SMA slow: {}", smaFastWasHigherOrEqualAsSlow);

            if (position==0){
                LOG.debug("No open positions, checking entries on {}...", pair);

                if (smaTrendIsRising || smaSlowIsHigherAsTrend){
                    LOG.debug("Trendline rising or slow SMA is over trend line, checking LONG entry");

                    if (smaFastIsHigherAsSlow && smaFastWasLowerOrEqualAsSlow){
                        buy(orderSize, close);
                    }
                } else if (smaTrendIsFalling || smaSlowIsLowerAsTrend){
                    LOG.debug("Trendline falling or slow SMA is under the trend line, checking SHORT entry");

                    if (smaFastIsLowerAsSlow && smaFastWasHigherOrEqualAsSlow){
                        sell(orderSize, close);
                    }
                }
            } else {
                if (position>0){
                    LOG.debug("Open long position: {} {}, checking exists...", position, pair);

                    if (!smaFastIsHigherAsSlow || !(smaSlowIsHigherAsTrend || smaTrendIsRising)) {
                        sell(position, close);
                    }
                } else {
                    LOG.debug("Open short position: {} {}, checking exists...", position, pair);

                    if (!smaFastIsLowerAsSlow || !(smaSlowIsLowerAsTrend || smaTrendIsFalling)) {
                        buy(-position, close);
                    }
                }
            }
        }
        if (newBar && !history && position!=0){
            double equity=balance;
            if (position>0)
                equity += s.lastClose() * position;
            else {
                equity += s.lastClose() * (-position);
            }
            LOG.info("Equity: {}, position: {}", equity, position);
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
                if (!series.containsKey(channelId))
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
}
