package com.miperu.repository;

import com.miperu.model.Nota;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class NotaRepository {
    private final List<Nota> notas = new ArrayList<>();

    public void save(Nota n) {
        notas.removeIf(existing -> existing.getId().equals(n.getId()));
        notas.add(n);
    }

    public List<Nota> findAll() {
        return notas;
    }

    public void deleteById(String id) {
        notas.removeIf(n -> n.getId().equals(id));
    }
}
