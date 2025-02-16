package ch.admin.foph.epr.policies;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.herasaf.xacml.core.api.PolicyRetrievalPoint;
import org.herasaf.xacml.core.context.impl.RequestType;
import org.herasaf.xacml.core.policy.Evaluatable;
import org.herasaf.xacml.core.policy.EvaluatableID;
import org.herasaf.xacml.core.policy.PolicyMarshaller;
import org.herasaf.xacml.core.policy.impl.EvaluatableIDImpl;
import org.herasaf.xacml.core.policy.impl.PolicyType;
import org.openehealth.ipf.commons.ihe.xacml20.Xacml20Utils;
import org.openehealth.ipf.commons.xml.XmlUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Dmytro Rud
 */
@Slf4j
public class PolicyRepository implements PolicyRetrievalPoint {

    static {
        Xacml20Utils.initializeHerasaf();
    }

    private static final String ORIGINAL_DIR_NAME = "src/main/resources/policy-stack/original";
    private static final String MODIFIED_DIR_NAME = "src/main/resources/policy-stack/modified";

    private final Map<EvaluatableID, Evaluatable> basisPolicies = new HashMap<>();
    private final Map<EvaluatableID, Evaluatable> patientPoliciesByPolicyId = new HashMap<>();
    private final Map<String, List<Evaluatable>> patientPoliciesByEprSpid = new HashMap<>();

    private final Set<String> ruleIds = new HashSet<>();

    /**
     * @param needUseModified whether the Policy Repository shall consider modified <i>base</i>
     *                        policies and policy sets (<code>true</code>)
     *                        or only the original ones (<code>false</code>).
     */
    public PolicyRepository(boolean needUseModified) throws Exception {
        loadBasePolicies(ORIGINAL_DIR_NAME);
        if (needUseModified) {
            loadBasePolicies(MODIFIED_DIR_NAME);
        }
    }

    public void addOriginal201PolicySet(String eprSpid) throws Exception {
        addPatientPolicy(ORIGINAL_DIR_NAME, 201, eprSpid, xml -> {
            xml = xml.replace(">\"epd-spid-goes-here\"<", ">" + eprSpid + "<");
            return xml;
        });
    }

    public void addModified201PolicySet(String eprSpid) throws Exception {
        addPatientPolicy(MODIFIED_DIR_NAME, 201, eprSpid, xml -> {
            xml = xml.replace(">\"epd-spid-goes-here\"<", ">" + eprSpid + "<");
            return xml;
        });
    }

    public void addOriginal202PolicySet(String eprSpid, String emergencyAccessLevel) throws Exception {
        addPatientPolicy(ORIGINAL_DIR_NAME, 202, eprSpid, xml -> {
            xml = xml.replace("urn:e-health-suisse:2015:policies:access-level:normal", emergencyAccessLevel);
            return xml;
        });
    }

    public void addModified202PolicySet(String eprSpid, String emergencyAccessLevel) throws Exception {
        addPatientPolicy(MODIFIED_DIR_NAME, 202, eprSpid, xml -> {
            xml = xml.replace("urn:e-health-suisse:2015:policies:access-level:normal", emergencyAccessLevel);
            return xml;
        });
    }

    public void addOriginal203PolicySet(String eprSpid, String provideAccessLevel) throws Exception {
        addPatientPolicy(ORIGINAL_DIR_NAME, 203, eprSpid, xml -> {
            xml = xml.replace("urn:e-health-suisse:2015:policies:provide-level:normal", provideAccessLevel);
            return xml;
        });
    }

    public void addModified203PolicySet(String eprSpid, String provideAccessLevel) throws Exception {
        addPatientPolicy(MODIFIED_DIR_NAME, 203, eprSpid, xml -> {
            xml = xml.replace("urn:e-health-suisse:2015:policies:provide-level:normal", provideAccessLevel);
            return xml;
        });
    }

