package com.bank.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferenciaResponse {

    private String referencia;
    private String estado; // EXITOSA, FALLIDA, COMPENSADA
    private String cuentaOrigen;
    private String cuentaDestino;
    private BigDecimal monto;
    private BigDecimal saldoOrigenResultante;
    private BigDecimal saldoDestinoResultante;
    private String mensaje;
    private LocalDateTime fecha;

    public TransferenciaResponse() {
        this.fecha = LocalDateTime.now();
    }

    // Builder-style setters para facilitar la construcción
    public static TransferenciaResponse exitosa(String referencia, String cuentaOrigen, String cuentaDestino,
                                                 BigDecimal monto, BigDecimal saldoOrigen, BigDecimal saldoDestino) {
        TransferenciaResponse response = new TransferenciaResponse();
        response.referencia = referencia;
        response.estado = "EXITOSA";
        response.cuentaOrigen = cuentaOrigen;
        response.cuentaDestino = cuentaDestino;
        response.monto = monto;
        response.saldoOrigenResultante = saldoOrigen;
        response.saldoDestinoResultante = saldoDestino;
        response.mensaje = "Transferencia interbancaria completada exitosamente";
        return response;
    }

    public static TransferenciaResponse fallida(String referencia, String cuentaOrigen, String cuentaDestino,
                                                 BigDecimal monto, String motivo) {
        TransferenciaResponse response = new TransferenciaResponse();
        response.referencia = referencia;
        response.estado = "FALLIDA";
        response.cuentaOrigen = cuentaOrigen;
        response.cuentaDestino = cuentaDestino;
        response.monto = monto;
        response.mensaje = motivo;
        return response;
    }

    public static TransferenciaResponse compensada(String referencia, String cuentaOrigen, String cuentaDestino,
                                                    BigDecimal monto, String motivo) {
        TransferenciaResponse response = new TransferenciaResponse();
        response.referencia = referencia;
        response.estado = "COMPENSADA";
        response.cuentaOrigen = cuentaOrigen;
        response.cuentaDestino = cuentaDestino;
        response.monto = monto;
        response.mensaje = "Transferencia fallida y compensada: " + motivo;
        return response;
    }

    // Getters y Setters
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCuentaOrigen() { return cuentaOrigen; }
    public void setCuentaOrigen(String cuentaOrigen) { this.cuentaOrigen = cuentaOrigen; }

    public String getCuentaDestino() { return cuentaDestino; }
    public void setCuentaDestino(String cuentaDestino) { this.cuentaDestino = cuentaDestino; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public BigDecimal getSaldoOrigenResultante() { return saldoOrigenResultante; }
    public void setSaldoOrigenResultante(BigDecimal saldoOrigenResultante) { this.saldoOrigenResultante = saldoOrigenResultante; }

    public BigDecimal getSaldoDestinoResultante() { return saldoDestinoResultante; }
    public void setSaldoDestinoResultante(BigDecimal saldoDestinoResultante) { this.saldoDestinoResultante = saldoDestinoResultante; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
