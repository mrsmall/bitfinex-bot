package com.klein.btc.controller;

import com.klein.btc.model.Exchange;
import com.klein.btc.model.Product;
import com.klein.btc.model.Ticks;
import com.klein.btc.repository.TicksRepository;
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

    @RequestMapping("/{product}/{exchange1}-{exchange2}/{ts1}-{ts2}")
    @ResponseBody
    public List<Object[]> indexAction(@PathVariable("product") String productName, @PathVariable("exchange1") String exchange1name, @PathVariable("exchange2") String exchange2name, @PathVariable("ts1") long ts1, @PathVariable("ts2") long ts2) {
        Exchange exchange1=Exchange.valueOf(exchange1name);
        Exchange exchange2=Exchange.valueOf(exchange2name);
        Product product=Product.valueOf(productName);
        List<Ticks> ticks1=ticksRepository.findById_exchangeAndId_productAndId_timestampBetweenOrderById_timestampDesc(exchange1, product, ts1, ts2, PageRequest.of(0, 1000)).getContent();
        List<Ticks> ticks2=ticksRepository.findById_exchangeAndId_productAndId_timestampBetweenOrderById_timestampDesc(exchange2, product, ts1, ts2, PageRequest.of(0, 1000)).getContent();

        int interval=1000;
        SortedMap<Long, Map<Exchange, Ticks>> sorted=combine(ticks1, ticks2);
        List<Object[]> spread=new ArrayList<>();
        for (Long ts : sorted.keySet()) {
            Map<Exchange, Ticks> timestampTicks = sorted.get(ts);
            float value;
            if (timestampTicks.size()!=2){
                value=0f;
                spread.add(new Object[]{ts, value});
            } else {
                value=(timestampTicks.get(exchange1).getOpenAsk()-timestampTicks.get(exchange2).getOpenBid())*100/timestampTicks.get(exchange2).getOpenBid();
                spread.addAll(getValues(ts, interval, timestampTicks.get(exchange1), timestampTicks.get(exchange2)));
            }
        }

        return spread;
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
