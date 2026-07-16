package com.miperu.model;

public class Asistencia {
    private String id;
    private String idUsuario;
    private String fecha;
    private String estado;
    private String observacion;
    private boolean esDocente;

    public Asistencia(String id, String idUsuario, String fecha, String estado, String observacion, boolean esDocente) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.fecha = fecha;
        this.estado = estado;
        this.observacion = observacion;
        this.esDocente = esDocente;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public boolean isEsDocente() {
        return esDocente;
    }

    public void setEsDocente(boolean esDocente) {
        this.esDocente = esDocente;
    }
}
