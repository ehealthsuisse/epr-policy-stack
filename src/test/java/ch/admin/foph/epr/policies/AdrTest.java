package ch.admin.foph.epr.policies;

import lombok.extern.slf4j.Slf4j;
import org.herasaf.xacml.core.api.PDP;
import org.herasaf.xacml.core.combiningAlgorithm.policy.impl.PolicyDenyOverridesAlgorithm;
import org.herasaf.xacml.core.context.impl.DecisionType;
import org.herasaf.xacml.core.context.impl.RequestType;
import org.herasaf.xacml.core.context.impl.ResponseType;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.xacml20.model.CE;
import org.openehealth.ipf.commons.ihe.xacml20.model.NameQualifier;
import org.openehealth.ipf.commons.ihe.xacml20.model.PurposeOfUse;
import org.openehealth.ipf.commons.ihe.xacml20.model.SubjectRole;

import java.util.Date;
import java.util.UUID;

/**
 * @author Dmytro Rud
 */
@Slf4j
public class AdrTest {

    private static final String GLN_1 = "1111111111111";
    private static final String GLN_2 = "2222222222222";

    private static final String EPR_SPID_1 = "333333333333333333";
    private static final String EPR_SPID_2 = "444444444444444444";

    private static final String ORG_ID_1 = "urn:oid:1.1.1.11";
    private static final String ORG_ID_2 = "urn:oid:2.2.2.22";

    private static final String REP_ID_1 = "rep1";
    private static final String REP_ID_2 = "rep2";

    private static final String HOME_COMMUNITY_ID_1 = "urn:oid:1.1.1";
    private static final String HOME_COMMUNITY_ID_2 = "urn:oid:2.2.2";

    private static class ConfCodes {
        private static final CE NORMAL = new CE("17621005", "2.16.840.1.113883.6.96", "SNOMED", "Normal");
        private static final CE RESTRICTED = new CE("263856008", "2.16.840.1.113883.6.96", "SNOMED", "Restricted");
        private static final CE SECRET = new CE("1141000195107", "2.16.756.5.30.1.127.3.4", "SwissEPR", "Secret");
    }

    private static class ActionIds {
        private static final String ITI_18 = "urn:ihe:iti:2007:RegistryStoredQuery";
        private static final String ITI_42 = "urn:ihe:iti:2007:RegisterDocumentSet-b";
        private static final String ITI_57 = "urn:ihe:iti:2010:UpdateDocumentSet";
        private static final String ITI_81 = "urn:e-health-suisse:2015:patient-audit-administration:RetrieveAtnaAudit";
        private static final String ITI_92 = "urn:ihe:iti:2018:RestrictedUpdateDocumentSet";
        private static final String PPQ_1_QUERY = "urn:e-health-suisse:2015:policy-administration:PolicyQuery";
        private static final String PPQ_1_ADD = "urn:e-health-suisse:2015:policy-administration:AddPolicy";
        private static final String PPQ_1_UPDATE = "urn:e-health-suisse:2015:policy-administration:UpdatePolicy";
        private static final String PPQ_1_DELETE = "urn:e-health-suisse:2015:policy-administration:DeletePolicy";
    }

    @Test
    public void test1() throws Exception {
        PolicyRepository pr = new PolicyRepository();
        pr.addOriginal201PolicySet(EPR_SPID_1);
        pr.addOriginal202PolicySet(EPR_SPID_1, "urn:e-health-suisse:2015:policies:access-level:normal");
        pr.addOriginal203PolicySet(EPR_SPID_1, "urn:e-health-suisse:2015:policies:provide-level:restricted");
        pr.addOriginal301PolicySet(EPR_SPID_1, GLN_1, new Date(), "urn:e-health-suisse:2015:policies:access-level:normal");
        pr.addOriginal302PolicySet(EPR_SPID_1, ORG_ID_1, new Date(), "urn:e-health-suisse:2015:policies:access-level:restricted");
        pr.addOriginal303PolicySet(EPR_SPID_1, REP_ID_1, new Date());

        RequestType adrRequest = AdrMessageCreator.createAdrRequest(
                GLN_1,
                NameQualifier.PROFESSIONAL,
                HOME_COMMUNITY_ID_1,
                SubjectRole.PROFESSIONAL,
                ORG_ID_1,
                PurposeOfUse.NORMAL,
                null,
                EPR_SPID_1,
                ConfCodes.NORMAL,
                null,
                ActionIds.ITI_18,
                null,
                null);

        doTest(pr, adrRequest, DecisionType.PERMIT);
    }

    private static void doTest(PolicyRepository pr, RequestType adrRequest, DecisionType... expectedDecisions) {
        PDP pdp = SimplePDPFactory.getSimplePDP(new PolicyDenyOverridesAlgorithm(), pr);
        ResponseType adrResponse = pdp.evaluate(adrRequest);
        Assertions.assertEquals(adrResponse.getResults().size(), adrRequest.getResources().size());
        for (int i = 0; i < adrResponse.getResults().size(); i++) {
            Assertions.assertEquals(expectedDecisions[i], adrResponse.getResults().get(i).getDecision());
        }
    }
}
