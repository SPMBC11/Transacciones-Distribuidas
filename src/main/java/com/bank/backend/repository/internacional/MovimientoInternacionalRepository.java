package com.bank.backend.repository.internacional;

import com.bank.backend.model.internacional.MovimientoInternacional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para movimientos del Banco Internacional (MySQL).
 */
@Repository
public interface MovimientoInternacionalRepository extends JpaRepository<MovimientoInternacional, Long> {

    List<MovimientoInternacional> findByCuentaIdOrderByFechaDesc(Long cuentaId);

    Optional<MovimientoInternacional> findByReferenciaTransferencia(String referencia);
}
