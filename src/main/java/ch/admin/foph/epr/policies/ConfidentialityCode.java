package ch.admin.foph.epr.policies;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openehealth.ipf.commons.ihe.xacml20.model.CE;

/**
 * EPR document confidentiality codes
 */
@RequiredArgsConstructor
public enum ConfidentialityCode {

    NORMAL(new CE("17621005", "2.16.840.1.113883.6.96", null, "Normal")),
    RESTRICTED(new CE("263856008", "2.16.840.1.113883.6.96", null, "Restricted")),
    SECRET(new CE("1141000195107", "2.16.756.5.30.1.127.3.4", null, "Secret"));

    @Getter
    private final CE code;

}
