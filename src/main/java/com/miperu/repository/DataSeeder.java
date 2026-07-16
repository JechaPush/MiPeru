package com.miperu.repository;

import com.miperu.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UsuarioRepository usuarioRepo;
    private final CursoRepository cursoRepo;
    private final NotaRepository notaRepo;
    private final AsistenciaRepository asistenciaRepo;
    private final TareaRepository tareaRepo;
    private final AnuncioRepository anuncioRepo;
    private final PagoRepository pagoRepo;
    private final HorarioRepository horarioRepo;

    @Autowired
    public DataSeeder(UsuarioRepository usuarioRepo, CursoRepository cursoRepo, NotaRepository notaRepo,
                      AsistenciaRepository asistenciaRepo, TareaRepository tareaRepo, AnuncioRepository anuncioRepo,
                      PagoRepository pagoRepo, HorarioRepository horarioRepo) {
        this.usuarioRepo = usuarioRepo;
        this.cursoRepo = cursoRepo;
        this.notaRepo = notaRepo;
        this.asistenciaRepo = asistenciaRepo;
        this.tareaRepo = tareaRepo;
        this.anuncioRepo = anuncioRepo;
        this.pagoRepo = pagoRepo;
        this.horarioRepo = horarioRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        Director dir = new Director("DIR01", "director", "director123", "Juan Pérez");
        usuarioRepo.save(dir);

        Docente doc1 = new Docente("DOC01", "profesor1", "profesor123", "Carlos Mendoza", "Matemáticas");
        Docente doc2 = new Docente("DOC02", "profesor2", "profesor123", "María Gómez", "Lenguaje");
        usuarioRepo.save(doc1);
        usuarioRepo.save(doc2);

        Alumno al1 = new Alumno("AL01", "alumno1", "alumno123", "Luis Torres", "5to", "A");
        Alumno al2 = new Alumno("AL02", "alumno2", "alumno123", "Ana Quispe", "5to", "A");
        Alumno al3 = new Alumno("AL03", "alumno3", "alumno123", "Diego Flores", "4to", "B");
        usuarioRepo.save(al1);
        usuarioRepo.save(al2);
        usuarioRepo.save(al3);

        Curso cur1 = new Curso("CUR01", "Álgebra y Aritmética", "5to", "A", "DOC01");
        Curso cur2 = new Curso("CUR02", "Geometría", "5to", "A", "DOC01");
        Curso cur3 = new Curso("CUR03", "Comprensión Lectora", "5to", "A", "DOC02");
        Curso cur4 = new Curso("CUR04", "Redacción", "4to", "B", "DOC02");
        cursoRepo.save(cur1);
        cursoRepo.save(cur2);
        cursoRepo.save(cur3);
        cursoRepo.save(cur4);

        horarioRepo.save(new Horario("HOR01", "CUR01", "Lunes", "08:00", "09:30", "Aula 101"));
        horarioRepo.save(new Horario("HOR02", "CUR01", "Miércoles", "08:00", "09:30", "Aula 101"));
        horarioRepo.save(new Horario("HOR03", "CUR02", "Martes", "09:45", "11:15", "Aula 101"));
        horarioRepo.save(new Horario("HOR04", "CUR03", "Lunes", "09:45", "11:15", "Aula 102"));
        horarioRepo.save(new Horario("HOR05", "CUR04", "Jueves", "08:00", "09:30", "Aula 103"));

        notaRepo.save(new Nota("NOT01", "AL01", "CUR01", 18.0, "A", "Bimestre 1"));
        notaRepo.save(new Nota("NOT02", "AL01", "CUR01", 20.0, "AD", "Bimestre 2"));
        notaRepo.save(new Nota("NOT03", "AL02", "CUR01", 14.0, "B", "Bimestre 1"));
        notaRepo.save(new Nota("NOT04", "AL02", "CUR01", 16.0, "A", "Bimestre 2"));
        notaRepo.save(new Nota("NOT05", "AL03", "CUR04", 10.0, "C", "Bimestre 1"));

        asistenciaRepo.save(new Asistencia("ASIS01", "AL01", "2026-07-01", "ASISTENCIA", "Llegó temprano", false));
        asistenciaRepo.save(new Asistencia("ASIS02", "AL02", "2026-07-01", "TARDANZA", "10 minutos de retraso", false));
        asistenciaRepo.save(new Asistencia("ASIS03", "AL03", "2026-07-01", "FALTA", "Inasistencia justificada", false));
        asistenciaRepo.save(new Asistencia("ASIS04", "DOC01", "2026-07-01", "ASISTENCIA", "Clase dictada con éxito", true));
        asistenciaRepo.save(new Asistencia("ASIS05", "DOC02", "2026-07-01", "ASISTENCIA", "Clase dictada con éxito", true));

        Tarea tar1 = new Tarea("TAR01", "CUR01", "Ecuaciones Cuadráticas", "Resolver los ejercicios de la página 45 del libro de Álgebra.", "2026-07-10", "guia_ecuaciones.pdf");
        Tarea.Entrega ent1 = new Tarea.Entrega("AL01", "solucion_ecuaciones_luis.pdf", "2026-07-02", "AD", "Excelente resolución paso a paso.", "CALIFICADO");
        Tarea.Entrega ent2 = new Tarea.Entrega("AL02", "tarea_ana.pdf", "2026-07-03", "", "", "ENVIADO");
        tar1.agregarEntrega(ent1);
        tar1.agregarEntrega(ent2);
        tareaRepo.save(tar1);

        Tarea tar2 = new Tarea("TAR02", "CUR03", "Lectura Crítica", "Leer el ensayo adjunto y redactar una opinión de 2 páginas.", "2026-07-12", "ensayo_comprension.pdf");
        tareaRepo.save(tar2);

        anuncioRepo.save(new Anuncio("AN01", "Inicio del II Bimestre", "Les damos la bienvenida al inicio del segundo bimestre académico. Éxitos a todos.", "2026-06-15", "Director General"));
        anuncioRepo.save(new Anuncio("AN02", "Exámenes Bimestrales", "Los exámenes del II Bimestre se realizarán desde el 14 de julio. Estudiar los temarios.", "2026-07-01", "Docente Carlos Mendoza"));

        pagoRepo.save(new Pago("PAG01", "AL01", 350.00, "Mensualidad Junio", "2026-06-30", true, "2026-06-28"));
        pagoRepo.save(new Pago("PAG02", "AL01", 350.00, "Mensualidad Julio", "2026-07-31", false, ""));
        pagoRepo.save(new Pago("PAG03", "AL02", 350.00, "Mensualidad Junio", "2026-06-30", true, "2026-06-29"));
        pagoRepo.save(new Pago("PAG04", "AL02", 350.00, "Mensualidad Julio", "2026-07-31", false, ""));
        pagoRepo.save(new Pago("PAG05", "AL03", 350.00, "Mensualidad Junio", "2026-06-30", false, ""));
    }
}
