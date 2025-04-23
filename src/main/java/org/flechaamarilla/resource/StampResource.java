package org.flechaamarilla.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.flechaamarilla.service.StampService;

@Path("/stamp")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.APPLICATION_XML)
public class StampResource {

    @Inject
    StampService stampService;

    @POST
    public Response timbrarFactura(String xmlFirmado) {
        try {
            String xmlTimbrado = stampService.localStamp(xmlFirmado);
            return Response.ok(xmlTimbrado).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al timbrar: " + e.getMessage())
                    .build();
        }
    }
}
