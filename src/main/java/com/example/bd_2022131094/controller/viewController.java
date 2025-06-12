package com.example.bd_2022131094.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hdfs")
public class viewController {
    @GetMapping({"/","/index"})
    public String index(Model model) {
        model.addAttribute("defaultPath", "/user/hadoop");
        return "index";
    }
}
