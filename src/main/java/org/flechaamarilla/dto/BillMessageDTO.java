package org.flechaamarilla.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para representar un mensaje en la cola unificada de facturación
 * Contiene el contenido XML y el estado actual de la factura
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillMessageDTO {
    
    /**
     * Identificador único del mensaje para seguimiento y deduplicación
     */
    private String messageId;
    
    /**
     * Contenido XML de la factura (en diferentes etapas)
     */
    private String xmlContent;
    
    /**
     * Estado actual de la factura en el proceso
     */
    private BillStatus status;
    
    /**
     * Timestamp para rastreo y procesamiento ordenado
     */
    private LocalDateTime timestamp;
    
    /**
     * Datos adicionales opcionales (como IDs de referencia, datos de usuario, etc)
     */
    private String additionalData;
    
    /**
     * Error si ocurre alguno durante el procesamiento
     */
    private String error;
    
    /**
     * Servicio que procesó por última vez el mensaje
     */
    private String processedBy;
    
    /**
     * Estados posibles de una factura en el proceso
     */
    public enum BillStatus {
        /**
         * Factura recién generada y lista para validar (Core → Validator)
         */
        NUEVA,
        
        /**
         * Factura validada contra esquemas y lista para firmar (Validator → Signer)
         */
        VALIDADA,
        
        /**
         * Factura firmada y lista para timbrar (Signer → Stamp)
         */
        FIRMADA,
        
        /**
         * Factura timbrada y lista para entregar (Stamp → Core)
         */
        TIMBRADA,
        
        /**
         * Factura completada y procesada
         */
        COMPLETADA,
        
        /**
         * Error en alguna fase del proceso
         */
        ERROR
    }
    
    /**
     * Crea una copia del mensaje actual con un nuevo estado
     * @param newStatus El nuevo estado para la factura
     * @param processedBy El servicio que está actualizando el estado
     * @return Una nueva instancia del mensaje con el estado actualizado
     */
    public BillMessageDTO withStatus(BillStatus newStatus, String processedBy) {
        return BillMessageDTO.builder()
                .messageId(this.messageId) // Mantiene el mismo ID
                .xmlContent(this.xmlContent)
                .status(newStatus)
                .timestamp(LocalDateTime.now())
                .additionalData(this.additionalData)
                .processedBy(processedBy)
                .build();
    }
    
    /**
     * Crea una copia del mensaje actual con un nuevo contenido XML y estado
     * @param newXmlContent El nuevo contenido XML
     * @param newStatus El nuevo estado para la factura
     * @param processedBy El servicio que está actualizando el mensaje
     * @return Una nueva instancia del mensaje con el XML y estado actualizados
     */
    public BillMessageDTO withXmlAndStatus(String newXmlContent, BillStatus newStatus, String processedBy) {
        return BillMessageDTO.builder()
                .messageId(this.messageId) // Mantiene el mismo ID
                .xmlContent(newXmlContent)
                .status(newStatus)
                .timestamp(LocalDateTime.now())
                .additionalData(this.additionalData)
                .processedBy(processedBy)
                .build();
    }
    
    /**
     * Crea un mensaje de error basado en el mensaje actual
     * @param errorMessage El mensaje de error
     * @param processedBy El servicio que está reportando el error
     * @return Una nueva instancia del mensaje con estado ERROR
     */
    public BillMessageDTO withError(String errorMessage, String processedBy) {
        return BillMessageDTO.builder()
                .messageId(this.messageId) // Mantiene el mismo ID
                .xmlContent(this.xmlContent)
                .status(BillStatus.ERROR)
                .timestamp(LocalDateTime.now())
                .additionalData(this.additionalData)
                .error(errorMessage)
                .processedBy(processedBy)
                .build();
    }
}
