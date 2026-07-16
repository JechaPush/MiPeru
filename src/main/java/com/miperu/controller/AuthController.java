package com.miperu.controller;

import com.miperu.exception.LoginException;
import com.miperu.model.Usuario;
import com.miperu.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        try {
            Usuario user = authService.login(username, password);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", user.getId(),
                "nombre", user.getNombre(),
                "rol", user.getRol()
            ));
        } catch (LoginException e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
