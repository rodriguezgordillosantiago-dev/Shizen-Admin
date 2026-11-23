package com.shizen.Admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard() { 
        return "redirect:/usuarios";
    }

    @GetMapping({"/pedidos", "/pedidos.html"})
    public String pedidos() {
        return "pedidos";
    }
    
    @GetMapping({"/estadisticas", "/estadisticas.html"})
    public String estadisticas() {
        return "estadisticas";
    }
    

}