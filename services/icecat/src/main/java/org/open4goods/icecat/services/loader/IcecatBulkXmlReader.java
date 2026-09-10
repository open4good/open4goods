package org.open4goods.icecat.services.loader;

import java.io.File;

import org.open4goods.icecat.jaxb.ICECATInterface;
import org.open4goods.icecat.jaxb.Response;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Unmarshals an Icecat bulk XML export file into the JAXB-generated {@link Response} contract
 * (the classes generated from {@code src/main/resources/xsd/icecat.xsd}).
 */
final class IcecatBulkXmlReader {

    private static final JAXBContext CONTEXT = createContext();

    private IcecatBulkXmlReader() {
    }

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(ICECATInterface.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * @throws JAXBException if the file cannot be unmarshalled
     * @throws ClassCastException if the export's root choice is a {@code Product} rather than a {@code Response}
     */
    static Response readResponse(File file) throws JAXBException {
        Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();
        ICECATInterface root = (ICECATInterface) unmarshaller.unmarshal(file);
        return (Response) root.getResponseOrProduct();
    }
}
