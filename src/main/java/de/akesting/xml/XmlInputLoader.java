package de.akesting.xml;

import java.io.File;
import java.net.URL;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.akesting.autogen.AdaptiveSmoothingMethodProject;

public final class XmlInputLoader {

    /** The Constant logger. */
    private final static Logger LOG = LoggerFactory.getLogger(XmlInputLoader.class);

    private static final Class<?> SCENARIO_FACTORY = AdaptiveSmoothingMethodProject.class;

    private static final String SCENARIO_XML_SCHEMA = "/schema/AdaptiveSmoothingMethod.xsd";

    private static final URL SCENARIO_XSD_URL = XmlInputLoader.class.getResource(SCENARIO_XML_SCHEMA);

    private XmlInputLoader() {
    }

    public static AdaptiveSmoothingMethodProject validateAndLoadScenarioInput(final File xmlFile) throws JAXBException,
            SAXException {
        return new FileUnmarshaller<AdaptiveSmoothingMethodProject>().load(xmlFile,
                AdaptiveSmoothingMethodProject.class, SCENARIO_FACTORY, SCENARIO_XSD_URL);
    }

    public static AdaptiveSmoothingMethodProject getInputData(File xmlFile) {
        AdaptiveSmoothingMethodProject inputData = null;
        try {
            System.out.println("try to open file = " + xmlFile.getName());
            inputData = XmlInputLoader.validateAndLoadScenarioInput(xmlFile);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (SAXException e) {
            throw new IllegalArgumentException(e.toString());
        }
        if (inputData == null) {
            System.out.println("input not valid. exit.");
            System.exit(-1);
        }
        return inputData;
    }
}
