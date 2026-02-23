package com.bank.backend.services;

import com.bank.backend.model.internacional.CuentaInternacional;
import com.bank.backend.model.internacional.MovimientoInternacional;
import com.bank.backend.repository.internacional.CuentaInternacionalRepository;
import com.bank.backend.repository.internacional.MovimientoInternacionalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BancoInternacionalService {

    private static final Logger log = LoggerFactory.getLogger(BancoInternacionalService.class);

    private final CuentaInternacionalRepository cuentaRepository;
    private final MovimientoInternacionalRepository movimientoRepository;

    public BancoInternacionalService(CuentaInternacionalRepository cuentaRepository,
                                     MovimientoInternacionalRepository movimientoRepository) {
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // Consulta el saldo actual de una cuenta
    @Transactional(transactionManager = "internacionalTransactionManager", readOnly = true)
    public BigDecimal consultarSaldo(String numeroCuenta) {
        log.info("[BancoInternacional] Consultando saldo de cuenta: {}", numeroCuenta);

        CuentaInternacional cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + numeroCuenta));

        log.info("[BancoInternacional] Saldo de {}: {}", numeroCuenta, cuenta.getSaldo());
        return cuenta.getSaldo();
    }

    // Devuelve la información completa de una cuenta
    @Transactional(transactionManager = "internacionalTransactionManager", readOnly = true)
    public CuentaInternacional consultarCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + numeroCuenta));
    }

    // Devuelve todos los movimientos de una cuenta
    @Transactional(transactionManager = "internacionalTransactionManager", readOnly = true)
    public List<MovimientoInternacional> consultarMovimientos(String numeroCuenta) {
        CuentaInternacional cuenta = consultarCuenta(numeroCuenta);
        return movimientoRepository.findByCuentaIdOrderByFechaDesc(cuenta.getId());
    }

    // Registra un crédito en la cuenta 
    @Transactional(transactionManager = "internacionalTransactionManager")
    public CuentaInternacional acreditar(String numeroCuenta, BigDecimal monto, String referencia) {
        log.info("[BancoInternacional] Iniciando crédito de {} en cuenta {} | ref: {}", monto, numeroCuenta, referencia);

        // Obtener cuenta con lock pesimista
        CuentaInternacional cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta destino no encontrada: " + numeroCuenta));

        // Validar que la cuenta esté activa
        if (!cuenta.getActiva()) {
            throw new RuntimeException("La cuenta " + numeroCuenta + " está inactiva");
        }

        // Actualizar saldo
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.add(monto);
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

        // Registrar movimiento
        MovimientoInternacional movimiento = new MovimientoInternacional();
        movimiento.setCuentaId(cuenta.getId());
        movimiento.setTipo("CREDITO");
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setDescripcion("Transferencia interbancaria - crédito");
        movimiento.setReferenciaTransferencia(referencia);
        movimientoRepository.save(movimiento);

        log.info("[BancoInternacional] Crédito exitoso. Saldo anterior: {} → Saldo nuevo: {}", saldoAnterior, saldoNuevo);
        return cuenta;
    }

    // SAGA: revierte un crédito 
    @Transactional(transactionManager = "internacionalTransactionManager")
    public void revertirCredito(String numeroCuenta, BigDecimal monto, String referencia) {
        log.warn("[BancoInternacional] COMPENSANDO - Revirtiendo crédito de {} en cuenta {} | ref: {}", monto, numeroCuenta, referencia);

        CuentaInternacional cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada para compensación: " + numeroCuenta));

        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.subtract(monto);
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

        // Registrar movimiento de compensación
        MovimientoInternacional compensacion = new MovimientoInternacional();
        compensacion.setCuentaId(cuenta.getId());
        compensacion.setTipo("DEBITO");
        compensacion.setMonto(monto);
        compensacion.setSaldoAnterior(saldoAnterior);
        compensacion.setSaldoNuevo(saldoNuevo);
        compensacion.setDescripcion("COMPENSACION SAGA - crédito revertido");
        compensacion.setReferenciaTransferencia(referencia + "-REVERTIDO");
        movimientoRepository.save(compensacion);

        log.warn("[BancoInternacional] Compensación exitosa. Saldo revertido: {} → {}", saldoAnterior, saldoNuevo);
    }
}
