package com.miperu.repository;

import com.miperu.model.Asistencia;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AsistenciaRepository {
    private final List<Asistencia> asistencias = new ArrayList<>();

    public void save(Asistencia a) {
        asistencias.removeIf(existing -> existing.getId().equals(a.getId()));
        asistencias.add(a);
    }

    public List<Asistencia> findAll() {
        return asistencias;
    }
}
