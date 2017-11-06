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
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    @Autowired
    TicksRepository ticksRepository;

    @RequestMapping("/index.html")
    public String indexAction(ModelMap model) {
        model.addAttribute("name", "TEST");
        return "index";
    }

}
