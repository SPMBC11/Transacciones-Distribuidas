package com.bank.backend.model.internacional;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que mapea la tabla 'movimiento' en MySQL (Banco Internacional).
 * Registra cada débito o crédito que ocurre en una cuenta internacional.
 */
@Entity
@Table(name = "movimiento")
public class MovimientoInternacional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Mapea AUTO_INCREMENT de MySQL
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    /**
     * Tipo de movimiento: "DEBITO" o "CREDITO"
     */
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_anterior", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_nuevo", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoNuevo;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    /**
     * UUID de la transferencia distribuida.
     * Vincula este movimiento con el débito que ocurre en PostgreSQL (Banco Nacional).
     */
    @Column(name = "referencia_transferencia", length = 50)
    private String referenciaTransferencia;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        fecha = LocalDateTime.now();
    }

    // Constructors
    public MovimientoInternacional() {}

    // Getters y Setters
    public Long getId() { return id; }

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public BigDecimal getSaldoAnterior() { return saldoAnterior; }
    public void setSaldoAnterior(BigDecimal saldoAnterior) { this.saldoAnterior = saldoAnterior; }

    public BigDecimal getSaldoNuevo() { return saldoNuevo; }
    public void setSaldoNuevo(BigDecimal saldoNuevo) { this.saldoNuevo = saldoNuevo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getReferenciaTransferencia() { return referenciaTransferencia; }
    public void setReferenciaTransferencia(String referenciaTransferencia) { this.referenciaTransferencia = referenciaTransferencia; }

    public LocalDateTime getFecha() { return fecha; }
}
