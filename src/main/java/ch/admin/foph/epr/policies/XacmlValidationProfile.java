package ch.admin.foph.epr.policies;

import org.openehealth.ipf.commons.xml.CombinedXmlValidationProfile;

import java.util.Map;

/**
 * Validation profile for PPQ-1 requests.
 *
 * @author Dmytro Rud
 */
public class XacmlValidationProfile implements CombinedXmlValidationProfile {

    @Override
    public boolean isValidRootElement(String s) {
        return "AddPolicyRequest".equals(s) || "UpdatePolicyRequest".equals(s) || "DeletePolicyRequest".equals(s);
    }

    @Override
    public String getXsdPath(String s) {
        return "epr-policy-administration-combined-schema-1.3-local.xsd";
    }

    @Override
    public String getSchematronPath(String s) {
        return "schematron/epr-patient-specific-policies.sch";
    }

    @Override
    public Map<String, Object> getCustomSchematronParameters(String s) {
        return null;
    }

}
