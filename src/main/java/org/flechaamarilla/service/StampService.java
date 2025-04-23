package org.flechaamarilla.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.xml.soap.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StampService {

    private static final String USERNAME = "demo";
    private static final String PASSWORD = "demo";
    private static final String ENDPOINT = "https://demo-facturacion.finkok.com/servicios/soap/stamp.wsdl";
    private static final Logger LOG = Logger.getLogger(StampService.class);
    
    // Caché para objetos utilizados frecuentemente
    private static MessageFactory cachedMessageFactory;
    private static SOAPConnectionFactory cachedConnectionFactory;
    private static DocumentBuilderFactory cachedDocumentBuilderFactory;
    private static TransformerFactory cachedTransformerFactory;
    
    // Tiempo de la última actualización del caché
    private static LocalDateTime lastCacheRefresh;
    
    // Tiempo de expiración del caché en horas
    private static final long CACHE_EXPIRATION_HOURS = 24;
    
    @PostConstruct
    public void init() {
        try {
            refreshCache();
            LOG.info("Caché de StampService inicializado correctamente");
        } catch (Exception e) {
            LOG.error("Error al inicializar caché de StampService", e);
        }
    }
    
    /**
     * Refresca todos los recursos en caché
     */
    public synchronized void refreshCache() throws Exception {
        LOG.info("Refrescando caché de StampService...");
        
        // Inicializar factories de SOAP
        cachedMessageFactory = MessageFactory.newInstance();
        cachedConnectionFactory = SOAPConnectionFactory.newInstance();
        
        // Inicializar factories de XML
        cachedDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        cachedDocumentBuilderFactory.setNamespaceAware(true);
        
        cachedTransformerFactory = TransformerFactory.newInstance();
        
        // Actualizar timestamp
        lastCacheRefresh = LocalDateTime.now();
        LOG.info("Caché de StampService actualizado");
    }
    
    /**
     * Verifica si el caché ha expirado
     */
    private boolean isCacheExpired() {
        if (lastCacheRefresh == null) {
            return true;
        }
        return lastCacheRefresh.plusHours(CACHE_EXPIRATION_HOURS).isBefore(LocalDateTime.now());
    }
    
    /**
     * Verifica y actualiza el caché si es necesario
     */
    private void checkAndRefreshCache() throws Exception {
        if (cachedMessageFactory == null || cachedConnectionFactory == null ||
            cachedDocumentBuilderFactory == null || cachedTransformerFactory == null ||
            isCacheExpired()) {
            refreshCache();
        }
    }

    public String stamp(String xmlFirmado) throws Exception {
        // Verificar caché
        checkAndRefreshCache();
        
        // Codificar XML a base64
        String xmlBase64 = Base64.getEncoder().encodeToString(xmlFirmado.getBytes(StandardCharsets.UTF_8));

        // Crear solicitud SOAP para Finkok usando factories en caché
        SOAPMessage soapMessage = cachedMessageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("ws", "http://facturacion.finkok.com/stamp");

        SOAPBody body = envelope.getBody();
        SOAPElement stamp = body.addChildElement("stamp", "ws");

        stamp.addChildElement("xml").addTextNode(xmlBase64);
        stamp.addChildElement("username").addTextNode(USERNAME);
        stamp.addChildElement("password").addTextNode(PASSWORD);

        soapMessage.saveChanges();

        // Enviar la solicitud SOAP usando conexión en caché
        SOAPConnection connection = cachedConnectionFactory.createConnection();
        SOAPMessage response = connection.call(soapMessage, ENDPOINT);

        // Leer XML timbrado de la respuesta
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.writeTo(out);
        connection.close();

        return out.toString(StandardCharsets.UTF_8);
    }


    public String localStamp(String xmlFirmado) throws Exception {
        // Verificar caché
        checkAndRefreshCache();
        
        // Parse the XML document using cached factories
        DocumentBuilder db = cachedDocumentBuilderFactory.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xmlFirmado.getBytes(StandardCharsets.UTF_8)));

        // Get root element
        Element comprobante = doc.getDocumentElement();
        String cfdiNamespace = comprobante.getNamespaceURI();

        // Generate UUID and stamp data
        String uuid = UUID.randomUUID().toString().toUpperCase();
        String fechaTimbrado = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String selloCFD = comprobante.getAttribute("Sello");
        String noCertificadoSAT = "00001000000504465028";
        String rfcProvCertif = "AAA010101AAA";

        // Generate simulated SAT seal
        String cadenaOriginal = "||1.1|" + uuid + "|" + fechaTimbrado + "|" + selloCFD + "|" + noCertificadoSAT + "||";
        String selloSAT = Base64.getEncoder().encodeToString(cadenaOriginal.getBytes(StandardCharsets.UTF_8));

        // Create or get Complemento element
        Element complemento;
        NodeList complementos = doc.getElementsByTagNameNS(cfdiNamespace, "Complemento");
        if (complementos.getLength() > 0) {
            complemento = (Element) complementos.item(0);
        } else {
            complemento = doc.createElementNS(cfdiNamespace, "Complemento");
            comprobante.appendChild(complemento);
        }

        // Create TimbreFiscalDigital element
        String tfdNamespace = "http://www.sat.gob.mx/TimbreFiscalDigital";
        Element tfd = doc.createElementNS(tfdNamespace, "tfd:TimbreFiscalDigital");
        tfd.setAttribute("Version", "1.1");
        tfd.setAttribute("UUID", uuid);
        tfd.setAttribute("FechaTimbrado", fechaTimbrado);
        tfd.setAttribute("SelloCFD", selloCFD);
        tfd.setAttribute("NoCertificadoSAT", noCertificadoSAT);
        tfd.setAttribute("SelloSAT", selloSAT);
        tfd.setAttribute("RfcProvCertif", rfcProvCertif);

        // Set namespaces
        tfd.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tfd", tfdNamespace);
        tfd.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        tfd.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation",
                          tfdNamespace + " http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd");

        complemento.appendChild(tfd);

        // Transform document back to string using cached transformer factory
        Transformer transformer = cachedTransformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));

        return out.toString(StandardCharsets.UTF_8);
    }
}
