package com.miperu.service;

import com.miperu.exception.LoginException;
import com.miperu.model.Usuario;
import com.miperu.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UsuarioRepository usuarioRepo;

    @Autowired
    public AuthService(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    public Usuario login(String username, String password) throws LoginException {
        Usuario usuario = usuarioRepo.findByUsername(username);

        if (usuario == null) {
            throw new LoginException("El usuario '" + username + "' no existe.");
        }

        if (!usuario.getPassword().equals(password)) {
            throw new LoginException("Contraseña incorrecta para el usuario '" + username + "'.");
        }

        return usuario;
    }
}
