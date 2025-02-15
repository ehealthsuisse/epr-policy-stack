package ch.admin.foph.epr.policies;

import lombok.extern.slf4j.Slf4j;
import org.herasaf.xacml.core.api.PDP;
import org.herasaf.xacml.core.combiningAlgorithm.policy.impl.PolicyDenyOverridesAlgorithm;
import org.herasaf.xacml.core.context.impl.RequestType;
import org.herasaf.xacml.core.context.impl.ResultType;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;
import org.openehealth.ipf.commons.ihe.xacml20.Xacml20Status;
import org.openehealth.ipf.commons.ihe.xacml20.Xacml20Utils;
import org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants;
import org.openehealth.ipf.commons.ihe.xacml20.stub.saml20.protocol.ResponseType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.protocol.XACMLAuthzDecisionQueryType;
import org.openehealth.ipf.commons.xml.XmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of an Authorization Decision Provider
 */
@Slf4j
public class AdrProvider {

    private final PolicyRepository pr;
    private final PDP pdp;
    private final AdrMessageCreator adrMessageCreator;

    public AdrProvider(PolicyRepository pr, AdrMessageCreator adrMessageCreator) {
        this.pr = pr;
        this.pdp = SimplePDPFactory.getSimplePDP(new PolicyDenyOverridesAlgorithm(), pr);
        this.adrMessageCreator = adrMessageCreator;
    }

    public ResponseType handleRequest(XACMLAuthzDecisionQueryType adrRequest) {
        log.info("Received CH:ADR request:\n{}", XmlUtils.renderJaxb(Xacml20Utils.JAXB_CONTEXT, adrRequest, true));

        // split the ADR request, if necessary (because HERAS AF does not support the OASIS multiple resource profile of XACML v2.0)
        RequestType request = (RequestType) adrRequest.getRest().getFirst().getValue();
        List<RequestType> subRequests;
        if (request.getResources().size() <= 1) {
            subRequests = List.of(request);
        } else {
            subRequests = request.getResources().stream()
                    .map(resource -> {
                        RequestType subRequest = new RequestType();
                        subRequest.getSubjects().addAll(request.getSubjects());
                        subRequest.getResources().add(resource);
                        subRequest.setAction(request.getAction());
                        subRequest.setEnvironment(request.getEnvironment());
                        return subRequest;
                    })
                    .toList();
        }
        log.info("Created {} sub-requests to the PDP", subRequests.size());

        // obtain results
        List<ResultType> results = new ArrayList<>();
        for (RequestType subRequest : subRequests) {
            String eprSpid = AdrUtils.extractEprSpid(subRequest);
            ResultType result = pr.isPatientKnown(eprSpid)
                    ? pdp.evaluate(subRequest).getResults().getFirst()
                    : AdrUtils.createNotHolderOfPatientPoliciesResult();

            // copy resource ID to the result
            result.setResourceId(subRequest.getResources().getFirst().getAttributes().stream()
                    .filter(attr -> PpqConstants.AttributeIds.XACML_1_0_RESOURCE_ID.equals(attr.getAttributeId()))
                    .findAny()
                    .map(attr -> attr.getAttributeValues().getFirst().getContent().getFirst().toString())
                    .orElseGet(() -> "unknown"));
            results.add(result);
        }

        // determine the overall status
        Xacml20Status responseStatus;
        Set<String> statusCodes = results.stream()
                .map(result -> result.getStatus().getStatusCode().getValue())
                .collect(Collectors.toSet());

        if (statusCodes.contains("urn:oasis:names:tc:xacml:1.0:status:missing-attribute") || statusCodes.contains("urn:oasis:names:tc:xacml:1.0:status:syntax-error")) {
            responseStatus = Xacml20Status.REQUESTER_ERROR;
        } else if ((statusCodes.size() == 1) && "urn:oasis:names:tc:xacml:1.0:status:ok".equals(statusCodes.iterator().next())) {
            responseStatus = Xacml20Status.SUCCESS;
        } else if ((statusCodes.size() == 1) && "urn:e-health-suisse:2015:error:not-holder-of-patient-policies".equals(statusCodes.iterator().next())) {
            responseStatus = Xacml20Status.EPR_NOT_HOLDER;
        } else {
            responseStatus = Xacml20Status.RESPONDER_ERROR;
        }

        // create and return aggregated response
        ResponseType adrResponse = adrMessageCreator.createAdrResponse(adrRequest, responseStatus, results);
        log.info("Produced CH:ADR response:\n{}", XmlUtils.renderJaxb(Xacml20Utils.JAXB_CONTEXT, adrResponse, true));
        return adrResponse;
    }

    private static Xacml20Status valueOfCode(String code) {
        if (code == null) {
            return null;
        }
        for (Xacml20Status value : Xacml20Status.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown code " + code);
    }

}
