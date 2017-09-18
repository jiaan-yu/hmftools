import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.google.common.io.Resources;
import com.hartwig.hmftools.bachelor.Programs;

import org.junit.Test;
import org.xml.sax.SAXException;

public class BachelorTest {

    private final static String TEST_XML = Resources.getResource("valid.xml").getPath();
    private final static String TEST_INVALID_XML = Resources.getResource("invalid.xml").getPath();

    @Test
    public void TestValid() throws JAXBException, IOException, SAXException {

        final JAXBContext context = JAXBContext.newInstance(com.hartwig.hmftools.bachelor.Programs.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        final Programs programs = (Programs) unmarshaller.unmarshal(new File(TEST_XML));

        assertTrue(programs != null);
    }

    @Test
    public void TestInvalid() throws JAXBException, IOException, SAXException {
        final JAXBContext context = JAXBContext.newInstance(com.hartwig.hmftools.bachelor.Programs.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();

        Programs programs = null;
        try {
            programs = (Programs) unmarshaller.unmarshal(new File(TEST_INVALID_XML));
        } catch (final JAXBException e) {
        }

        assertTrue(programs == null);
    }
}
