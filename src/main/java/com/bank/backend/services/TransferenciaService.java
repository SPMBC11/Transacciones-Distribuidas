package com.bank.backend.services;

import com.bank.backend.dto.TransferenciaResponse;
import com.bank.backend.model.internacional.CuentaInternacional;
import com.bank.backend.model.nacional.CuentaNacional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Orquestador del Patrón SAGA para transferencias interbancarias.
 *
 * Flujo SAGA:
 *   Paso 1 → Débito en Banco Nacional (PostgreSQL)
 *   Paso 2 → Crédito en Banco Internacional (MySQL)
 *
 * Compensación:
 *   Si el Paso 2 falla → Se revierte el Paso 1 (revertirDebito)
 *
 * NOTA: Cada paso es una transacción LOCAL independiente que se commitea
 * inmediatamente. No se usa 2PC/XA. La consistencia se logra mediante
 * transacciones compensatorias.
 */
@Service
public class TransferenciaService {

    private static final Logger log = LoggerFactory.getLogger(TransferenciaService.class);

    private final BancoNacionalService bancoNacionalService;
    private final BancoInternacionalService bancoInternacionalService;

    public TransferenciaService(BancoNacionalService bancoNacionalService,
                                 BancoInternacionalService bancoInternacionalService) {
        this.bancoNacionalService = bancoNacionalService;
        this.bancoInternacionalService = bancoInternacionalService;
    }

    /**
     * Ejecuta una transferencia interbancaria usando el patrón SAGA.
     *
     * Banco Nacional (PostgreSQL) → Banco Internacional (MySQL)
     *
     * @param cuentaOrigen  Número de cuenta en Banco Nacional (ej: "BN-001")
     * @param cuentaDestino Número de cuenta en Banco Internacional (ej: "BI-001")
     * @param monto         Monto a transferir
     * @return TransferenciaResponse con el resultado de la operación
     */
    public TransferenciaResponse transferirNacionalAInternacional(String cuentaOrigen, String cuentaDestino, BigDecimal monto) {

        // Generar referencia única para rastrear toda la SAGA
        String referencia = UUID.randomUUID().toString();

        log.info("========== INICIO SAGA [ref: {}] ==========", referencia);
        log.info("Transferencia: {} → {} | Monto: {}", cuentaOrigen, cuentaDestino, monto);

        // ─── PASO 1: Débito en Banco Nacional (PostgreSQL) ───
        CuentaNacional cuentaDebitada;
        try {
            log.info("[SAGA Paso 1] Ejecutando débito en Banco Nacional...");
            cuentaDebitada = bancoNacionalService.debitar(cuentaOrigen, monto, referencia);
            log.info("[SAGA Paso 1] ✅ Débito exitoso. Saldo resultante: {}", cuentaDebitada.getSaldo());
        } catch (Exception e) {
            // El débito falló → No hay nada que compensar
            log.error("[SAGA Paso 1] ❌ Débito fallido: {}", e.getMessage());
            return TransferenciaResponse.fallida(referencia, cuentaOrigen, cuentaDestino, monto,
                    "Error en débito (Banco Nacional): " + e.getMessage());
        }

        // ─── PASO 2: Crédito en Banco Internacional (MySQL) ───
        CuentaInternacional cuentaAcreditada;
        try {
            log.info("[SAGA Paso 2] Ejecutando crédito en Banco Internacional...");
            cuentaAcreditada = bancoInternacionalService.acreditar(cuentaDestino, monto, referencia);
            log.info("[SAGA Paso 2] ✅ Crédito exitoso. Saldo resultante: {}", cuentaAcreditada.getSaldo());
        } catch (Exception e) {
            // El crédito falló → COMPENSAR revirtiendo el débito del Paso 1
            log.error("[SAGA Paso 2] ❌ Crédito fallido: {}. Iniciando compensación...", e.getMessage());

            try {
                bancoNacionalService.revertirDebito(cuentaOrigen, monto, referencia);
                log.warn("[SAGA Compensación] ✅ Débito revertido exitosamente en Banco Nacional");
                return TransferenciaResponse.compensada(referencia, cuentaOrigen, cuentaDestino, monto,
                        "Crédito falló en Banco Internacional (" + e.getMessage() + "). Débito revertido.");
            } catch (Exception compensacionEx) {
                // Caso crítico: la compensación también falló
                log.error("[SAGA Compensación] ❌ FALLO CRÍTICO: No se pudo revertir el débito: {}", compensacionEx.getMessage());
                return TransferenciaResponse.fallida(referencia, cuentaOrigen, cuentaDestino, monto,
                        "FALLO CRÍTICO: Crédito falló y la compensación también falló. " +
                                "Requiere intervención manual. Ref: " + referencia);
            }
        }

        // ─── SAGA completada exitosamente ───
        log.info("========== SAGA COMPLETADA [ref: {}] ==========", referencia);
        return TransferenciaResponse.exitosa(referencia, cuentaOrigen, cuentaDestino, monto,
                cuentaDebitada.getSaldo(), cuentaAcreditada.getSaldo());
    }

