package com.miperu.repository;

import com.miperu.model.Usuario;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UsuarioRepository {
    private final List<Usuario> usuarios = new ArrayList<>();
    private final Map<String, Usuario> usuariosById = new HashMap<>();
    private final Map<String, Usuario> usuariosByUsername = new HashMap<>();

    public void save(Usuario u) {
        deleteById(u.getId());
        usuarios.add(u);
        usuariosById.put(u.getId(), u);
        usuariosByUsername.put(u.getUsername(), u);
    }

    public void deleteById(String id) {
        Usuario u = usuariosById.remove(id);
        if (u != null) {
            usuarios.remove(u);
            usuariosByUsername.remove(u.getUsername());
        }
    }

    public List<Usuario> findAll() {
        return usuarios;
    }

    public Usuario findById(String id) {
        return usuariosById.get(id);
    }

    public Usuario findByUsername(String username) {
        return usuariosByUsername.get(username);
    }
}
