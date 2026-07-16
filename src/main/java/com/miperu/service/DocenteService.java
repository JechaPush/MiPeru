package com.miperu.service;

import com.miperu.exception.ReglaNegocioException;
import com.miperu.exception.RegistroNoEncontradoException;
import com.miperu.model.*;
import com.miperu.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocenteService {
    private final UsuarioRepository usuarioRepo;
    private final CursoRepository cursoRepo;
    private final NotaRepository notaRepo;
    private final AsistenciaRepository asistenciaRepo;
    private final TareaRepository tareaRepo;
    private final HorarioRepository horarioRepo;
    private final AnuncioRepository anuncioRepo;

    @Autowired
    public DocenteService(UsuarioRepository usuarioRepo, CursoRepository cursoRepo, NotaRepository notaRepo,
                          AsistenciaRepository asistenciaRepo, TareaRepository tareaRepo, HorarioRepository horarioRepo,
                          AnuncioRepository anuncioRepo) {
        this.usuarioRepo = usuarioRepo;
        this.cursoRepo = cursoRepo;
        this.notaRepo = notaRepo;
        this.asistenciaRepo = asistenciaRepo;
        this.tareaRepo = tareaRepo;
        this.horarioRepo = horarioRepo;
        this.anuncioRepo = anuncioRepo;
    }

    public List<Curso> obtenerCursosPorDocente(String idDocente) {
        return cursoRepo.findAll().stream()
            .filter(c -> c.getIdDocente().equals(idDocente))
            .collect(Collectors.toList());
    }

    public String determinarLetraNota(double valorNumerico) throws ReglaNegocioException {
        if (valorNumerico < 0.0 || valorNumerico > 20.0) {
            throw new ReglaNegocioException("La nota debe estar en el rango de 0 a 20. Valor recibido: " + valorNumerico);
        }

        if (valorNumerico >= 19.0) {
            return "AD";
        } else if (valorNumerico >= 15.0) {
            return "A";
        } else if (valorNumerico >= 11.0) {
            return "B";
        } else {
            return "C";
        }
    }

    public void registrarNota(String idAlumno, String idCurso, double valorNumerico, String periodo) throws ReglaNegocioException, RegistroNoEncontradoException {
        if (usuarioRepo.findById(idAlumno) == null) {
            throw new RegistroNoEncontradoException("Estudiante no encontrado con ID: " + idAlumno);
        }
        if (cursoRepo.findById(idCurso) == null) {
            throw new RegistroNoEncontradoException("Curso no encontrado con ID: " + idCurso);
        }

        String valorLetra = determinarLetraNota(valorNumerico);

        Optional<Nota> notaExistente = notaRepo.findAll().stream()
            .filter(n -> n.getIdAlumno().equals(idAlumno) && n.getIdCurso().equals(idCurso) && n.getPeriodo().equals(periodo))
            .findFirst();

        if (notaExistente.isPresent()) {
            notaExistente.get().setValorNumerico(valorNumerico);
            notaExistente.get().setValorLetra(valorLetra);
        } else {
            String newId = "NOT" + (notaRepo.findAll().size() + 1);
            notaRepo.save(new Nota(newId, idAlumno, idCurso, valorNumerico, valorLetra, periodo));
        }
    }

    public void registrarAsistencia(String idUsuario, String fecha, String estado, String observacion, boolean esDocente) throws ReglaNegocioException {
        if (!estado.equals("ASISTENCIA") && !estado.equals("TARDANZA") && !estado.equals("FALTA")) {
            throw new ReglaNegocioException("Estado de asistencia inválido. Use ASISTENCIA, TARDANZA o FALTA.");
        }

        String newId = "ASIS" + (asistenciaRepo.findAll().size() + 1);
        asistenciaRepo.save(new Asistencia(newId, idUsuario, fecha, estado, observacion, esDocente));
    }

    public void crearTarea(String idCurso, String titulo, String descripcion, String fechaEntrega, String archivoAdjunto) throws RegistroNoEncontradoException {
        if (cursoRepo.findById(idCurso) == null) {
            throw new RegistroNoEncontradoException("Curso no encontrado con ID: " + idCurso);
        }
        String newId = "TAR" + (tareaRepo.findAll().size() + 1);
        tareaRepo.save(new Tarea(newId, idCurso, titulo, descripcion, fechaEntrega, archivoAdjunto));
    }

    public void calificarTarea(String idTarea, String idAlumno, String calificacion, String comentarios) throws ReglaNegocioException, RegistroNoEncontradoException {
        if (!calificacion.equals("AD") && !calificacion.equals("A") && !calificacion.equals("B") && !calificacion.equals("C")) {
            throw new ReglaNegocioException("La calificación de la tarea debe estar en la escala AD, A, B, o C.");
        }

        Tarea tarea = tareaRepo.findById(idTarea);
        if (tarea == null) {
            throw new RegistroNoEncontradoException("Tarea no encontrada con ID: " + idTarea);
        }

        Tarea.Entrega entrega = tarea.getEntregas().get(idAlumno);
        if (entrega == null) {
            throw new RegistroNoEncontradoException("No se encontró una entrega del alumno con ID: " + idAlumno);
        }

        entrega.setCalificacion(calificacion);
        entrega.setComentarios(comentarios);
        entrega.setEstado("CALIFICADO");
        tareaRepo.save(tarea);
    }

    public void publicarAnuncio(String titulo, String contenido, String fecha, String autor) {
        String newId = "AN" + (anuncioRepo.findAll().size() + 1);
        anuncioRepo.save(new Anuncio(newId, titulo, contenido, fecha, autor));
    }

    public List<Horario> obtenerHorariosPorDocente(String idDocente) {
        return horarioRepo.findAll().stream()
            .filter(h -> {
                Curso c = cursoRepo.findById(h.getIdCurso());
                return c != null && c.getIdDocente().equals(idDocente);
            }).collect(Collectors.toList());
    }
}
