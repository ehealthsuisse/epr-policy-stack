package ch.admin.foph.epr.policies;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.xml.CombinedXmlValidationProfile;
import org.openehealth.ipf.commons.xml.CombinedXmlValidator;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Dmytro Rud
 */
@Slf4j
public class SchTest {

    private static final CombinedXmlValidator COMBINED_XML_VALIDATOR = new CombinedXmlValidator();
    private static final CombinedXmlValidationProfile SCHEMATRON_VALIDATION_PROFILE = new XacmlValidationProfile();

    private static String loadFile(String fileName) throws IOException {
        try (FileInputStream inputStream = new FileInputStream("src/test/resources/task7/" + fileName)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private void doTest(String fileName, boolean errorExpected) throws Exception {
        String policy = loadFile(fileName);
        try {
            COMBINED_XML_VALIDATOR.validate(policy, SCHEMATRON_VALIDATION_PROFILE);
            Assertions.assertFalse(errorExpected);
        } catch (Exception e) {
            log.info("{} --> ", fileName, e);
            Assertions.assertTrue(errorExpected);
        }
    }

    @Test
    public void testValidation1() throws Exception {
//        doTest("policy-304-correct.xml", false);
//        doTest("policy-304-correct-no-from-date.xml", false);
//
//        doTest("policy-304-from-date-mismatch.xml", true);
//        doTest("policy-304-to-date-mismatch.xml", true);
//        doTest("policy-304-action-mismatch.xml", true);
//        doTest("policy-304-no-resource-from-date.xml", true);
//        doTest("policy-304-no-resource-to-date.xml", true);
//        doTest("policy-304-no-env-to-date.xml", true);
    }

    @Test
    public void testValidation2() throws Exception {
//        doTest("add-request-1.xml", false);
//        doTest("002634-159_IN_AddPolicy.xml", true);
    }

}