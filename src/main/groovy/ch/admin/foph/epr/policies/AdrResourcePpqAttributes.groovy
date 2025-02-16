package ch.admin.foph.epr.policies

import groovy.transform.CompileStatic
import org.herasaf.xacml.core.context.impl.ResourceType
import org.herasaf.xacml.core.dataTypeAttribute.impl.AnyURIDataTypeAttribute
import org.herasaf.xacml.core.dataTypeAttribute.impl.DateDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.herasaf.types.IiDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants

import java.time.LocalDate

import static ch.admin.foph.epr.policies.AdrUtils.toIi

/**
 * Attributes of the element "Resource" of an ADR request for trigger event PPQ.
 */
@CompileStatic
class AdrResourcePpqAttributes extends AdrAttributes<ResourceType> {

    private final String policySetId
    private final String eprSpid
    private final String referencedPolicySet
    private final String fromDate
    private final String toDate

    AdrResourcePpqAttributes(String policySetId, String eprSpid, String referencedPolicySet, LocalDate fromDate, LocalDate toDate) {
        this.policySetId = Objects.requireNonNull(policySetId)
        this.eprSpid = Objects.requireNonNull(eprSpid)
        this.referencedPolicySet = Objects.requireNonNull(referencedPolicySet)
        this.fromDate = AdrUtils.formatDate(fromDate)
        this.toDate = AdrUtils.formatDate(toDate)
    }

    @Override
    List<ResourceType> createAdrRequestParts() {
        def result = new ResourceType()
        add(result.attributes, PpqConstants.AttributeIds.XACML_1_0_RESOURCE_ID, new AnyURIDataTypeAttribute(), policySetId)
        add(result.attributes, PpqConstants.AttributeIds.EHEALTH_SUISSSE_2015_EPR_SPID, new IiDataTypeAttribute(), toIi(eprSpid))
        add(result.attributes, PpqConstants.AttributeIds.EHEALTH_SUISSSE_2015_REFERENCED_POLICY_SET, new AnyURIDataTypeAttribute(), referencedPolicySet)
        add(result.attributes, 'urn:e-health-suisse:2023:policy-attributes:start-date', new DateDataTypeAttribute(), fromDate)
        add(result.attributes, 'urn:e-health-suisse:2023:policy-attributes:end-date', new DateDataTypeAttribute(), toDate)
        return [result]
    }
    
}
