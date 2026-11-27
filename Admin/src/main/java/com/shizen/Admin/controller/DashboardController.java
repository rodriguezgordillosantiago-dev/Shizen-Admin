package com.shizen.Admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    /** Redirige la entrada general del panel a la pantalla de usuarios. */
    @GetMapping("/dashboard")
    public String dashboard() { 
        return "redirect:/usuarios";
    }
    
    /** Sirve la pantalla de estadísticas; sus datos se cargan desde la API. */
    @GetMapping({"/estadisticas", "/estadisticas.html"})
    public String estadisticas() {
        return "estadisticas";
    }
    

}