package com.miperu.repository;

import com.miperu.model.Pago;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PagoRepository {
    private final List<Pago> pagos = new ArrayList<>();

    public void save(Pago p) {
        pagos.removeIf(existing -> existing.getId().equals(p.getId()));
        pagos.add(p);
    }

    public List<Pago> findAll() {
        return pagos;
    }

    public Pago findById(String id) {
        return pagos.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
