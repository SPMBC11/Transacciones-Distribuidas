package com.bank.backend.repository.nacional;

import com.bank.backend.model.CuentaNacional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para cuentas del Banco Nacional (PostgreSQL).
 */
@Repository
public interface CuentaNacionalRepository extends JpaRepository<CuentaNacional, Long> {

    Optional<CuentaNacional> findByNumeroCuenta(String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CuentaNacional c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<CuentaNacional> findByNumeroCuentaWithLock(@Param("numeroCuenta") String numeroCuenta);
}
