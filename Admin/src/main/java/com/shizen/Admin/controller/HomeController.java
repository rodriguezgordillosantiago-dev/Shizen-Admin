package com.shizen.Admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador sencillo que asigna la URL raíz ("/") a una vista predeterminada.
 * Puedes cambiar el nombre de la vista por cualquier plantilla de Thymeleaf
 * ubicada en src/main/resources/templates.
 */
@Controller
public class HomeController {

    /**
     * Asigna la ruta raíz a la página de inicio de sesión o a otra página inicial.
     */
    @GetMapping("/")
    public String home() {
        // Devuelve el nombre de la plantilla de Thymeleaf sin la extensión .html.
        // Por ejemplo, "login" mostrará src/main/resources/templates/login.html
        return "login";
    }
}
