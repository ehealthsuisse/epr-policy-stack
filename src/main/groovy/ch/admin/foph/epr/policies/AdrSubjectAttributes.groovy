package ch.admin.foph.epr.policies

import groovy.transform.CompileStatic
import org.herasaf.xacml.core.context.impl.SubjectType
import org.herasaf.xacml.core.dataTypeAttribute.impl.AnyURIDataTypeAttribute
import org.herasaf.xacml.core.dataTypeAttribute.impl.StringDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.herasaf.types.CvDataTypeAttribute
import org.openehealth.ipf.commons.ihe.xacml20.model.NameQualifier
import org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants
import org.openehealth.ipf.commons.ihe.xacml20.model.PurposeOfUse
import org.openehealth.ipf.commons.ihe.xacml20.model.SubjectRole

import static ch.admin.foph.epr.policies.AdrUtils.toCv

@CompileStatic
class AdrSubjectAttributes extends AdrAttributes<SubjectType> {

    private final String subjectId
    private final NameQualifier subjectIdQualifier
    private final SubjectRole subjectRole
    private final List<String> organizationOids
    private final PurposeOfUse purposeOfUse
    private final String homeCommunityId

    AdrSubjectAttributes(String subjectId, NameQualifier subjectIdQualifier, SubjectRole subjectRole, List<String> organizationOids, PurposeOfUse purposeOfUse, String homeCommunityId) {
        this.subjectId = Objects.requireNonNull(subjectId)
        this.subjectIdQualifier = Objects.requireNonNull(subjectIdQualifier)
        this.subjectRole = Objects.requireNonNull(subjectRole)
        this.organizationOids = organizationOids
        this.purposeOfUse = Objects.requireNonNull(purposeOfUse)
        this.homeCommunityId = Objects.requireNonNull(homeCommunityId)
    }

    @Override
    List<SubjectType> createAdrRequestParts() {
        def result = new SubjectType()
        add(result.attributes, PpqConstants.AttributeIds.XACML_1_0_SUBJECT_ID, new StringDataTypeAttribute(), subjectId)
        add(result.attributes, PpqConstants.AttributeIds.XACML_1_0_SUBJECT_ID_QUALIFIER, new StringDataTypeAttribute(), subjectIdQualifier.qualifier)
        add(result.attributes, PpqConstants.AttributeIds.XCA_2010_HOME_COMMUNITY_ID, new AnyURIDataTypeAttribute(), homeCommunityId)
        add(result.attributes, PpqConstants.AttributeIds.XACML_2_0_SUBJECT_ROLE, new CvDataTypeAttribute(), toCv(subjectRole.code))
        organizationOids?.forEach { orgOid ->
            add(result.attributes, PpqConstants.AttributeIds.XSPA_1_0_SUBJECT_ORGANIZATION_ID, new AnyURIDataTypeAttribute(), orgOid)
        }
        add(result.attributes, PpqConstants.AttributeIds.XSPA_1_0_SUBJECT_PURPOSE_OF_USE, new CvDataTypeAttribute(), toCv(purposeOfUse.code))
        return [result]
    }

}


