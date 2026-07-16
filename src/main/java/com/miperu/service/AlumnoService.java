package com.miperu.service;

import com.miperu.exception.RegistroNoEncontradoException;
import com.miperu.model.*;
import com.miperu.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlumnoService {
    private final UsuarioRepository usuarioRepo;
    private final CursoRepository cursoRepo;
    private final NotaRepository notaRepo;
    private final TareaRepository tareaRepo;
    private final PagoRepository pagoRepo;

    @Autowired
    public AlumnoService(UsuarioRepository usuarioRepo, CursoRepository cursoRepo, NotaRepository notaRepo,
                         TareaRepository tareaRepo, PagoRepository pagoRepo) {
        this.usuarioRepo = usuarioRepo;
        this.cursoRepo = cursoRepo;
        this.notaRepo = notaRepo;
        this.tareaRepo = tareaRepo;
        this.pagoRepo = pagoRepo;
    }

    public Alumno obtenerAlumnoPorId(String id) throws RegistroNoEncontradoException {
        Usuario u = usuarioRepo.findById(id);
        if (u == null || !(u instanceof Alumno)) {
            throw new RegistroNoEncontradoException("Estudiante no encontrado con ID: " + id);
        }
        return (Alumno) u;
    }

    public List<Curso> obtenerCursosPorAlumno(String idAlumno) throws RegistroNoEncontradoException {
        Alumno alumno = obtenerAlumnoPorId(idAlumno);
        return cursoRepo.findAll().stream()
            .filter(c -> c.getGrado().equals(alumno.getGrado()) && c.getSeccion().equals(alumno.getSeccion()))
            .collect(Collectors.toList());
    }

    public List<Nota> obtenerNotasPorAlumno(String idAlumno) {
        return notaRepo.findAll().stream()
            .filter(n -> n.getIdAlumno().equals(idAlumno))
            .collect(Collectors.toList());
    }

    public void enviarTarea(String idTarea, String idAlumno, String archivoEnviado, String fechaEnvio) throws RegistroNoEncontradoException {
        Tarea tarea = tareaRepo.findById(idTarea);
        if (tarea == null) {
            throw new RegistroNoEncontradoException("Tarea no encontrada con ID: " + idTarea);
        }

        Tarea.Entrega entrega = new Tarea.Entrega(idAlumno, archivoEnviado, fechaEnvio, "", "", "ENVIADO");
        tarea.agregarEntrega(entrega);
        tareaRepo.save(tarea);
    }

    public String obtenerDesempenoGeneral(String idAlumno) {
        List<Nota> notasAlumno = obtenerNotasPorAlumno(idAlumno);

        if (notasAlumno.isEmpty()) {
            return "Sin notas registradas";
        }

        long countAD = notasAlumno.stream().filter(n -> n.getValorLetra().equals("AD")).count();
        long countC = notasAlumno.stream().filter(n -> n.getValorLetra().equals("C")).count();

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

    public List<Pago> obtenerPagosPorAlumno(String idAlumno) {
        return pagoRepo.findAll().stream()
            .filter(p -> p.getIdAlumno().equals(idAlumno))
            .collect(Collectors.toList());
    }
}
