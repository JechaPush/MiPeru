package Controller;

import Exceptions.*;
import Models.*;
import Service.EscuelaService;
import java.util.List;

public class EscuelaController {
    private EscuelaService service;

    public EscuelaController() {
        this.service = new EscuelaService();
    }

    public EscuelaService getService() {
        return service;
    }

    // Iniciar Sesión (RF01)
    public Usuario login(String username, String password) throws LoginException {
        return service.login(username, password);
    }

    // Obtener cursos por docente
    public List<Curso> obtenerCursosPorDocente(String idDocente) {
        return service.obtenerCursosPorDocente(idDocente);
    }

    // Obtener cursos por alumno
    public List<Curso> obtenerCursosPorAlumno(String idAlumno) throws RegistroNoEncontradoException {
        return service.obtenerCursosPorAlumno(idAlumno);
    }

    // Registrar notas (RF04)
    public void registrarNota(String idAlumno, String idCurso, double valorNumerico, String periodo) throws ReglaNegocioException, RegistroNoEncontradoException {
        service.registrarNota(idAlumno, idCurso, valorNumerico, periodo);
    }

    // Registrar asistencia (RF05)
    public void registrarAsistencia(String idUsuario, String fecha, String estado, String observacion, boolean esDocente) throws ReglaNegocioException {
        service.registrarAsistencia(idUsuario, fecha, estado, observacion, esDocente);
    }

    // Crear tareas (RF06)
    public void crearTarea(String idCurso, String titulo, String descripcion, String fechaEntrega, String archivoAdjunto) throws RegistroNoEncontradoException {
        service.crearTarea(idCurso, titulo, descripcion, fechaEntrega, archivoAdjunto);
    }

    // Calificar tarea (RF06)
    public void calificarTarea(String idTarea, String idAlumno, String calificacion, String comentarios) throws ReglaNegocioException, RegistroNoEncontradoException {
        service.calificarTarea(idTarea, idAlumno, calificacion, comentarios);
    }

    // Enviar tarea (RF11)
    public void enviarTarea(String idTarea, String idAlumno, String archivoEnviado, String fechaEnvio) throws RegistroNoEncontradoException {
        service.enviarTarea(idTarea, idAlumno, archivoEnviado, fechaEnvio);
    }

    // Publicar anuncio (RF08)
    public void publicarAnuncio(String titulo, String contenido, String fecha, String autor) {
        service.publicarAnuncio(titulo, contenido, fecha, autor);
    }

    // Registrar Docente (RF17)
    public void registrarDocente(String id, String username, String password, String nombre, String especialidad) throws ReglaNegocioException {
        service.registrarDocente(id, username, password, nombre, especialidad);
    }

    // Registrar Alumno (RF18)
    public void registrarAlumno(String id, String username, String password, String nombre, String grado, String seccion) throws ReglaNegocioException {
        service.registrarAlumno(id, username, password, nombre, grado, seccion);
    }

    // Eliminar Usuario
    public void eliminarUsuario(String id) throws RegistroNoEncontradoException {
        service.eliminarUsuario(id);
    }

    // Matricular Alumno (RF19)
    public void registrarMatricula(String idAlumno, String grado, String seccion) throws ReglaNegocioException, RegistroNoEncontradoException {
        service.registrarMatricula(idAlumno, grado, seccion);
    }

    // Registrar Pago (RF20)
    public void registrarPago(String idAlumno, double monto, String concepto, String fechaVencimiento) throws RegistroNoEncontradoException {
        service.registrarPago(idAlumno, monto, concepto, fechaVencimiento);
    }

    // Realizar Pago
    public void realizarPago(String idPago, String fechaPago) throws RegistroNoEncontradoException {
        service.realizarPago(idPago, fechaPago);
    }
}
