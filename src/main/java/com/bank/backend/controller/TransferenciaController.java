package com.bank.backend.controller;

import com.bank.backend.dto.TransferenciaRequest;
import com.bank.backend.dto.TransferenciaResponse;
import com.bank.backend.services.TransferenciaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Controlador REST para transferencias interbancarias (Patrón SAGA).
 *
 * Endpoints:
 *   POST /api/transferencias/nacional-a-internacional
 *   POST /api/transferencias/internacional-a-nacional
 */
@RestController
@RequestMapping("/api/transferencias")
@CrossOrigin(origins = "*")
public class TransferenciaController {

    private static final Logger log = LoggerFactory.getLogger(TransferenciaController.class);

    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    /**
     * Transferencia del Banco Nacional (PostgreSQL) → Banco Internacional (MySQL)
     *
     * Body esperado:
     * {
     *   "cuentaOrigen": "BN-001",
     *   "cuentaDestino": "BI-001",
     *   "monto": 1000.00,
     *   "descripcion": "Pago de servicio"
     * }
     */
    @PostMapping("/nacional-a-internacional")
    public ResponseEntity<TransferenciaResponse> transferirNacionalAInternacional(
            @RequestBody TransferenciaRequest request) {

        log.info("POST /api/transferencias/nacional-a-internacional | {} → {} | Monto: {}",
                request.getCuentaOrigen(), request.getCuentaDestino(), request.getMonto());

        validarRequest(request);

        TransferenciaResponse response = transferenciaService.transferirNacionalAInternacional(
                request.getCuentaOrigen(),
                request.getCuentaDestino(),
                request.getMonto()
        );

        return buildResponse(response);
    }

    /**
     * Transferencia del Banco Internacional (MySQL) → Banco Nacional (PostgreSQL)
     *
     * Body esperado:
     * {
     *   "cuentaOrigen": "BI-001",
     *   "cuentaDestino": "BN-001",
     *   "monto": 500.00,
     *   "descripcion": "Reembolso"
     * }
     */
    @PostMapping("/internacional-a-nacional")
    public ResponseEntity<TransferenciaResponse> transferirInternacionalANacional(
            @RequestBody TransferenciaRequest request) {

        log.info("POST /api/transferencias/internacional-a-nacional | {} → {} | Monto: {}",
                request.getCuentaOrigen(), request.getCuentaDestino(), request.getMonto());

        validarRequest(request);

        TransferenciaResponse response = transferenciaService.transferirInternacionalANacional(
                request.getCuentaOrigen(),
                request.getCuentaDestino(),
                request.getMonto()
        );

        return buildResponse(response);
    }

    private void validarRequest(TransferenciaRequest request) {
        if (request.getCuentaOrigen() == null || request.getCuentaOrigen().isBlank()) {
            throw new IllegalArgumentException("La cuenta origen es obligatoria");
        }
        if (request.getCuentaDestino() == null || request.getCuentaDestino().isBlank()) {
            throw new IllegalArgumentException("La cuenta destino es obligatoria");
        }
        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
    }

    private ResponseEntity<TransferenciaResponse> buildResponse(TransferenciaResponse response) {
        return switch (response.getEstado()) {
            case "EXITOSA" -> ResponseEntity.ok(response);
            case "COMPENSADA" -> ResponseEntity.status(409).body(response); // 409 Conflict
            case "FALLIDA" -> ResponseEntity.badRequest().body(response);
            default -> ResponseEntity.internalServerError().body(response);
        };
    }
}
