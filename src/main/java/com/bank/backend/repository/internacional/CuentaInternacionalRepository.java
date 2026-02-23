package com.bank.backend.repository.internacional;

import com.bank.backend.model.internacional.CuentaInternacional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para cuentas del Banco Internacional (MySQL).
 */
@Repository
public interface CuentaInternacionalRepository extends JpaRepository<CuentaInternacional, Long> {

    Optional<CuentaInternacional> findByNumeroCuenta(String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CuentaInternacional c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<CuentaInternacional> findByNumeroCuentaWithLock(@Param("numeroCuenta") String numeroCuenta);
}
