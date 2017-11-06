package com.klein.btc.controller;

import com.klein.btc.model.Exchange;
import com.klein.btc.model.Product;
import com.klein.btc.model.Ticks;
import com.klein.btc.repository.TicksRepository;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/arbitrage_spread")
public class ArbitrageSpreadController {

    @Autowired
    TicksRepository ticksRepository;
    private Core core=new Core();

    @RequestMapping("/{product}/{exchange1}-{exchange2}/{ts1}-{ts2}")
    @ResponseBody
    public Object[] indexAction(@PathVariable("product") String productName, @PathVariable("exchange1") String exchange1name, @PathVariable("exchange2") String exchange2name, @PathVariable("ts1") long ts1, @PathVariable("ts2") long ts2) {
        Exchange exchange1=Exchange.valueOf(exchange1name);
        Exchange exchange2=Exchange.valueOf(exchange2name);
        Product product=Product.valueOf(productName);
        List<Ticks> ticks1=ticksRepository.findById_exchangeAndId_productAndId_timestampBetweenOrderById_timestampDesc(exchange1, product, ts1, ts2, PageRequest.of(0, 2000)).getContent();
        List<Ticks> ticks2=ticksRepository.findById_exchangeAndId_productAndId_timestampBetweenOrderById_timestampDesc(exchange2, product, ts1, ts2, PageRequest.of(0, 2000)).getContent();

        int interval=1000;
        SortedMap<Long, Map<Exchange, Ticks>> sorted=combine(ticks1, ticks2);
        List<Long> timestamps=new ArrayList<>();
        List<Float> spread=new ArrayList<>();
        for (Long ts : sorted.keySet()) {
            Map<Exchange, Ticks> timestampTicks = sorted.get(ts);
            float value;
            if (timestampTicks.size()!=2){
//                value=0f;
//                for (int i=0;i<60000;i+=interval)
//                    spread.add(new Object[]{ts+i, value});
            } else {
                value=(timestampTicks.get(exchange1).getOpenAsk()-timestampTicks.get(exchange2).getOpenBid())*100/timestampTicks.get(exchange2).getOpenBid();
                List<Object[]> values = getValues(ts, interval, timestampTicks.get(exchange1), timestampTicks.get(exchange2));
                for (Object[] objects : values) {
                    timestamps.add((Long) objects[0]);
                    spread.add((Float) objects[1]);
                }
            }
        }

        int len = spread.size();
        int stdDevPeriod=len/2;
        float[] values = new float[len];
        float[] sma= new float[len];
        float smaSum=0;
        float[] bbLow = new float[len];
        float[] bbHigh= new float[len];
        List<Float> tmp=new ArrayList<>();
        for(int i=0;i<len;i++) {
            float val=spread.get(i);
            values[i] = val;
            tmp.add(val);
            smaSum+=val;
            if (tmp.size()>stdDevPeriod)
                smaSum-=tmp.remove(0);
            if (tmp.size()==stdDevPeriod){
                float smaVal=smaSum/stdDevPeriod;
                sma[i]=smaVal;
                bbLow[i]=smaVal;
                bbHigh[i]=smaVal;
            } else{
                sma[i]=Float.MIN_VALUE;
                bbLow[i]=Float.MIN_VALUE;
                bbHigh[i]=Float.MIN_VALUE;
            }
        }

        double[] stdDev = new double[len];
        float[] oldValues =null;
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        RetCode res = core.stdDev(0, len - 1, values, stdDevPeriod, 2D, outBegIdx, outNBElement, stdDev);
        if (res == RetCode.Success) {
            // System.out.println(Arrays.toString(values));
            stdDev=createFullArray(len, outBegIdx, outNBElement, stdDev, Float.MIN_VALUE);
        }
        for(int i=stdDevPeriod-1;i<len;i++) {
            float smaVal=sma[i];
            double stdDevVal=stdDev[i];
            bbLow[i]-=stdDevVal;
            bbHigh[i]+=stdDevVal;
        }

        return new Object[]{timestamps, spread, sma, bbLow, bbHigh};
    }

    protected double[] createFullArray(int len, MInteger outBegIdx, MInteger outNBElement, double[] roc, float emptyValue) {
//        System.out.println("outBegIdx: " + outBegIdx.value);
//        System.out.println("outNBElement: " + outNBElement.value);
//        System.out.println("IN: " + Arrays.toString(roc));
        roc = Arrays.copyOf(roc, outNBElement.value);
//        System.out.println("COPY IN: " + Arrays.toString(roc));

        double[] fullRoc = new double[len];
        Arrays.fill(fullRoc, 0, outBegIdx.value, emptyValue);
        System.arraycopy(roc, 0, fullRoc, outBegIdx.value, roc.length);
//        System.out.println("OUT: " + Arrays.toString(fullRoc));
        return fullRoc;
    }



    private List<Object[]> getValues(long ts, int interval, Ticks ticks1, Ticks ticks2) {
        SortedMap<Long, Map<Exchange, Float>> sorted=new TreeMap<>();

        Ticks ticks=ticks1;
        for (int i=0;i<ticks.getTicksMillis().length;i++) {
            int millis=ticks.getTicksMillis()[i];
            long timestamp= (long) (ts+Math.floor(millis/interval)*interval);
            Map<Exchange, Float> timestampValues = sorted.get(timestamp);
            if (timestampValues ==null){
                timestampValues =new HashMap<>();
                sorted.put(timestamp, timestampValues);
            }
            timestampValues.put(ticks.getId().getExchange(), ticks.getTicksBestAsk()[i]);
        }
        ticks=ticks2;
        for (int i=0;i<ticks.getTicksMillis().length;i++) {
            int millis=ticks.getTicksMillis()[i];
            long timestamp= (long) (ts+Math.floor(millis/interval)*interval);
            Map<Exchange, Float> timestampValues = sorted.get(timestamp);
            if (timestampValues ==null){
                timestampValues =new HashMap<>();
                sorted.put(timestamp, timestampValues);
            }
            timestampValues.put(ticks.getId().getExchange(), ticks.getTicksBestBid()[i]);
        }
        List<Object[]> data=new ArrayList<>();
        for (Long timestamp: sorted.keySet()) {
            Map<Exchange, Float> timestampValues = sorted.get(timestamp);
            if (timestampValues.size()==2){
                float ask=timestampValues.get(ticks1.getId().getExchange());
                float bid=timestampValues.get(ticks2.getId().getExchange());
                data.add(new Object[]{timestamp, (ask-bid)*100/bid});
            }
        }
        return data;
    }

    private SortedMap<Long,Map<Exchange,Ticks>> combine(List<Ticks>...ticksArray) {
        SortedMap<Long, Map<Exchange, Ticks>> combined =new TreeMap<>();
        for (List<Ticks> ticks1 : ticksArray) {
            for (Ticks ticks : ticks1) {
                Map<Exchange, Ticks> timestampTicks = combined.get(ticks.getId().getTimestamp());
                if (timestampTicks==null){
                    timestampTicks=new HashMap<>();
                    combined.put(ticks.getId().getTimestamp(), timestampTicks);
                }
                timestampTicks.put(ticks.getId().getExchange(), ticks);
            }
        }
        return combined;
    }

}
