package ch.admin.foph.epr.policies;

import lombok.extern.slf4j.Slf4j;
import org.herasaf.xacml.core.context.impl.DecisionType;
import org.herasaf.xacml.core.context.impl.ResourceType;
import org.herasaf.xacml.core.context.impl.ResultType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.xacml20.chadr.*;
import org.openehealth.ipf.commons.ihe.xacml20.model.EprConstants;
import org.openehealth.ipf.commons.ihe.xacml20.model.NameQualifier;
import org.openehealth.ipf.commons.ihe.xacml20.model.PurposeOfUse;
import org.openehealth.ipf.commons.ihe.xacml20.model.SubjectRole;
import org.openehealth.ipf.commons.ihe.xacml20.stub.saml20.assertion.AssertionType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.saml20.protocol.ResponseType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.assertion.XACMLAuthzDecisionStatementType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.protocol.XACMLAuthzDecisionQueryType;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
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

    private static final AdrMessageCreator ADR_MESSAGE_CREATOR = new AdrMessageCreator(HOME_COMMUNITY_ID_1);

    @Test
    public void test1() throws Exception {
        PolicyRepository pr = new PolicyRepository(false);
        pr.addOriginal201PolicySet(EPR_SPID_1);
        pr.addOriginal202PolicySet(EPR_SPID_1, "urn:e-health-suisse:2015:policies:access-level:normal");
        pr.addOriginal203PolicySet(EPR_SPID_1, "urn:e-health-suisse:2015:policies:provide-level:restricted");
        pr.addOriginal301PolicySet(EPR_SPID_1, GLN_1, LocalDate.now(), "urn:e-health-suisse:2015:policies:access-level:normal");
        pr.addOriginal302PolicySet(EPR_SPID_1, ORG_ID_1, LocalDate.now(), "urn:e-health-suisse:2015:policies:access-level:restricted");
        pr.addOriginal303PolicySet(EPR_SPID_1, REP_ID_1, LocalDate.of(2025, Month.DECEMBER, 31));

        AdrSubjectAttributes subjectAttrs = new AdrSubjectAttributes(
                GLN_1,
                NameQualifier.PROFESSIONAL,
                SubjectRole.PROFESSIONAL,
                List.of(ORG_ID_1),
                PurposeOfUse.NORMAL,
                HOME_COMMUNITY_ID_1);

        AdrResourceXdsAttributes xdsResourceAttrs = new AdrResourceXdsAttributes(EPR_SPID_1, HOME_COMMUNITY_ID_1);
        doTest(pr, subjectAttrs, xdsResourceAttrs, EprConstants.ActionIds.ITI_18, DecisionType.PERMIT, DecisionType.PERMIT, DecisionType.NOT_APPLICABLE);

        AdrResourcePpqAttributes ppqResourceAttrs = new AdrResourcePpqAttributes(UUID.randomUUID().toString(), EPR_SPID_2,
                "urn:e-health-suisse:2015:policies:access-level:normal", LocalDate.now(), LocalDate.of(2025, Month.DECEMBER, 31));
        doTest(pr, subjectAttrs, ppqResourceAttrs, EprConstants.ActionIds.PPQ_1_UPDATE, DecisionType.INDETERMINATE);

        AdrResourceAtcAttributes atcResourceAttrs = new AdrResourceAtcAttributes(EPR_SPID_1);
        doTest(pr, subjectAttrs, atcResourceAttrs, EprConstants.ActionIds.ITI_81, DecisionType.NOT_APPLICABLE);
    }

    private void doTestChangedPolicySet(boolean needLoadModified, DecisionType... expectedDecisions) throws Exception {
        PolicyRepository pr = new PolicyRepository(needLoadModified);
        pr.addOriginal201PolicySet(EPR_SPID_1);
        pr.addOriginal202PolicySet(EPR_SPID_1, "urn:e-health-suisse:2015:policies:access-level:normal");
        pr.addOriginal203PolicySet(EPR_SPID_1, "urn:e-health-suisse:2015:policies:provide-level:normal");

        AdrSubjectAttributes subjectAttrs = new AdrSubjectAttributes(
                GLN_1,
                NameQualifier.PROFESSIONAL,
                SubjectRole.PROFESSIONAL,
                List.of(ORG_ID_1),
                PurposeOfUse.NORMAL,
                HOME_COMMUNITY_ID_1);

        AdrResourceXdsAttributes xdsResourceAttrs = new AdrResourceXdsAttributes(EPR_SPID_1, HOME_COMMUNITY_ID_1);

        doTest(pr, subjectAttrs, xdsResourceAttrs, EprConstants.ActionIds.ITI_42, expectedDecisions);
    }

    @Test
    void testChangedPolicySet() throws Exception {
        doTestChangedPolicySet(false, DecisionType.PERMIT, DecisionType.PERMIT, DecisionType.NOT_APPLICABLE);
        doTestChangedPolicySet(true , DecisionType.PERMIT, DecisionType.NOT_APPLICABLE, DecisionType.NOT_APPLICABLE);
    }

    private static void doTest(
            PolicyRepository pr,
            AdrSubjectAttributes subjectAttrs,
            AdrAttributes<ResourceType> resourceAttrs,
            String actionId,
            DecisionType... expectedDecisions)
    {
        XACMLAuthzDecisionQueryType adrRequest = ADR_MESSAGE_CREATOR.createAdrRequest(subjectAttrs, resourceAttrs, actionId);
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
