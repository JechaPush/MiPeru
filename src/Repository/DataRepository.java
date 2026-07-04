package Repository;

import Models.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataRepository {
    private static DataRepository instance;

    private List<Usuario> usuarios;
    private Map<String, Usuario> usuariosById;
    private Map<String, Usuario> usuariosByUsername;

    private List<Curso> cursos;
    private Map<String, Curso> cursosById;

    private List<Nota> notas;
    private List<Asistencia> asistencias;
    private List<Tarea> tareas;
    private List<Anuncio> anuncios;
    private List<Pago> pagos;
    private List<Horario> horarios;

    private DataRepository() {
        usuarios = new ArrayList<>();
        usuariosById = new HashMap<>();
        usuariosByUsername = new HashMap<>();

        cursos = new ArrayList<>();
        cursosById = new HashMap<>();

        notas = new ArrayList<>();
        asistencias = new ArrayList<>();
        tareas = new ArrayList<>();
        anuncios = new ArrayList<>();
        pagos = new ArrayList<>();
        horarios = new ArrayList<>();

        seedData();
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    private void seedData() {
        // 1. Seed Users (OOP Inheritance)
        Director dir = new Director("DIR01", "director", "director123", "Juan Pérez");
        agregarUsuario(dir);

        Docente doc1 = new Docente("DOC01", "profesor1", "profesor123", "Carlos Mendoza", "Matemáticas");
        Docente doc2 = new Docente("DOC02", "profesor2", "profesor123", "María Gómez", "Lenguaje");
        agregarUsuario(doc1);
        agregarUsuario(doc2);

        Alumno al1 = new Alumno("AL01", "alumno1", "alumno123", "Luis Torres", "5to", "A");
        Alumno al2 = new Alumno("AL02", "alumno2", "alumno123", "Ana Quispe", "5to", "A");
        Alumno al3 = new Alumno("AL03", "alumno3", "alumno123", "Diego Flores", "4to", "B");
        agregarUsuario(al1);
        agregarUsuario(al2);
        agregarUsuario(al3);

        // 2. Seed Courses
        Curso cur1 = new Curso("CUR01", "Álgebra y Aritmética", "5to", "A", "DOC01");
        Curso cur2 = new Curso("CUR02", "Geometría", "5to", "A", "DOC01");
        Curso cur3 = new Curso("CUR03", "Comprensión Lectora", "5to", "A", "DOC02");
        Curso cur4 = new Curso("CUR04", "Redacción", "4to", "B", "DOC02");
        agregarCurso(cur1);
        agregarCurso(cur2);
        agregarCurso(cur3);
        agregarCurso(cur4);

        // 3. Seed Horarios
        horarios.add(new Horario("HOR01", "CUR01", "Lunes", "08:00", "09:30", "Aula 101"));
        horarios.add(new Horario("HOR02", "CUR01", "Miércoles", "08:00", "09:30", "Aula 101"));
        horarios.add(new Horario("HOR03", "CUR02", "Martes", "09:45", "11:15", "Aula 101"));
        horarios.add(new Horario("HOR04", "CUR03", "Lunes", "09:45", "11:15", "Aula 102"));
        horarios.add(new Horario("HOR05", "CUR04", "Jueves", "08:00", "09:30", "Aula 103"));

        // 4. Seed Notas (Grades)
        // AL01 (Luis Torres) - CUR01 (Álgebra)
        notas.add(new Nota("NOT01", "AL01", "CUR01", 18.0, "A", "Bimestre 1"));
        notas.add(new Nota("NOT02", "AL01", "CUR01", 20.0, "AD", "Bimestre 2"));
        // AL02 (Ana Quispe) - CUR01 (Álgebra)
        notas.add(new Nota("NOT03", "AL02", "CUR01", 14.0, "B", "Bimestre 1"));
        notas.add(new Nota("NOT04", "AL02", "CUR01", 16.0, "A", "Bimestre 2"));
        // AL03 (Diego Flores) - CUR04 (Redacción)
        notas.add(new Nota("NOT05", "AL03", "CUR04", 10.0, "C", "Bimestre 1"));

        // 5. Seed Asistencia (Attendance)
        asistencias.add(new Asistencia("ASIS01", "AL01", "2026-07-01", "ASISTENCIA", "Llegó temprano", false));
        asistencias.add(new Asistencia("ASIS02", "AL02", "2026-07-01", "TARDANZA", "10 minutos de retraso", false));
        asistencias.add(new Asistencia("ASIS03", "AL03", "2026-07-01", "FALTA", "Inasistencia justificada", false));
        // Docente attendance
        asistencias.add(new Asistencia("ASIS04", "DOC01", "2026-07-01", "ASISTENCIA", "Clase dictada con éxito", true));
        asistencias.add(new Asistencia("ASIS05", "DOC02", "2026-07-01", "ASISTENCIA", "Clase dictada con éxito", true));

        // 6. Seed Tareas (Homework)
        Tarea tar1 = new Tarea("TAR01", "CUR01", "Ecuaciones Cuadráticas", "Resolver los ejercicios de la página 45 del libro de Álgebra.", "2026-07-10", "guia_ecuaciones.pdf");
        // Seed submission
        Tarea.Entrega ent1 = new Tarea.Entrega("AL01", "solucion_ecuaciones_luis.pdf", "2026-07-02", "AD", "Excelente resolución paso a paso.", "CALIFICADO");
        Tarea.Entrega ent2 = new Tarea.Entrega("AL02", "tarea_ana.pdf", "2026-07-03", "", "", "ENVIADO");
        tar1.agregarEntrega(ent1);
        tar1.agregarEntrega(ent2);
        tareas.add(tar1);

        Tarea tar2 = new Tarea("TAR02", "CUR03", "Lectura Crítica", "Leer el ensayo adjunto y redactar una opinión de 2 páginas.", "2026-07-12", "ensayo_comprension.pdf");
        tareas.add(tar2);

        // 7. Seed Anuncios (Announcements)
        anuncios.add(new Anuncio("AN01", "Inicio del II Bimestre", "Les damos la bienvenida al inicio del segundo bimestre académico. Éxitos a todos.", "2026-06-15", "Director General"));
        anuncios.add(new Anuncio("AN02", "Exámenes Bimestrales", "Los exámenes del II Bimestre se realizarán desde el 14 de julio. Estudiar los temarios.", "2026-07-01", "Docente Carlos Mendoza"));

        // 8. Seed Pagos
        // AL01 (Luis Torres)
        pagos.add(new Pago("PAG01", "AL01", 350.00, "Mensualidad Junio", "2026-06-30", true, "2026-06-28"));
        pagos.add(new Pago("PAG02", "AL01", 350.00, "Mensualidad Julio", "2026-07-31", false, ""));
        // AL02 (Ana Quispe)
        pagos.add(new Pago("PAG03", "AL02", 350.00, "Mensualidad Junio", "2026-06-30", true, "2026-06-29"));
        pagos.add(new Pago("PAG04", "AL02", 350.00, "Mensualidad Julio", "2026-07-31", false, ""));
        // AL03 (Diego Flores)
        pagos.add(new Pago("PAG05", "AL03", 350.00, "Mensualidad Junio", "2026-06-30", false, ""));
    }

    public void agregarUsuario(Usuario u) {
        usuarios.add(u);
        usuariosById.put(u.getId(), u);
        usuariosByUsername.put(u.getUsername(), u);
    }

    public void eliminarUsuario(String id) {
        Usuario u = usuariosById.remove(id);
        if (u != null) {
            usuarios.remove(u);
            usuariosByUsername.remove(u.getUsername());
        }
    }

    public void agregarCurso(Curso c) {
        cursos.add(c);
        cursosById.put(c.getId(), c);
    }

    public void eliminarCurso(String id) {
        Curso c = cursosById.remove(id);
        if (c != null) {
            cursos.remove(c);
        }
    }

    // Getters for Collections
    public List<Usuario> getUsuarios() { return usuarios; }
    public Map<String, Usuario> getUsuariosById() { return usuariosById; }
    public Map<String, Usuario> getUsuariosByUsername() { return usuariosByUsername; }
    public List<Curso> getCursos() { return cursos; }
    public Map<String, Curso> getCursosById() { return cursosById; }
    public List<Nota> getNotas() { return notas; }
    public List<Asistencia> getAsistencias() { return asistencias; }
    public List<Tarea> getTareas() { return tareas; }
    public List<Anuncio> getAnuncios() { return anuncios; }
    public List<Pago> getPagos() { return pagos; }
    public List<Horario> getHorarios() { return horarios; }
}
