package Models;

public class Docente extends Usuario {
    private String especialidad;

    public Docente(String id, String username, String password, String nombre, String especialidad) {
        super(id, username, password, nombre, "Profesor");
        this.especialidad = especialidad;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    @Override
    public String obtenerDetalles() {
        return "Docente: " + getNombre() + " (Especialidad: " + especialidad + ")";
    }
}
