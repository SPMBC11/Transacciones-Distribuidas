package com.bank.backend.services;

import com.bank.backend.model.CuentaNacional;
import com.bank.backend.model.MovimientoNacional;
import com.bank.backend.repository.nacional.CuentaNacionalRepository;
import com.bank.backend.repository.nacional.MovimientoNacionalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio del Banco Nacional (PostgreSQL).
 *
 * IMPORTANTE: Cada método que modifica datos usa:
 *   @Transactional(transactionManager = "nacionalTransactionManager")
 *
 * Esto garantiza que las operaciones van a PostgreSQL y no a MySQL.
 * Sin especificar el transactionManager, Spring usaría el primario (@Primary)
 * que en este proyecto es también el nacional, pero es buena práctica ser explícito.
 */
@Service
public class BancoNacionalService {

    private static final Logger log = LoggerFactory.getLogger(BancoNacionalService.class);

    private final CuentaNacionalRepository cuentaRepository;
    private final MovimientoNacionalRepository movimientoRepository;

    public BancoNacionalService(CuentaNacionalRepository cuentaRepository,
                                 MovimientoNacionalRepository movimientoRepository) {
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTAS (solo lectura)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Consulta el saldo actual de una cuenta.
     * readOnly = true: optimización, PostgreSQL no adquiere locks de escritura.
     */
    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public BigDecimal consultarSaldo(String numeroCuenta) {
        log.info("[BancoNacional] Consultando saldo de cuenta: {}", numeroCuenta);

        CuentaNacional cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + numeroCuenta));

        log.info("[BancoNacional] Saldo de {}: {}", numeroCuenta, cuenta.getSaldo());
        return cuenta.getSaldo();
    }

    /**
     * Devuelve la información completa de una cuenta.
     */
    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public CuentaNacional consultarCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + numeroCuenta));
    }

    /**
     * Devuelve todos los movimientos de una cuenta, ordenados del más reciente al más antiguo.
     */
    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public List<MovimientoNacional> consultarMovimientos(String numeroCuenta) {
        CuentaNacional cuenta = consultarCuenta(numeroCuenta);
        return movimientoRepository.findByCuentaIdOrderByFechaDesc(cuenta.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OPERACIONES DE ESCRITURA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registra un DÉBITO en la cuenta: descuenta el monto y guarda el movimiento.
     *
     * Usa findByNumeroCuentaWithLock → SELECT FOR UPDATE en PostgreSQL.
     * Esto bloquea la fila mientras dura la transacción, evitando race conditions
     * si dos transferencias intentan debitar la misma cuenta al mismo tiempo.
     *
     * @param numeroCuenta  Número de la cuenta origen (ej: "BN-001")
     * @param monto         Monto a debitar
     * @param referencia    UUID de la transferencia distribuida (para trazabilidad)
     * @return La cuenta con el saldo ya actualizado
     * @throws RuntimeException si la cuenta no existe, está inactiva o saldo insuficiente
     */
    @Transactional(transactionManager = "nacionalTransactionManager")
    public CuentaNacional debitar(String numeroCuenta, BigDecimal monto, String referencia) {
        log.info("[BancoNacional] Iniciando débito de {} en cuenta {} | ref: {}", monto, numeroCuenta, referencia);

        // 1. Obtener cuenta con lock pesimista
        CuentaNacional cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta origen no encontrada: " + numeroCuenta));

        // 2. Validar que la cuenta esté activa
        if (!cuenta.getActiva()) {
            throw new RuntimeException("La cuenta " + numeroCuenta + " está inactiva");
        }

        // 3. Validar saldo suficiente
        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new RuntimeException("Saldo insuficiente en cuenta " + numeroCuenta
                    + ". Saldo actual: " + cuenta.getSaldo() + ", monto requerido: " + monto);
        }

        // 4. Actualizar saldo
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.subtract(monto);
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

        // 5. Registrar movimiento
        MovimientoNacional movimiento = new MovimientoNacional();
        movimiento.setCuentaId(cuenta.getId());
        movimiento.setTipo("DEBITO");
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setDescripcion("Transferencia interbancaria - débito");
        movimiento.setReferenciaTransferencia(referencia);
        movimientoRepository.save(movimiento);

        log.info("[BancoNacional] Débito exitoso. Saldo anterior: {} → Saldo nuevo: {}", saldoAnterior, saldoNuevo);
        return cuenta;
    }

    /**
     * COMPENSACIÓN SAGA: revierte un débito previamente realizado.

     * @param numeroCuenta
     * @param monto
     * @param referencia
     */
    @Transactional(transactionManager = "nacionalTransactionManager")
    public void revertirDebito(String numeroCuenta, BigDecimal monto, String referencia) {
        log.warn("[BancoNacional] COMPENSANDO - Revirtiendo débito de {} en cuenta {} | ref: {}", monto, numeroCuenta, referencia);

        CuentaNacional cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada para compensación: " + numeroCuenta));

        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.add(monto);
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

        // Registrar movimiento de compensación
        MovimientoNacional compensacion = new MovimientoNacional();
        compensacion.setCuentaId(cuenta.getId());
        compensacion.setTipo("CREDITO");
        compensacion.setMonto(monto);
        compensacion.setSaldoAnterior(saldoAnterior);
        compensacion.setSaldoNuevo(saldoNuevo);
        compensacion.setDescripcion("COMPENSACION SAGA - débito revertido");
        compensacion.setReferenciaTransferencia(referencia + "-REVERTIDO");
        movimientoRepository.save(compensacion);

        log.warn("[BancoNacional] Compensación exitosa. Saldo restaurado: {} → {}", saldoAnterior, saldoNuevo);
    }
}
