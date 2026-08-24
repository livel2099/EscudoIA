package com.livel.escudo.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {
    @RequestMapping(value={"/{path:[^\\.]*}","/{path1:[^\\.]*}/{path2:[^\\.]*}"})
    public String forward(){return "forward:/index.html";}
}
