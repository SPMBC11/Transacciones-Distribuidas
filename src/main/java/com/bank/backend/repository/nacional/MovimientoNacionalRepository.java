package com.bank.backend.repository.nacional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bank.backend.model.nacional.MovimientoNacional;

import java.util.List;
import java.util.Optional;


@Repository
public interface MovimientoNacionalRepository extends JpaRepository<MovimientoNacional, Long> {

    List<MovimientoNacional> findByCuentaIdOrderByFechaDesc(Long cuentaId);

    Optional<MovimientoNacional> findByReferenciaTransferencia(String referencia);
}
