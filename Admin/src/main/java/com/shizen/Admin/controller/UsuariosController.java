package com.shizen.Admin.controller;

import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.shizen.Admin.model.Usuario;
import com.shizen.Admin.repository.UsuariosRepository;
import java.util.List;

@Controller 
public class UsuariosController {

private final UsuariosRepository usuariosRepository;

// constructor para la inyección de la vista
public UsuariosController(UsuariosRepository usuariosRepository){
    this.usuariosRepository = usuariosRepository;
 }


//Get /usuarios -> lista los usuarios y los envia al html
@GetMapping({"/usuarios", "/usuarios.html"})

public String listarUsuarios(

        @RequestParam (required = false) String buscar,
        @RequestParam (required = false) String rol,
        Model model) {

        //Si se filtra por todos el valor cambia a null, para que se muestren todos
        String rolFiltro = (rol != null && !rol.equalsIgnoreCase("Todos")) ? rol : null;

        List<Usuario> lista = usuariosRepository.buscarConFiltros(buscar, rolFiltro);

        model.addAttribute("usuarios", lista);

        model.addAttribute("buscar", buscar);

//Usado para que el texto no se borre del input

        model.addAttribute("rolActual", rol!= null ? rol :"Todos");

        return "usuarios";
   
 }


// Post /usuarios/{id},/banear/->busca por ID, coloca baneado = true y lo guarda
@PostMapping ("/usuarios/{id}/banear")
public String banearUsuario(@PathVariable Integer id, @RequestParam(required = false) String motivo) {Usuario u = usuariosRepository.findById(id).orElseThrow();
    u.setBaneado(true);
    u.setMotivoBaneo(motivo);
    u.setFechaBaneo(LocalDateTime.now());
    usuariosRepository.save(u);
    return "redirect:/usuarios";
  } 
// POST /usuarios/{id}/desbanear → busca por ID, pone baneado=false y lo guarda

@PostMapping ("/usuarios/{id}/desbanear")
public String desbanearUsuario(@PathVariable Integer id) {Usuario u = usuariosRepository.findById(id).orElseThrow();
    u.setBaneado(false);
    u.setMotivoBaneo(null);
    u.setFechaBaneo(null);
    usuariosRepository.save(u);
    return "redirect:/usuarios";
}
  }