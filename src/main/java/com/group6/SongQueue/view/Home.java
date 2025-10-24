package com.group6.SongQueue.view;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Home {

    @Value("${test:#{null}}")
    private String testEnv;
    
    @GetMapping(path="/")
    public String getHomeView() {
        System.out.println(testEnv);
        return "home";
    }

}
