package Models;

import java.util.HashMap;
import java.util.Map;

public class Tarea {
    private String id;
    private String idCurso;
    private String titulo;
    private String descripcion;
    private String fechaEntrega;
    private String archivoAdjunto;
    private Map<String, Entrega> entregas; // Key: idAlumno, Value: Entrega

    public Tarea(String id, String idCurso, String titulo, String descripcion, String fechaEntrega, String archivoAdjunto) {
        this.id = id;
        this.idCurso = idCurso;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaEntrega = fechaEntrega;
        this.archivoAdjunto = archivoAdjunto;
        this.entregas = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdCurso() {
        return idCurso;
    }

    public void setIdCurso(String idCurso) {
        this.idCurso = idCurso;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(String fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public String getArchivoAdjunto() {
        return archivoAdjunto;
    }

    public void setArchivoAdjunto(String archivoAdjunto) {
        this.archivoAdjunto = archivoAdjunto;
    }

    public Map<String, Entrega> getEntregas() {
        return entregas;
    }

    public void setEntregas(Map<String, Entrega> entregas) {
        this.entregas = entregas;
    }

    public void agregarEntrega(Entrega entrega) {
        this.entregas.put(entrega.getIdAlumno(), entrega);
    }

    public static class Entrega {
        private String idAlumno;
        private String archivoEnviado;
        private String fechaEnvio;
        private String calificacion; // AD, A, B, C or empty
        private String comentarios;
        private String estado; // ENVIADO, CALIFICADO

        public Entrega(String idAlumno, String archivoEnviado, String fechaEnvio, String calificacion, String comentarios, String estado) {
            this.idAlumno = idAlumno;
            this.archivoEnviado = archivoEnviado;
            this.fechaEnvio = fechaEnvio;
            this.calificacion = calificacion;
            this.comentarios = comentarios;
            this.estado = estado;
        }

        public String getIdAlumno() {
            return idAlumno;
        }

        public void setIdAlumno(String idAlumno) {
            this.idAlumno = idAlumno;
        }

        public String getArchivoEnviado() {
            return archivoEnviado;
        }

        public void setArchivoEnviado(String archivoEnviado) {
            this.archivoEnviado = archivoEnviado;
        }

        public String getFechaEnvio() {
            return fechaEnvio;
        }

        public void setFechaEnvio(String fechaEnvio) {
            this.fechaEnvio = fechaEnvio;
        }

        public String getCalificacion() {
            return calificacion;
        }

        public void setCalificacion(String calificacion) {
            this.calificacion = calificacion;
        }

        public String getComentarios() {
            return comentarios;
        }

        public void setComentarios(String comentarios) {
            this.comentarios = comentarios;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }
    }
}
