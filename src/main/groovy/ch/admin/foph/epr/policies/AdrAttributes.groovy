package ch.admin.foph.epr.policies

import groovy.transform.CompileStatic
import org.herasaf.xacml.core.context.impl.AttributeType
import org.herasaf.xacml.core.dataTypeAttribute.DataTypeAttribute

@CompileStatic
abstract class AdrAttributes<T> {

    protected static void add(List<AttributeType> targetList, String id, DataTypeAttribute dataType, Object value) {
        if (value) {
            targetList << AdrUtils.createAttr(id, dataType, value)
        }
    }

    abstract List<T> createAdrRequestParts()

}