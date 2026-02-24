package com.bank.backend.controller;

import com.bank.backend.dto.CuentaResponse;
import com.bank.backend.dto.MovimientoResponse;
import com.bank.backend.model.internacional.CuentaInternacional;
import com.bank.backend.model.internacional.MovimientoInternacional;
import com.bank.backend.model.nacional.CuentaNacional;
import com.bank.backend.model.nacional.MovimientoNacional;
import com.bank.backend.services.BancoInternacionalService;
import com.bank.backend.services.BancoNacionalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador REST para consultas de cuentas y movimientos.
 *
 * Endpoints:
 *   GET /api/cuentas/nacional/{numeroCuenta}
 *   GET /api/cuentas/nacional/{numeroCuenta}/saldo
 *   GET /api/cuentas/nacional/{numeroCuenta}/movimientos
 *   GET /api/cuentas/internacional/{numeroCuenta}
 *   GET /api/cuentas/internacional/{numeroCuenta}/saldo
 *   GET /api/cuentas/internacional/{numeroCuenta}/movimientos
 */
@RestController
@RequestMapping("/api/cuentas")
@CrossOrigin(origins = "*")
public class CuentaController {

    private static final Logger log = LoggerFactory.getLogger(CuentaController.class);

    private final BancoNacionalService bancoNacionalService;
    private final BancoInternacionalService bancoInternacionalService;

    public CuentaController(BancoNacionalService bancoNacionalService,
                            BancoInternacionalService bancoInternacionalService) {
        this.bancoNacionalService = bancoNacionalService;
        this.bancoInternacionalService = bancoInternacionalService;
    }

    // ─── BANCO NACIONAL (PostgreSQL) ───

    @GetMapping("/nacional/{numeroCuenta}")
    public ResponseEntity<CuentaResponse> consultarCuentaNacional(@PathVariable String numeroCuenta) {
        log.info("GET /api/cuentas/nacional/{}", numeroCuenta);
        CuentaNacional cuenta = bancoNacionalService.consultarCuenta(numeroCuenta);
        return ResponseEntity.ok(mapCuentaNacional(cuenta));
    }

    @GetMapping("/nacional/{numeroCuenta}/saldo")
    public ResponseEntity<BigDecimal> consultarSaldoNacional(@PathVariable String numeroCuenta) {
        log.info("GET /api/cuentas/nacional/{}/saldo", numeroCuenta);
        return ResponseEntity.ok(bancoNacionalService.consultarSaldo(numeroCuenta));
    }

    @GetMapping("/nacional/{numeroCuenta}/movimientos")
    public ResponseEntity<List<MovimientoResponse>> consultarMovimientosNacional(@PathVariable String numeroCuenta) {
        log.info("GET /api/cuentas/nacional/{}/movimientos", numeroCuenta);
        List<MovimientoNacional> movimientos = bancoNacionalService.consultarMovimientos(numeroCuenta);
        List<MovimientoResponse> response = movimientos.stream().map(this::mapMovimientoNacional).toList();
        return ResponseEntity.ok(response);
    }

    // ─── BANCO INTERNACIONAL (MySQL) ───

    @GetMapping("/internacional/{numeroCuenta}")
    public ResponseEntity<CuentaResponse> consultarCuentaInternacional(@PathVariable String numeroCuenta) {
        log.info("GET /api/cuentas/internacional/{}", numeroCuenta);
        CuentaInternacional cuenta = bancoInternacionalService.consultarCuenta(numeroCuenta);
        return ResponseEntity.ok(mapCuentaInternacional(cuenta));
    }

    @GetMapping("/internacional/{numeroCuenta}/saldo")
    public ResponseEntity<BigDecimal> consultarSaldoInternacional(@PathVariable String numeroCuenta) {
        log.info("GET /api/cuentas/internacional/{}/saldo", numeroCuenta);
        return ResponseEntity.ok(bancoInternacionalService.consultarSaldo(numeroCuenta));
    }

    @GetMapping("/internacional/{numeroCuenta}/movimientos")
    public ResponseEntity<List<MovimientoResponse>> consultarMovimientosInternacional(@PathVariable String numeroCuenta) {
        log.info("GET /api/cuentas/internacional/{}/movimientos", numeroCuenta);
        List<MovimientoInternacional> movimientos = bancoInternacionalService.consultarMovimientos(numeroCuenta);
        List<MovimientoResponse> response = movimientos.stream().map(this::mapMovimientoInternacional).toList();
        return ResponseEntity.ok(response);
    }

    // ─── Mappers privados ───

    private CuentaResponse mapCuentaNacional(CuentaNacional cuenta) {
        CuentaResponse r = new CuentaResponse();
        r.setNumeroCuenta(cuenta.getNumeroCuenta());
        r.setTitular(cuenta.getTitular());
        r.setSaldo(cuenta.getSaldo());
        r.setActiva(cuenta.getActiva());
        r.setBanco("NACIONAL");
        r.setFechaCreacion(cuenta.getFechaCreacion());
        return r;
    }

    private CuentaResponse mapCuentaInternacional(CuentaInternacional cuenta) {
        CuentaResponse r = new CuentaResponse();
        r.setNumeroCuenta(cuenta.getNumeroCuenta());
        r.setTitular(cuenta.getTitular());
        r.setSaldo(cuenta.getSaldo());
        r.setActiva(cuenta.getActiva());
        r.setBanco("INTERNACIONAL");
        r.setFechaCreacion(cuenta.getFechaCreacion());
        return r;
    }

    private MovimientoResponse mapMovimientoNacional(MovimientoNacional m) {
        MovimientoResponse r = new MovimientoResponse();
        r.setId(m.getId());
        r.setTipo(m.getTipo());
        r.setMonto(m.getMonto());
        r.setSaldoAnterior(m.getSaldoAnterior());
        r.setSaldoNuevo(m.getSaldoNuevo());
        r.setDescripcion(m.getDescripcion());
        r.setReferenciaTransferencia(m.getReferenciaTransferencia());
        r.setFecha(m.getFecha());
        return r;
    }

    private MovimientoResponse mapMovimientoInternacional(MovimientoInternacional m) {
        MovimientoResponse r = new MovimientoResponse();
        r.setId(m.getId());
        r.setTipo(m.getTipo());
        r.setMonto(m.getMonto());
        r.setSaldoAnterior(m.getSaldoAnterior());
        r.setSaldoNuevo(m.getSaldoNuevo());
        r.setDescripcion(m.getDescripcion());
        r.setReferenciaTransferencia(m.getReferenciaTransferencia());
        r.setFecha(m.getFecha());
        return r;
    }
}