    public void addOriginal301PolicySet(String eprSpid, String gln, LocalDate toDate, String hcpReadAccessLevel) throws Exception {
        addPatientPolicy(ORIGINAL_DIR_NAME, 301, eprSpid, xml -> {
            xml = xml.replace("2.999", gln);
            xml = xml.replace("2016-02-07", AdrUtils.formatDate(toDate));
            xml = xml.replace("urn:e-health-suisse:2015:policies:exclusion-list", hcpReadAccessLevel);
            return xml;
        });
    }

    public void addModified301PolicySet(String eprSpid, String gln, LocalDate toDate, String hcpReadAccessLevel) throws Exception {
        addPatientPolicy(MODIFIED_DIR_NAME, 301, eprSpid, xml -> {
            xml = xml.replace("2.999", gln);
            xml = xml.replace("2016-02-07", AdrUtils.formatDate(toDate));
            xml = xml.replace("urn:e-health-suisse:2015:policies:exclusion-list", hcpReadAccessLevel);
            return xml;
        });
    }

    public void addOriginal302PolicySet(String eprSpid, String groupOid, LocalDate toDate, String groupReadAccessLevel) throws Exception {
        addPatientPolicy(ORIGINAL_DIR_NAME, 302, eprSpid, xml -> {
            xml = xml.replace("urn:oid:2.999", groupOid);
            xml = xml.replace("2016-02-07", AdrUtils.formatDate(toDate));
            xml = xml.replace("urn:e-health-suisse:2015:policies:access-level:normal", groupReadAccessLevel);
            return xml;
        });
    }

    public void addModified302PolicySet(String eprSpid, String groupOid, LocalDate toDate, String groupReadAccessLevel) throws Exception {
        addPatientPolicy(MODIFIED_DIR_NAME, 302, eprSpid, xml -> {
            xml = xml.replace("urn:oid:2.999", groupOid);
            xml = xml.replace("2016-02-07", AdrUtils.formatDate(toDate));
            xml = xml.replace("urn:e-health-suisse:2015:policies:access-level:normal", groupReadAccessLevel);
            return xml;
        });
    }

    public void addOriginal303PolicySet(String eprSpid, String representativeId, LocalDate toDate) throws Exception {
        addPatientPolicy(ORIGINAL_DIR_NAME, 303, eprSpid, xml -> {
            xml = xml.replace("2.999", representativeId);
            xml = xml.replace("2016-02-07", AdrUtils.formatDate(toDate));
            return xml;
        });
    }

    public void addModified303PolicySet(String eprSpid, String representativeId, LocalDate toDate) throws Exception {
        addPatientPolicy(MODIFIED_DIR_NAME, 303, eprSpid, xml -> {
            xml = xml.replace("2.999", representativeId);
            xml = xml.replace("2016-02-07", AdrUtils.formatDate(toDate));
            return xml;
        });
    }

    public void addOriginal304PolicySet(String eprSpid, String gln, LocalDate fromDate, LocalDate toDate, String hcpReadAccessLevel) throws Exception {
        addPatientPolicy(ORIGINAL_DIR_NAME, 304, eprSpid, xml -> {
            xml = xml.replace("2.999", gln);
            xml = xml.replace("2023-02-01", AdrUtils.formatDate(fromDate));
            xml = xml.replace("2023-02-28", AdrUtils.formatDate(toDate));
            xml = xml.replace("urn:e-health-suisse:2015:policies:access-level:delegation-and-normal", hcpReadAccessLevel);
            return xml;
        });
    }

    public void addModified304PolicySet(String eprSpid, String gln, LocalDate fromDate, LocalDate toDate, String hcpReadAccessLevel) throws Exception {
        addPatientPolicy(ORIGINAL_DIR_NAME, 304, eprSpid, xml -> {
            xml = xml.replace("2.999", gln);
            xml = xml.replace("2023-02-01", AdrUtils.formatDate(fromDate));
            xml = xml.replace("2023-02-28", AdrUtils.formatDate(toDate));
            xml = xml.replace("urn:e-health-suisse:2015:policies:access-level:delegation-and-normal", hcpReadAccessLevel);
            return xml;
        });
    }

