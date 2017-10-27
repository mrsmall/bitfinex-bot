package com.klein.btc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    @RequestMapping("/index.html")
    public String indexAction(ModelMap model) {
        model.addAttribute("name", "TEST");
        return "index";
    }

}
