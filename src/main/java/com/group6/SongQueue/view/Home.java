package com.group6.SongQueue.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Home {

    @GetMapping(path="/")
    public String getHomeView() {
        return "home";
    }

}
