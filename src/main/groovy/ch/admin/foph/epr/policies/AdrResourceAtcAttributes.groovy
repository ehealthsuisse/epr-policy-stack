package ch.admin.foph.epr.policies

import groovy.transform.CompileStatic
import org.herasaf.xacml.core.context.impl.ResourceType
import org.herasaf.xacml.core.dataTypeAttribute.impl.AnyURIDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.herasaf.types.IiDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants

import static ch.admin.foph.epr.policies.AdrUtils.toIi

@CompileStatic
class AdrResourceAtcAttributes extends AdrAttributes<ResourceType> {

    private final String eprSpid

    AdrResourceAtcAttributes(String eprSpid) {
        this.eprSpid = Objects.requireNonNull(eprSpid)
    }

    @Override
    List<ResourceType> createAdrRequestParts() {
        def result = new ResourceType()
        add(result.attributes, PpqConstants.AttributeIds.XACML_1_0_RESOURCE_ID, new AnyURIDataTypeAttribute(), "urn:e-health-suisse:2015:epr-subset:${eprSpid}:patient-audit-trail-records".toString())
        add(result.attributes, PpqConstants.AttributeIds.EHEALTH_SUISSSE_2015_EPR_SPID, new IiDataTypeAttribute(), toIi(eprSpid))
        return [result]
    }

}
