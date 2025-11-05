package com.group6.SongQueue.view;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Home {

    @Value("${TESTING_KEY:#{null}}")
    private String TESTING_KEY;
    
    @GetMapping(path="/")
    public String getHomeView() {
        System.out.println(TESTING_KEY);
        return "home";
    }

}
