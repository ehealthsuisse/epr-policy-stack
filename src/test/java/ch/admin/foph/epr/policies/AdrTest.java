package ch.admin.foph.epr.policies;

import lombok.extern.slf4j.Slf4j;
import org.herasaf.xacml.core.context.impl.DecisionType;
import org.herasaf.xacml.core.context.impl.ResourceType;
import org.herasaf.xacml.core.context.impl.ResultType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.xacml20.model.NameQualifier;
import org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants;
import org.openehealth.ipf.commons.ihe.xacml20.model.PurposeOfUse;
import org.openehealth.ipf.commons.ihe.xacml20.model.SubjectRole;
import org.openehealth.ipf.commons.ihe.xacml20.stub.saml20.assertion.AssertionType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.saml20.protocol.ResponseType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.assertion.XACMLAuthzDecisionStatementType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.protocol.XACMLAuthzDecisionQueryType;

import java.util.Date;
import java.util.List;

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

    private static final AdrMessageCreator ADR_MESSAGE_CREATOR = new AdrMessageCreator(HOME_COMMUNITY_ID_1);

    @Test
    public void test1() throws Exception {
        PolicyRepository pr = new PolicyRepository();
        pr.addOriginal201PolicySet(EPR_SPID_1);
        pr.addOriginal202PolicySet(EPR_SPID_1, "urn:e-health-suisse:2015:policies:access-level:normal");
        pr.addOriginal203PolicySet(EPR_SPID_1, "urn:e-health-suisse:2015:policies:provide-level:restricted");
        pr.addOriginal301PolicySet(EPR_SPID_1, GLN_1, new Date(), "urn:e-health-suisse:2015:policies:access-level:normal");
        pr.addOriginal302PolicySet(EPR_SPID_1, ORG_ID_1, new Date(), "urn:e-health-suisse:2015:policies:access-level:restricted");
        pr.addOriginal303PolicySet(EPR_SPID_1, REP_ID_1, new Date());

        AdrResourceXdsAttributes resourceAttrs = new AdrResourceXdsAttributes(EPR_SPID_1, HOME_COMMUNITY_ID_1);
        XACMLAuthzDecisionQueryType adrRequest = ADR_MESSAGE_CREATOR.createAdrRequest(
                new AdrSubjectAttributes(
                        GLN_1,
                        NameQualifier.PROFESSIONAL,
                        HOME_COMMUNITY_ID_1,
                        SubjectRole.PROFESSIONAL,
                        List.of(ORG_ID_1),
                        PurposeOfUse.NORMAL
                ),
                resourceAttrs,
                PpqConstants.ActionIds.ITI_18);

        doTest(pr, resourceAttrs, adrRequest, DecisionType.PERMIT, DecisionType.PERMIT, DecisionType.NOT_APPLICABLE);
    }

    private static void doTest(PolicyRepository pr, AdrAttributes<ResourceType> resourceAttrs, XACMLAuthzDecisionQueryType adrRequest, DecisionType... expectedDecisions) {
        AdrProvider adrProvider = new AdrProvider(pr, ADR_MESSAGE_CREATOR);
        ResponseType adrResponse = adrProvider.handleRequest(adrRequest);
        AssertionType assertion = (AssertionType) adrResponse.getAssertionOrEncryptedAssertion().getFirst();
        XACMLAuthzDecisionStatementType statement = (XACMLAuthzDecisionStatementType) assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().getFirst();
        List<ResultType> results = statement.getResponse().getResults();
        List<ResourceType> resources = resourceAttrs.createAdrRequestParts();

        Assertions.assertEquals(results.size(), resources.size());
        for (int i = 0; i < results.size(); i++) {
            Assertions.assertEquals(expectedDecisions[i], results.get(i).getDecision());
        }
    }
}
