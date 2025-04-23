package org.flechaamarilla.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.flechaamarilla.dto.XmlRequestDTO;
import org.flechaamarilla.dto.XmlResponseDTO;
import org.flechaamarilla.service.StampService;
import org.jboss.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST endpoint for XML stamping
 * 
 * TODO: Change language convention to English in future refactorings
 */
@Path("/api/stamp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StampController {
    
    private static final Logger LOG = Logger.getLogger(StampController.class);
    
    @Inject
    StampService stampService;
    
    /**
     * Stamps an XML document using the configured service
     */
    @POST
    @Path("/stamp")
    public XmlResponseDTO stampXml(XmlRequestDTO request) {
        LOG.info("Received stamping request");
        
        try {
            // Use local stamping for development/test environments
            // In production, switch to stampService.stamp()
            String stampedXml = stampService.localStamp(request.getXmlContent());
            LOG.info("XML stamping successful");
            
            // Extract UUID from stamped XML
            String uuid = extractUUID(stampedXml);
            
            return XmlResponseDTO.builder()
                    .valid(true)
                    .xmlContent(stampedXml)
                    .uuid(uuid)
                    .build();
            
        } catch (Exception e) {
            LOG.error("Error during XML stamping", e);
            
            return XmlResponseDTO.builder()
                    .valid(false)
                    .xmlContent(request.getXmlContent())
                    .errors(java.util.Collections.singletonList("Error during stamping: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Extracts the UUID from a stamped XML
     */
    private String extractUUID(String stampedXml) {
        try {
            Pattern pattern = Pattern.compile("UUID=\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(stampedXml);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LOG.warn("Failed to extract UUID from stamped XML", e);
        }
        return null;
    }
}