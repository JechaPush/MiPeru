package com.miperu.model;

public class Alumno extends Usuario {
    private String grado;
    private String seccion;

    public Alumno(String id, String username, String password, String nombre, String grado, String seccion) {
        super(id, username, password, nombre, "Alumno");
        this.grado = grado;
        this.seccion = seccion;
    }

    public String getGrado() {
        return grado;
    }

    public void setGrado(String grado) {
        this.grado = grado;
    }

    public String getSeccion() {
        return seccion;
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    @Override
    public String obtenerDetalles() {
        return "Estudiante: " + getNombre() + " (Grado: " + grado + ", Sección: " + seccion + ")";
    }
}
