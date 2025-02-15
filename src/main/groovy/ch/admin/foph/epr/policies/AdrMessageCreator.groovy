package ch.admin.foph.epr.policies

import groovy.transform.CompileStatic
import org.herasaf.xacml.core.context.impl.ActionType
import org.herasaf.xacml.core.context.impl.EnvironmentType
import org.herasaf.xacml.core.context.impl.ObjectFactory
import org.herasaf.xacml.core.context.impl.RequestType
import org.herasaf.xacml.core.context.impl.ResourceType
import org.herasaf.xacml.core.context.impl.ResultType
import org.herasaf.xacml.core.context.impl.SubjectType
import org.herasaf.xacml.core.dataTypeAttribute.impl.AnyURIDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.Xacml20MessageCreator
import org.openehealth.ipf.commons.ihe.xacml20.Xacml20Status
import org.openehealth.ipf.commons.ihe.xacml20.stub.saml20.protocol.ResponseType
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.assertion.XACMLAuthzDecisionStatementType
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.protocol.XACMLAuthzDecisionQueryType

import static org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants.AttributeIds

/**
 * @author Dmytro Rud
 */
@CompileStatic
class AdrMessageCreator extends Xacml20MessageCreator {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory()

    AdrMessageCreator(String homeCommunityId) {
        super(homeCommunityId)
    }

    XACMLAuthzDecisionQueryType createAdrRequest(AdrAttributes<SubjectType> subjectAttrs, AdrAttributes<ResourceType> resourceAttrs, String actionId) {
        def request = new XACMLAuthzDecisionQueryType(
                ID: '_' + UUID.randomUUID(),
                issueInstant: XML_OBJECT_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()),
                version: '2.0',
                issuer: createIssuer(),
                returnContext: false,
                inputContextOnly: false,
        )
        request.rest << OBJECT_FACTORY.createRequest(new RequestType(
                subjects: subjectAttrs.createAdrRequestParts(),
                resources: resourceAttrs.createAdrRequestParts(),
                action: new ActionType(attributes: [
                        AdrUtils.createAttr(AttributeIds.XACML_1_0_ACTION_ID, new AnyURIDataTypeAttribute(), actionId),
                ]),
                environment: new EnvironmentType(),
        ))
        return request
    }

    ResponseType createAdrResponse(XACMLAuthzDecisionQueryType adrRequest, Xacml20Status status, List <ResultType> results) {
        def assertion = createAssertion()
        assertion.statementOrAuthnStatementOrAuthzDecisionStatement << new XACMLAuthzDecisionStatementType(
                response: new org.herasaf.xacml.core.context.impl.ResponseType(
                        results: results,
                ),
        )

        def response = createResponse(status, null, assertion)
        response.inResponseTo = adrRequest.ID
        return response
    }

}