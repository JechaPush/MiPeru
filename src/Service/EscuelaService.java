package Service;

import Exceptions.*;
import Models.*;
import Repository.DataRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EscuelaService {
    private DataRepository repo;

    public EscuelaService() {
        this.repo = DataRepository.getInstance();
    }

    // RF01: Inicio de sesión
    public Usuario login(String username, String password) throws LoginException {
        // Uso de programación funcional (Streams y Lambda)
        Optional<Usuario> usuarioOpt = repo.getUsuarios().stream()
            .filter(u -> u.getUsername().equals(username))
            .findFirst();

        if (usuarioOpt.isEmpty()) {
            throw new LoginException("El usuario '" + username + "' no existe.");
        }

        Usuario usuario = usuarioOpt.get();
        if (!usuario.getPassword().equals(password)) {
            throw new LoginException("Contraseña incorrecta para el usuario '" + username + "'.");
        }

        return usuario;
    }

    // Regla de Negocio obligatoria: Conversión de notas
    public String determinarLetraNota(double valorNumerico) throws ReglaNegocioException {
        if (valorNumerico < 0.0 || valorNumerico > 20.0) {
            throw new ReglaNegocioException("La nota debe estar en el rango de 0 a 20. Valor recibido: " + valorNumerico);
        }

        // Reglas de negocio basadas en condicionales y rangos
        if (valorNumerico >= 19.0) {
            return "AD"; // Logro Destacado
        } else if (valorNumerico >= 15.0) {
            return "A";  // Logro Previsto
        } else if (valorNumerico >= 11.0) {
            return "B";  // En Proceso
        } else {
            return "C";  // En Inicio
        }
    }

    // RF13: Determinar el nivel general de desempeño sin calculadora
    // Reglas basadas en conteo y condiciones de la escala
    public String obtenerDesempenoGeneral(String idAlumno) {
        List<Nota> notasAlumno = repo.getNotas().stream()
            .filter(n -> n.getIdAlumno().equals(idAlumno))
            .collect(Collectors.toList());

        if (notasAlumno.isEmpty()) {
            return "Sin notas registradas";
        }

        // Uso de filter y count para aplicar reglas académicas
        long countAD = notasAlumno.stream().filter(n -> n.getValorLetra().equals("AD")).count();
        long countC = notasAlumno.stream().filter(n -> n.getValorLetra().equals("C")).count();

        // Reglas académicas simuladas
        if (countC >= 2) {
            return "En Recuperación (Requiere nivelación inmediata)";
        } else if (countAD >= (notasAlumno.size() / 2.0)) {
            return "Logro Destacado (Excelente rendimiento)";
        } else if (countC == 0) {
            return "Logro Previsto (Buen rendimiento)";
        } else {
            return "En Proceso (Rendimiento regular)";
        }
    }

    // RF19: Gestión de Matrículas (Regla de negocio: no se puede matricular si tiene pagos pendientes)
    public void registrarMatricula(String idAlumno, String grado, String seccion) throws ReglaNegocioException, RegistroNoEncontradoException {
        Alumno alumno = obtenerAlumnoPorId(idAlumno);

        // Regla lógica: Verificar si tiene deudas de meses pasados (por ejemplo, pagos no realizados anteriores a julio)
        boolean tieneDeudas = repo.getPagos().stream()
            .filter(p -> p.getIdAlumno().equals(idAlumno))
            .filter(p -> !p.isPagado())
            .anyMatch(p -> p.getConcepto().contains("Junio") || p.getConcepto().contains("Matrícula"));

        if (tieneDeudas) {
            throw new ReglaNegocioException("No se puede registrar la matrícula del alumno '" + alumno.getNombre() + "' porque registra pagos pendientes del mes anterior.");
        }

        alumno.setGrado(grado);
        alumno.setSeccion(seccion);
    }

    // Operaciones del Director (RF17 & RF18)
    public void registrarDocente(String id, String username, String password, String nombre, String especialidad) throws ReglaNegocioException {
        if (repo.getUsuariosById().containsKey(id) || repo.getUsuariosByUsername().containsKey(username)) {
            throw new ReglaNegocioException("Ya existe un usuario con el mismo ID o nombre de usuario.");
        }
        repo.agregarUsuario(new Docente(id, username, password, nombre, especialidad));
    }

    public void registrarAlumno(String id, String username, String password, String nombre, String grado, String seccion) throws ReglaNegocioException {
        if (repo.getUsuariosById().containsKey(id) || repo.getUsuariosByUsername().containsKey(username)) {
            throw new ReglaNegocioException("Ya existe un usuario con el mismo ID o nombre de usuario.");
        }
        repo.agregarUsuario(new Alumno(id, username, password, nombre, grado, seccion));
    }

    public void eliminarUsuario(String id) throws RegistroNoEncontradoException {
        if (!repo.getUsuariosById().containsKey(id)) {
            throw new RegistroNoEncontradoException("No se encontró el usuario con ID: " + id);
        }
        repo.eliminarUsuario(id);
    }

    public List<Docente> obtenerTodosLosDocentes() {
        return repo.getUsuarios().stream()
            .filter(u -> u instanceof Docente)
            .map(u -> (Docente) u)
            .collect(Collectors.toList());
    }

    public List<Alumno> obtenerTodosLosAlumnos() {
        return repo.getUsuarios().stream()
            .filter(u -> u instanceof Alumno)
            .map(u -> (Alumno) u)
            .collect(Collectors.toList());
    }

    public Alumno obtenerAlumnoPorId(String id) throws RegistroNoEncontradoException {
        Usuario u = repo.getUsuariosById().get(id);
        if (u == null || !(u instanceof Alumno)) {
            throw new RegistroNoEncontradoException("Estudiante no encontrado con ID: " + id);
        }
        return (Alumno) u;
    }

    // RF04: Gestión de Notas
    public void registrarNota(String idAlumno, String idCurso, double valorNumerico, String periodo) throws ReglaNegocioException, RegistroNoEncontradoException {
        // Verificar si existe el alumno
        obtenerAlumnoPorId(idAlumno);
        // Verificar si existe el curso
        if (!repo.getCursosById().containsKey(idCurso)) {
            throw new RegistroNoEncontradoException("Curso no encontrado con ID: " + idCurso);
        }

        // Regla de conversión de nota (excepción si está fuera de rango)
        String valorLetra = determinarLetraNota(valorNumerico);

        // Buscar si ya existe la nota para este periodo y curso, si existe se modifica
        Optional<Nota> notaExistente = repo.getNotas().stream()
            .filter(n -> n.getIdAlumno().equals(idAlumno) && n.getIdCurso().equals(idCurso) && n.getPeriodo().equals(periodo))
            .findFirst();

        if (notaExistente.isPresent()) {
            notaExistente.get().setValorNumerico(valorNumerico);
            notaExistente.get().setValorLetra(valorLetra);
        } else {
            String newId = "NOT" + (repo.getNotas().size() + 1);
            repo.getNotas().add(new Nota(newId, idAlumno, idCurso, valorNumerico, valorLetra, periodo));
        }
    }

    public List<Nota> obtenerNotasPorAlumno(String idAlumno) {
        return repo.getNotas().stream()
            .filter(n -> n.getIdAlumno().equals(idAlumno))
            .collect(Collectors.toList());
    }

    // RF05: Gestión de Asistencia
    public void registrarAsistencia(String idUsuario, String fecha, String estado, String observacion, boolean esDocente) throws ReglaNegocioException {
        if (!estado.equals("ASISTENCIA") && !estado.equals("TARDANZA") && !estado.equals("FALTA")) {
            throw new ReglaNegocioException("Estado de asistencia inválido. Use ASISTENCIA, TARDANZA o FALTA.");
        }

        String newId = "ASIS" + (repo.getAsistencias().size() + 1);
        repo.getAsistencias().add(new Asistencia(newId, idUsuario, fecha, estado, observacion, esDocente));
    }

    public List<Asistencia> obtenerAsistenciasPorUsuario(String idUsuario) {
        return repo.getAsistencias().stream()
            .filter(a -> a.getIdUsuario().equals(idUsuario))
            .collect(Collectors.toList());
    }

    // RF06: Gestión de Tareas
    public void crearTarea(String idCurso, String titulo, String descripcion, String fechaEntrega, String archivoAdjunto) throws RegistroNoEncontradoException {
        if (!repo.getCursosById().containsKey(idCurso)) {
            throw new RegistroNoEncontradoException("Curso no encontrado con ID: " + idCurso);
        }
        String newId = "TAR" + (repo.getTareas().size() + 1);
        repo.getTareas().add(new Tarea(newId, idCurso, titulo, descripcion, fechaEntrega, archivoAdjunto));
    }

    // RF11: Envío de Tareas
    public void enviarTarea(String idTarea, String idAlumno, String archivoEnviado, String fechaEnvio) throws RegistroNoEncontradoException {
        Optional<Tarea> tareaOpt = repo.getTareas().stream()
            .filter(t -> t.getId().equals(idTarea))
            .findFirst();

        if (tareaOpt.isEmpty()) {
            throw new RegistroNoEncontradoException("Tarea no encontrada con ID: " + idTarea);
        }

        Tarea tarea = tareaOpt.get();
        // Permite subir o reemplazar antes de calificar
        Tarea.Entrega entrega = new Tarea.Entrega(idAlumno, archivoEnviado, fechaEnvio, "", "", "ENVIADO");
        tarea.agregarEntrega(entrega);
    }

    // RF06: Calificar tarea
    public void calificarTarea(String idTarea, String idAlumno, String calificacion, String comentarios) throws ReglaNegocioException, RegistroNoEncontradoException {
        if (!calificacion.equals("AD") && !calificacion.equals("A") && !calificacion.equals("B") && !calificacion.equals("C")) {
            throw new ReglaNegocioException("La calificación de la tarea debe estar en la escala AD, A, B, o C.");
        }

        Optional<Tarea> tareaOpt = repo.getTareas().stream()
            .filter(t -> t.getId().equals(idTarea))
            .findFirst();

        if (tareaOpt.isEmpty()) {
            throw new RegistroNoEncontradoException("Tarea no encontrada con ID: " + idTarea);
        }

        Tarea.Entrega entrega = tareaOpt.get().getEntregas().get(idAlumno);
        if (entrega == null) {
            throw new RegistroNoEncontradoException("No se encontró una entrega del alumno con ID: " + idAlumno);
        }

        entrega.setCalificacion(calificacion);
        entrega.setComentarios(comentarios);
        entrega.setEstado("CALIFICADO");
    }

    // RF08: Gestión de Anuncios
    public void publicarAnuncio(String titulo, String contenido, String fecha, String autor) {
        String newId = "AN" + (repo.getAnuncios().size() + 1);
        repo.getAnuncios().add(new Anuncio(newId, titulo, contenido, fecha, autor));
    }

    // RF20: Gestión de Pagos
    public void registrarPago(String idAlumno, double monto, String concepto, String fechaVencimiento) throws RegistroNoEncontradoException {
        obtenerAlumnoPorId(idAlumno);
        String newId = "PAG" + (repo.getPagos().size() + 1);
        repo.getPagos().add(new Pago(newId, idAlumno, monto, concepto, fechaVencimiento, false, ""));
    }

    public void realizarPago(String idPago, String fechaPago) throws RegistroNoEncontradoException {
        Optional<Pago> pagoOpt = repo.getPagos().stream()
            .filter(p -> p.getId().equals(idPago))
            .findFirst();

        if (pagoOpt.isEmpty()) {
            throw new RegistroNoEncontradoException("Pago no encontrado con ID: " + idPago);
        }

        Pago pago = pagoOpt.get();
        pago.setPagado(true);
        pago.setFechaPago(fechaPago);
    }

    // Consultas y Filtros usando Streams para Vistas de Curso
    public List<Curso> obtenerCursosPorDocente(String idDocente) {
        return repo.getCursos().stream()
            .filter(c -> c.getIdDocente().equals(idDocente))
            .collect(Collectors.toList());
    }

    public List<Curso> obtenerCursosPorAlumno(String idAlumno) throws RegistroNoEncontradoException {
        Alumno alumno = obtenerAlumnoPorId(idAlumno);
        // Filtrar cursos que coincidan con el grado y seccion del alumno
        return repo.getCursos().stream()
            .filter(c -> c.getGrado().equals(alumno.getGrado()) && c.getSeccion().equals(alumno.getSeccion()))
            .collect(Collectors.toList());
    }

    public List<Alumno> obtenerAlumnosPorCurso(String idCurso) throws RegistroNoEncontradoException {
        Curso curso = repo.getCursosById().get(idCurso);
        if (curso == null) {
            throw new RegistroNoEncontradoException("Curso no encontrado con ID: " + idCurso);
        }
        return repo.getUsuarios().stream()
            .filter(u -> u instanceof Alumno)
            .map(u -> (Alumno) u)
            .filter(a -> a.getGrado().equals(curso.getGrado()) && a.getSeccion().equals(curso.getSeccion()))
            .collect(Collectors.toList());
    }

    public List<Horario> obtenerHorariosPorCurso(String idCurso) {
        return repo.getHorarios().stream()
            .filter(h -> h.getIdCurso().equals(idCurso))
            .collect(Collectors.toList());
    }

    // RF21: Reportes - Programación Funcional utilizando reduce y map
    public double calcularTotalRecaudado() {
        // Uso de map y reduce
        return repo.getPagos().stream()
            .filter(Pago::isPagado)
            .map(Pago::getMonto)
            .reduce(0.0, Double::sum);
    }

    public double calcularTasaAsistenciaAlumnos() {
        List<Asistencia> asistenciasAlumnos = repo.getAsistencias().stream()
            .filter(a -> !a.isEsDocente())
            .collect(Collectors.toList());

        if (asistenciasAlumnos.isEmpty()) {
            return 100.0;
        }

        long asistieron = asistenciasAlumnos.stream()
            .filter(a -> a.getEstado().equals("ASISTENCIA") || a.getEstado().equals("TARDANZA"))
            .count();

        return (double) asistieron / asistenciasAlumnos.size() * 100.0;
    }

    public List<Pago> obtenerPagosPendientes() {
        return repo.getPagos().stream()
            .filter(p -> !p.isPagado())
            .collect(Collectors.toList());
    }
}
