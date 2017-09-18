import static org.junit.Assert.assertTrue;

import java.io.IOException;

import com.google.common.io.Resources;
import com.hartwig.hmftools.bachelor.BachelorSchema;

import org.junit.Test;
import org.xml.sax.SAXException;

public class BachelorTest {

    private final static String TEST_XML = Resources.getResource("test.xml").getPath();

    @Test
    public void TestXML() throws SAXException, IOException {
        assertTrue(BachelorSchema.validateXML(TEST_XML));
    }
}
