package com.bank.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO genérico para respuestas de consulta de cuenta.
 */
public class CuentaResponse {

    private String numeroCuenta;
    private String titular;
    private BigDecimal saldo;
    private Boolean activa;
    private String banco; // "NACIONAL" o "INTERNACIONAL"
    private LocalDateTime fechaCreacion;
    private List<MovimientoResponse> movimientos;

    public CuentaResponse() {}

    // Getters y Setters
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public List<MovimientoResponse> getMovimientos() { return movimientos; }
    public void setMovimientos(List<MovimientoResponse> movimientos) { this.movimientos = movimientos; }
}
