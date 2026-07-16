package com.miperu.service;

import com.miperu.exception.ReglaNegocioException;
import com.miperu.exception.RegistroNoEncontradoException;
import com.miperu.model.*;
import com.miperu.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DirectorService {
    private final UsuarioRepository usuarioRepo;
    private final PagoRepository pagoRepo;
    private final AsistenciaRepository asistenciaRepo;

    @Autowired
    public DirectorService(UsuarioRepository usuarioRepo, PagoRepository pagoRepo, AsistenciaRepository asistenciaRepo) {
        this.usuarioRepo = usuarioRepo;
        this.pagoRepo = pagoRepo;
        this.asistenciaRepo = asistenciaRepo;
    }

    public void registrarDocente(String id, String username, String password, String nombre, String especialidad) throws ReglaNegocioException {
        if (usuarioRepo.findById(id) != null || usuarioRepo.findByUsername(username) != null) {
            throw new ReglaNegocioException("Ya existe un usuario con el mismo ID o nombre de usuario.");
        }
        usuarioRepo.save(new Docente(id, username, password, nombre, especialidad));
    }

    public void registrarAlumno(String id, String username, String password, String nombre, String grado, String seccion) throws ReglaNegocioException {
        if (usuarioRepo.findById(id) != null || usuarioRepo.findByUsername(username) != null) {
            throw new ReglaNegocioException("Ya existe un usuario con el mismo ID o nombre de usuario.");
        }
        usuarioRepo.save(new Alumno(id, username, password, nombre, grado, seccion));
    }

    public void eliminarUsuario(String id) throws RegistroNoEncontradoException {
        if (usuarioRepo.findById(id) == null) {
            throw new RegistroNoEncontradoException("No se encontró el usuario con ID: " + id);
        }
        usuarioRepo.deleteById(id);
    }

    public void registrarMatricula(String idAlumno, String grado, String seccion) throws ReglaNegocioException, RegistroNoEncontradoException {
        Usuario u = usuarioRepo.findById(idAlumno);
        if (u == null || !(u instanceof Alumno)) {
            throw new RegistroNoEncontradoException("Estudiante no encontrado con ID: " + idAlumno);
        }
        Alumno alumno = (Alumno) u;

        boolean tieneDeudas = pagoRepo.findAll().stream()
            .filter(p -> p.getIdAlumno().equals(idAlumno))
            .filter(p -> !p.isPagado())
            .anyMatch(p -> p.getConcepto().contains("Junio") || p.getConcepto().contains("Matrícula"));

        if (tieneDeudas) {
            throw new ReglaNegocioException("No se puede registrar la matrícula del alumno '" + alumno.getNombre() + "' porque registra pagos pendientes del mes anterior.");
        }

        alumno.setGrado(grado);
        alumno.setSeccion(seccion);
        usuarioRepo.save(alumno);
    }

    public void registrarPago(String idAlumno, double monto, String concepto, String fechaVencimiento) throws RegistroNoEncontradoException {
        Usuario u = usuarioRepo.findById(idAlumno);
        if (u == null || !(u instanceof Alumno)) {
            throw new RegistroNoEncontradoException("Estudiante no encontrado con ID: " + idAlumno);
        }
        String newId = "PAG" + (pagoRepo.findAll().size() + 1);
        pagoRepo.save(new Pago(newId, idAlumno, monto, concepto, fechaVencimiento, false, ""));
    }

    public void realizarPago(String idPago, String fechaPago) throws RegistroNoEncontradoException {
        Pago pago = pagoRepo.findById(idPago);
        if (pago == null) {
            throw new RegistroNoEncontradoException("Pago no encontrado con ID: " + idPago);
        }

        pago.setPagado(true);
        pago.setFechaPago(fechaPago);
        pagoRepo.save(pago);
    }

    public double calcularTotalRecaudado() {
        return pagoRepo.findAll().stream()
            .filter(Pago::isPagado)
            .map(Pago::getMonto)
            .reduce(0.0, Double::sum);
    }

    public double calcularTasaAsistenciaAlumnos() {
        List<Asistencia> asistenciasAlumnos = asistenciaRepo.findAll().stream()
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
        return pagoRepo.findAll().stream()
            .filter(p -> !p.isPagado())
            .collect(Collectors.toList());
    }

    public List<Docente> obtenerTodosLosDocentes() {
        return usuarioRepo.findAll().stream()
            .filter(u -> u instanceof Docente)
            .map(u -> (Docente) u)
            .collect(Collectors.toList());
    }

    public List<Alumno> obtenerTodosLosAlumnos() {
        return usuarioRepo.findAll().stream()
            .filter(u -> u instanceof Alumno)
            .map(u -> (Alumno) u)
            .collect(Collectors.toList());
    }
}
