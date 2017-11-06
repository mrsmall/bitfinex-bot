package com.klein.btc.controller;

import com.klein.btc.model.Exchange;
import com.klein.btc.model.Product;
import com.klein.btc.model.Ticks;
import com.klein.btc.repository.TicksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    @Autowired
    TicksRepository ticksRepository;

    @RequestMapping("/")
    public String indexAction(ModelMap model) {
        return "redirect:/spread-BTCUSD-GDAX-BITFINEX-"+(System.currentTimeMillis()-3600000)+"-"+System.currentTimeMillis()+".html";
    }

    @RequestMapping("/spread-{product}-{exchange1}-{exchange2}-{ts1}-{ts2}.html")
    public String spreadAction(@PathVariable("product") String productName, @PathVariable("exchange1") String exchange1name, @PathVariable("exchange2") String exchange2name, @PathVariable("ts1") long ts1, @PathVariable("ts2") long ts2, ModelMap model) {
        model.addAttribute("product", productName);
        model.addAttribute("exchange1", exchange1name);
        model.addAttribute("exchange2", exchange2name);
        model.addAttribute("ts1", ts1);
        model.addAttribute("ts1_10m", ts2-(10*60*1000));
        model.addAttribute("ts1_30m", ts2-(30*60*1000));
        model.addAttribute("ts1_60m", ts2-(60*60*1000));
        model.addAttribute("ts1_240m", ts2-(240*60*1000));
        model.addAttribute("ts1_480m", ts2-(480*60*1000));
        model.addAttribute("ts1_960m", ts2-(960*60*1000));
        long tsDiff=ts2-ts1;
        model.addAttribute("ts1_prev", ts1-tsDiff);
        model.addAttribute("ts1_next", ts1+tsDiff);
        model.addAttribute("ts2", ts2);
        model.addAttribute("ts2_prev", ts2-tsDiff);
        model.addAttribute("ts2_next", ts2+tsDiff);
        model.addAttribute("ts_diff", tsDiff);
        return "spread";
    }

}
