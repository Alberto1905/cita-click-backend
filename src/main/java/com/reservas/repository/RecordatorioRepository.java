package com.reservas.repository;

import com.reservas.entity.Recordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordatorioRepository extends JpaRepository<Recordatorio, String> {
    List<Recordatorio> findByCitaIdAndEnviado(String citaId, boolean enviado);

    List<Recordatorio> findByEnviado(boolean enviado);

    List<Recordatorio> findByEnviadoFalse();
}