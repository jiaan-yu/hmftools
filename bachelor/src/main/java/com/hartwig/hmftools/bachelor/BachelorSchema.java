package com.hartwig.hmftools.bachelor;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.google.common.io.Resources;

import org.xml.sax.SAXException;

public class BachelorSchema {
    public static boolean validateXML(final String path) throws SAXException, IOException {
        final File file = new File(path);

        final Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).
                newSchema(new StreamSource(Resources.getResource("bachelor.xsd").openStream()));
        final Validator validator = schema.newValidator();

        try {
            validator.validate(new StreamSource(file));
        } catch (final Exception e) {
            return false;
        }

        return true;
    }
}
