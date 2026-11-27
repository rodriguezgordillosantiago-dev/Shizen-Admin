package com.shizen.Admin.service;

import com.shizen.Admin.model.Usuario;
import com.shizen.Admin.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + email));

        boolean enabled = usuario.getBaneado() == null || !usuario.getBaneado();

        String rol = (usuario.getRol() != null && !usuario.getRol().isBlank()) 
                     ? usuario.getRol().toUpperCase() 
                     : "USER";

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPasswordHash().trim())
                .disabled(!enabled)
                .roles(rol)
                .build();
    }
}