    private void registerPolicy(String fn, Evaluatable evaluatable, Map<EvaluatableID, Evaluatable> targetMap) throws Exception {
        if (evaluatable instanceof PolicyType policy) {
            String ruleId = policy.getOrderedRules().getFirst().getRuleId();
            log.debug("file {} --> rule ID {}", fn, ruleId);
            if (!ruleIds.add(ruleId)) {
                log.warn("Duplicate rule ID {}", ruleId);
            }
        }
        boolean replace = (targetMap.put(evaluatable.getId(), evaluatable) != null);
        log.info("{} policy '{}' from {}", replace ? "Replaced" : "Loaded", evaluatable.getId(), fn);
    }

    private void loadBasePolicies(String dirName) throws Exception {
        List<String> policyFileNames;
        try (Stream<Path> stream = Files.walk(Paths.get(dirName + "/Privacy Policy Stack/EPD Policy Stack").toAbsolutePath())) {
            policyFileNames = stream
                    .map(path -> path.toString().toLowerCase(Locale.ROOT))
                    .filter(fn -> fn.endsWith(".xml"))
                    .toList();

            for (String fn : policyFileNames) {
                log.debug("Read base policy from {}", fn);
                try (InputStream inputStream = new FileInputStream(fn)) {
                    String xml = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    Evaluatable evaluatable = PolicyMarshaller.unmarshal(XmlUtils.source(xml));
                    registerPolicy(fn, evaluatable, basisPolicies);
                }
            }
        }
    }

    private void addPatientPolicy(String dirName, int templateId, String eprSpid, Function<String, String> placeholderFiller) throws Exception {
        List<String> policyFileNames;
        try (Stream<Path> stream = Files.walk(Paths.get(dirName + "/Privacy Policy Stack/Patient Specific via Policy Manager").toAbsolutePath())) {
            policyFileNames = stream
                    .map(path -> path.toString().toLowerCase(Locale.ROOT))
                    .filter(fn -> {
                        int pos = fn.lastIndexOf(File.separatorChar);
                        return fn.substring(pos + 1).startsWith(Integer.toString(templateId) + '-') && fn.endsWith(".xml");
                    })
                    .toList();

            if (policyFileNames.size() != 1) {
                throw new Exception("Expected exactly one template with the ID " + templateId + ", got " + policyFileNames.size());
            }

            String fn = policyFileNames.getFirst();
            try (InputStream inputStream = new FileInputStream(fn)) {
                log.debug("Read patient policy from {}", fn);
                String xml = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                xml = xml.replaceAll("=\"epr-spid-goes-here\"", "=\"" + eprSpid + "\"");
                xml = placeholderFiller.apply(xml);
                Evaluatable evaluatable = PolicyMarshaller.unmarshal(XmlUtils.source(xml));
                registerPolicy(fn, evaluatable, patientPoliciesByPolicyId);
                patientPoliciesByEprSpid.putIfAbsent(eprSpid, new ArrayList<>());
                patientPoliciesByEprSpid.get(eprSpid).add(evaluatable);
            }
        }
    }

    @Override
    public Evaluatable getEvaluatable(EvaluatableID evaluatableId) {
        return basisPolicies.getOrDefault(evaluatableId, patientPoliciesByPolicyId.get(evaluatableId));
    }

    /**
     * Returns entry-point policies for the given ADR request: the ones related to the patient, to the PADM, and to the DADM.
     */
    @Override
    public List<Evaluatable> getEvaluatables(RequestType request) {
        String eprSpid = AdrUtils.extractEprSpid(request);
        if (isPatientKnown(eprSpid)) {
            ArrayList<Evaluatable> result = new ArrayList<>();
            result.add(basisPolicies.get(new EvaluatableIDImpl("urn:e-health-suisse:2015:policies:policy-bootstrap")));
            result.add(basisPolicies.get(new EvaluatableIDImpl("urn:e-health-suisse:2015:policies:doc-admin")));
            result.addAll(patientPoliciesByEprSpid.get(eprSpid));
            return result;
        } else {
            return List.of();
        }
    }

    public boolean isPatientKnown(String eprSpid) {
        return patientPoliciesByEprSpid.containsKey(eprSpid);
    }

}