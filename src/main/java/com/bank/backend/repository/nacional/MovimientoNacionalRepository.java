package com.bank.backend.repository.nacional;

import com.bank.backend.model.MovimientoNacional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para movimientos del Banco Nacional (PostgreSQL).
 */
@Repository
public interface MovimientoNacionalRepository extends JpaRepository<MovimientoNacional, Long> {

    List<MovimientoNacional> findByCuentaIdOrderByFechaDesc(Long cuentaId);

    Optional<MovimientoNacional> findByReferenciaTransferencia(String referencia);
}
