package com.miperu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebRedirectController {

    @GetMapping("/")
    public String index() {
        return "redirect:/html/login.html";
    }
}
