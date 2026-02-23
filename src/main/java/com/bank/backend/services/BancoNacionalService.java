package com.bank.backend.services;

import com.bank.backend.model.nacional.CuentaNacional;
import com.bank.backend.model.nacional.MovimientoNacional;
import com.bank.backend.repository.nacional.CuentaNacionalRepository;
import com.bank.backend.repository.nacional.MovimientoNacionalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public BigDecimal consultarSaldo(String numeroCuenta) {
        log.info("[BancoNacional] Consultando saldo de cuenta: {}", numeroCuenta);

        CuentaNacional cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + numeroCuenta));

        log.info("[BancoNacional] Saldo de {}: {}", numeroCuenta, cuenta.getSaldo());
        return cuenta.getSaldo();
    }

    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public CuentaNacional consultarCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada: " + numeroCuenta));
    }


    @Transactional(transactionManager = "nacionalTransactionManager", readOnly = true)
    public List<MovimientoNacional> consultarMovimientos(String numeroCuenta) {
        CuentaNacional cuenta = consultarCuenta(numeroCuenta);
        return movimientoRepository.findByCuentaIdOrderByFechaDesc(cuenta.getId());
    }


    @Transactional(transactionManager = "nacionalTransactionManager")
    public CuentaNacional debitar(String numeroCuenta, BigDecimal monto, String referencia) {
        log.info("[BancoNacional] Iniciando débito de {} en cuenta {} | ref: {}", monto, numeroCuenta, referencia);

        CuentaNacional cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta origen no encontrada: " + numeroCuenta));

        if (!cuenta.getActiva()) {
            throw new RuntimeException("La cuenta " + numeroCuenta + " está inactiva");
        }

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new RuntimeException("Saldo insuficiente en cuenta " + numeroCuenta
                    + ". Saldo actual: " + cuenta.getSaldo() + ", monto requerido: " + monto);
        }

        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.subtract(monto);
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

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

    @Transactional(transactionManager = "nacionalTransactionManager")
    public void revertirDebito(String numeroCuenta, BigDecimal monto, String referencia) {
        log.warn("[BancoNacional] COMPENSANDO - Revirtiendo débito de {} en cuenta {} | ref: {}", monto, numeroCuenta, referencia);

        CuentaNacional cuenta = cuentaRepository.findByNumeroCuentaWithLock(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada para compensación: " + numeroCuenta));

        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.add(monto);
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

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
