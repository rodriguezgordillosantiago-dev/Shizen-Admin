package com.shizen.Admin.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.shizen.Admin.model.Usuario;
import com.shizen.Admin.repository.UsuariosRepository;

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


// Post /usuarios/{id}/banear -> busca por ID, coloca baneado = true y responde en JSON (sin recargar la pagina)
@PostMapping("/usuarios/{id}/banear")
@ResponseBody
public ResponseEntity<?> banearUsuario(
        @PathVariable Integer id,
        @RequestParam(required = false) String motivo) {
    Usuario u = usuariosRepository.findById(id).orElseThrow();
    u.setBaneado(true);
    u.setMotivoBaneo(motivo);
    u.setFechaBaneo(LocalDateTime.now());
    usuariosRepository.save(u);

    // Retorna la respuesta en formato JSON para que el JS muestre el SweetAlert directamente sin necesidad de recargar la pagina
    return ResponseEntity.ok(Map.of(
        "success", true,
        "accion", "baneado",
        "nombre", u.getNombre()
    ));
}

// Post /usuarios/{id}/desbanear -> reactiva la cuenta y responde en JSON (sin recargar la pagina)
@PostMapping("/usuarios/{id}/desbanear")
@ResponseBody
public ResponseEntity<?> desbanearUsuario(@PathVariable Integer id) {
    Usuario u = usuariosRepository.findById(id).orElseThrow();
    u.setBaneado(false);
    u.setMotivoBaneo(null);
    u.setFechaBaneo(null);
    usuariosRepository.save(u);

    // Regresa la respuesta en formato JSON para que el JS muestre el SweetAlert directamente
    return ResponseEntity.ok(Map.of(
        "success", true,
        "accion", "desbaneado",
        "nombre", u.getNombre()
    ));
}
}