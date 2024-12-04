package ch.admin.foph.epr.policies

import org.herasaf.xacml.core.context.impl.*
import org.herasaf.xacml.core.dataTypeAttribute.DataTypeAttribute
import org.herasaf.xacml.core.dataTypeAttribute.impl.AnyURIDataTypeAttribute
import org.herasaf.xacml.core.dataTypeAttribute.impl.DateDataTypeAttribute
import org.herasaf.xacml.core.dataTypeAttribute.impl.StringDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.herasaf.types.CvDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.herasaf.types.IiDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.model.CE
import org.openehealth.ipf.commons.ihe.xacml20.model.NameQualifier
import org.openehealth.ipf.commons.ihe.xacml20.model.PurposeOfUse
import org.openehealth.ipf.commons.ihe.xacml20.model.SubjectRole
import org.openehealth.ipf.commons.ihe.xacml20.stub.hl7v3.CV
import org.openehealth.ipf.commons.ihe.xacml20.stub.hl7v3.II

import static org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants.AttributeIds
import static org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants.CodingSystemIds

/**
 * @author Dmytro Rud
 */
class AdrMessageCreator {

    private static AttributeType createAttribute(String id, DataTypeAttribute dataType, Object value) {
        if (!value) {
            return null
        }
        return new AttributeType(
                attributeId: id,
                dataType: dataType,
                attributeValues: [new AttributeValueType(content: [value])]
        )
    }

    private static CV toCv(CE ce) {
        if (!ce) {
            return null
        }
        return new CV(
                code:           ce.code,
                codeSystem:     ce.codeSystem,
                codeSystemName: ce.codeSystemName,
                displayName:    ce.displayName,
        )
    }

    private static II toIi(String eprSpid) {
        return new II(extension: eprSpid, root: CodingSystemIds.SWISS_PATIENT_ID)
    }

    static RequestType createAdrRequest(
            String subjectId,
            NameQualifier subjectIdQualifier,
            String homeCommunityId,
            SubjectRole subjectRole,
            String organizationOid,
            PurposeOfUse purposeOfUse,
            String resourceId,
            String eprSpid,
            CE confidentialityCode,
            String referencedPolicySet,
            String actionId,
            String fromDate,
            String toDate)
    {
        return new RequestType(
                subjects: [
                        new SubjectType(
                                attributes: [
                                        createAttribute(AttributeIds.XACML_1_0_SUBJECT_ID, new StringDataTypeAttribute(), subjectId),
                                        createAttribute(AttributeIds.XACML_1_0_SUBJECT_ID_QUALIFIER, new StringDataTypeAttribute(), subjectIdQualifier.qualifier),
                                        createAttribute(AttributeIds.XCA_2010_HOME_COMMUNITY_ID, new AnyURIDataTypeAttribute(), homeCommunityId),
                                        createAttribute(AttributeIds.XACML_2_0_SUBJECT_ROLE, new CvDataTypeAttribute(), toCv(subjectRole.code)),
                                        createAttribute(AttributeIds.XSPA_1_0_SUBJECT_ORGANIZATION_ID, new AnyURIDataTypeAttribute(), organizationOid),
                                        createAttribute(AttributeIds.XSPA_1_0_SUBJECT_PURPOSE_OF_USE, new CvDataTypeAttribute(), toCv(purposeOfUse.code)),
                                ].findAll {it},
                        ),
                ],
                resources: [
                        new ResourceType(
                                attributes: [
                                        createAttribute(AttributeIds.XACML_1_0_RESOURCE_ID, new IiDataTypeAttribute(), toIi(resourceId)),
                                        createAttribute(AttributeIds.EHEALTH_SUISSSE_2015_EPR_SPID, new IiDataTypeAttribute(), toIi(eprSpid)),
                                        createAttribute(AttributeIds.XDS_2007_CONFIDENTIALITY_CODE, new CvDataTypeAttribute(), toCv(confidentialityCode)),
                                        createAttribute(AttributeIds.EHEALTH_SUISSSE_2015_REFERENCED_POLICY_SET, new AnyURIDataTypeAttribute(), referencedPolicySet),
                                        createAttribute('urn:e-health-suisse:2023:policy-attributes:start-date', new DateDataTypeAttribute(), fromDate),
                                        createAttribute('urn:e-health-suisse:2023:policy-attributes:end-date', new DateDataTypeAttribute(), toDate),
                                ].findAll {it},
                        ),
                ],
                action: new ActionType(
                        attributes: [
                                createAttribute(AttributeIds.XACML_1_0_ACTION_ID, new AnyURIDataTypeAttribute(), actionId),
                        ].findAll {it},
                ),
                environment: new EnvironmentType(),
        )
    }

}