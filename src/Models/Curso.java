package Models;

public class Curso {
    private String id;
    private String nombre;
    private String grado;
    private String seccion;
    private String idDocente;

    public Curso(String id, String nombre, String grado, String seccion, String idDocente) {
        this.id = id;
        this.nombre = nombre;
        this.grado = grado;
        this.seccion = seccion;
        this.idDocente = idDocente;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public String getIdDocente() {
        return idDocente;
    }

    public void setIdDocente(String idDocente) {
        this.idDocente = idDocente;
    }
}
