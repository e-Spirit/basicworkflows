package com.espirit.moddev.basicworkflows;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.junit.Assert.assertThat;

/**
 * Created by Zaplatynski on 14.11.2014.
 */
public class ModuleXmlTest {

    /**
     * The Errors.
     */
    @Rule
    public ErrorCollector errors = new ErrorCollector();

    private static Node moduleXML;
    private static Properties pomProperties;

    @BeforeClass
    public static void setUpBefore() throws Exception {
        File file = new File(ClassLoader.getSystemClassLoader().getResource("module.xml").toURI());
        String content = FileUtils.readFileToString(file);
        moduleXML = createXMLfromString(content);

        pomProperties = new Properties();
        pomProperties.load(ClassLoader.getSystemClassLoader().getResourceAsStream("moduleTest.properties"));
    }

    private static Node createXMLfromString(String xmlString) throws Exception {
        return DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(new ByteArrayInputStream(xmlString.getBytes()))
            .getDocumentElement();
    }

    @Test
    public void testIfThereIsAVersion() throws Exception {
        assertThat("Expect a specific path", moduleXML, hasXPath("/module/version"));
    }

    @Test
    public void testIfVersionIsEqualToPomVersion() throws Exception {
        String expectedVersion = pomProperties.getProperty("version");
        assertThat("Expect a specific value", moduleXML, hasXPath("/module/version", equalTo(expectedVersion)));
    }

    @Test
    public void testIfThereIsADisplayName() throws Exception {
        assertThat("Expect a specific path", moduleXML, hasXPath("/module/displayname"));
    }

    @Test
    public void testIfDisplayNameIsEqualToBasicWorkflows() throws Exception {
        String expectedName = pomProperties.getProperty("displayName");
        assertThat("Expect a specific value", moduleXML, hasXPath("/module/displayname", equalTo(expectedName)));
    }

    @Test
    public void testIfThereIsAName() throws Exception {
        assertThat("Expect a specific path", moduleXML, hasXPath("/module/name"));
    }

    @Test
    public void testIfNameIsEqualTobasicworkflows() throws Exception {
        String expectedName = pomProperties.getProperty("name");
        assertThat("Expect a specific value", moduleXML, hasXPath("/module/name", equalTo(expectedName)));
    }

    @Test
    public void testIfThereIsADescription() throws Exception {
        assertThat("Expect a specific path", moduleXML, hasXPath("/module/description"));
    }

    @Test
    public void testIfDescriptionIsEqualToArtifactId() throws Exception {
        String expectedDescription = pomProperties.getProperty("description");
        assertThat("Expect a specific value", moduleXML, hasXPath("/module/description", equalTo(expectedDescription)));
    }

    /**
     * Test all classes.
     *
     * @throws Exception the exception
     */
    @Test
    public void testAllClasses() throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xPath.evaluate("//class",
                                                   moduleXML, XPathConstants.NODESET);
        System.out.println("Number of classes: " + nodes.getLength());
        boolean hasErrors = false;
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node clazz = nodes.item(i);
            System.out.println("Check if '" + clazz.getTextContent() + "' is existent ...");
            try {
                Class.forName(clazz.getTextContent());
            } catch (Exception e) {
                errors.addError(e);
            }
        }
    }
}
