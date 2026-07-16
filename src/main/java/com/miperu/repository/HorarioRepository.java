package com.miperu.repository;

import com.miperu.model.Horario;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class HorarioRepository {
    private final List<Horario> horarios = new ArrayList<>();

    public void save(Horario h) {
        horarios.removeIf(existing -> existing.getId().equals(h.getId()));
        horarios.add(h);
    }

    public List<Horario> findAll() {
        return horarios;
    }
}