    /**
     * Ejecuta una transferencia interbancaria en sentido inverso.
     *
     * Banco Internacional (MySQL) → Banco Nacional (PostgreSQL)
     *
     * @param cuentaOrigen  Número de cuenta en Banco Internacional (ej: "BI-001")
     * @param cuentaDestino Número de cuenta en Banco Nacional (ej: "BN-001")
     * @param monto         Monto a transferir
     * @return TransferenciaResponse con el resultado de la operación
     */
    public TransferenciaResponse transferirInternacionalANacional(String cuentaOrigen, String cuentaDestino, BigDecimal monto) {

        String referencia = UUID.randomUUID().toString();

        log.info("========== INICIO SAGA INVERSA [ref: {}] ==========", referencia);
        log.info("Transferencia: {} → {} | Monto: {}", cuentaOrigen, cuentaDestino, monto);

        // ─── PASO 1: Débito en Banco Internacional (MySQL) ───
        // Reutilizamos acreditar con monto negativo? No. Necesitamos un método debitar en Internacional.
        // Como el servicio Internacional solo tiene acreditar y revertirCredito, simulamos el débito
        // usando la lógica inversa: restar saldo y registrar movimiento DEBITO.
        CuentaInternacional cuentaDebitada;
        try {
            log.info("[SAGA Paso 1] Ejecutando débito en Banco Internacional...");
            cuentaDebitada = bancoInternacionalService.debitar(cuentaOrigen, monto, referencia);
            log.info("[SAGA Paso 1] ✅ Débito exitoso. Saldo resultante: {}", cuentaDebitada.getSaldo());
        } catch (Exception e) {
            log.error("[SAGA Paso 1] ❌ Débito fallido: {}", e.getMessage());
            return TransferenciaResponse.fallida(referencia, cuentaOrigen, cuentaDestino, monto,
                    "Error en débito (Banco Internacional): " + e.getMessage());
        }

        // ─── PASO 2: Crédito en Banco Nacional (PostgreSQL) ───
        CuentaNacional cuentaAcreditada;
        try {
            log.info("[SAGA Paso 2] Ejecutando crédito en Banco Nacional...");
            cuentaAcreditada = bancoNacionalService.acreditar(cuentaDestino, monto, referencia);
            log.info("[SAGA Paso 2] ✅ Crédito exitoso. Saldo resultante: {}", cuentaAcreditada.getSaldo());
        } catch (Exception e) {
            log.error("[SAGA Paso 2] ❌ Crédito fallido: {}. Iniciando compensación...", e.getMessage());

            try {
                bancoInternacionalService.revertirDebito(cuentaOrigen, monto, referencia);
                log.warn("[SAGA Compensación] ✅ Débito revertido exitosamente en Banco Internacional");
                return TransferenciaResponse.compensada(referencia, cuentaOrigen, cuentaDestino, monto,
                        "Crédito falló en Banco Nacional (" + e.getMessage() + "). Débito revertido.");
            } catch (Exception compensacionEx) {
                log.error("[SAGA Compensación] ❌ FALLO CRÍTICO: No se pudo revertir el débito: {}", compensacionEx.getMessage());
                return TransferenciaResponse.fallida(referencia, cuentaOrigen, cuentaDestino, monto,
                        "FALLO CRÍTICO: Crédito falló y la compensación también falló. " +
                                "Requiere intervención manual. Ref: " + referencia);
            }
        }

        log.info("========== SAGA INVERSA COMPLETADA [ref: {}] ==========", referencia);
        return TransferenciaResponse.exitosa(referencia, cuentaOrigen, cuentaDestino, monto,
                cuentaDebitada.getSaldo(), cuentaAcreditada.getSaldo());
    }
}
