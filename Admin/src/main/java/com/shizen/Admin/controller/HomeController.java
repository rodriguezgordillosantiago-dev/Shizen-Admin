package com.shizen.Admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Simple controller that maps the root URL ("/") to a default view.
 * You can change the returned view name to any Thymeleaf template
 * that exists under src/main/resources/templates.
 */
@Controller
public class HomeController {

    /**
     * Map the root path to the login page (or any other start page).
     */
    @GetMapping("/")
    public String home() {
        // Return the Thymeleaf template name without the .html extension.
        // For example, "login" will render src/main/resources/templates/login.html
        return "login";
    }
}
