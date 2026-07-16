package com.miperu.model;

public class Director extends Usuario {
    public Director(String id, String username, String password, String nombre) {
        super(id, username, password, nombre, "Director");
    }

    @Override
    public String obtenerDetalles() {
        return "Director de la Institución: " + getNombre();
    }
}
