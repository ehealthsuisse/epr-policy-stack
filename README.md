# Unit tests for EPR policy sets

## Location of policies and policy sets

The policy stack is located in the folder `src/main/resources/policy-stack`.  The contents of the subfolder `original` shall correspond
to the current state of the official policy stack from [GitHub](https://github.com/ehealthsuisse/ch-epr-adr-ppq).  The subfolder
`modified` is intended for changed policies and policy sets.  For example, if we have created a new version of the policy set 101, 
we shall store it as `src/main/resources/policy-stack/modified/EPR Policy Stack/Base Policy Sets/101-base-policyset-access-normal.xml`.
In the modified policies and policy sets, the policy/policy set IDs shall remain the same and in the original ones.

## Test process

First, we have to initialize a Policy Repository and fill it with patient-specific policies according to the use case we want to test.
On the initialization, we shall specify whether the Policy Repository shall consider modified base policies and policy sets, or
load only the original ones.  Then, to add patient-specific policies, we shall use methods `addOriginal${id}PolicySet()` or 
`addModified${id}PolicySet()`, depending from whether we want to use an original or a modified template with the corresponding 
ID (201-304).

Example:

```java
// create a policy repository with only original base policies and policy sets
PolicyRepository pr = new PolicyRepository(false);

// "onboard" a patient with EPR-SPID "765012345678901234" 
pr.addOriginal201PolicySet("765012345678901234");
pr.addOriginal202PolicySet("765012345678901234", "urn:e-health-suisse:2015:policies:access-level:normal");
pr.addOriginal203PolicySet("765012345678901234", "urn:e-health-suisse:2015:policies:provide-level:restricted");

// allow a HCP with the GLN 7600000000000 access to the patient's documents with the confidentiality level "normal" till today
pr.addOriginal301PolicySet("765012345678901234", "7600000000000", LocalDate.now(), "urn:e-health-suisse:2015:policies:access-level:normal");
        
// allow a group with the OID "1.1.1.11" access to the patient's documents with the confidentiality level "restricted" till today
pr.addOriginal302PolicySet("765012345678901234", "urn:oid:1.1.1.11", LocalDate.now(), "urn:e-health-suisse:2015:policies:access-level:restricted");

// make a person with ID "rep1" a patient's representative until 31.12.2025
pr.addOriginal303PolicySet("765012345678901234", "rep1", LocalDate.of(2025, Month.DECEMBER, 31));
```

(Note: from technical reasons, it is not possible to omit optional parameters like the from-date and the to-date 
in template 301 -- instead, default values shall be provided, e.g. the current date as shown above.)

After that we shall create an ADR request.  An ADR request contains Subject attributes, 1..N sets of Resource attributes, 
and an Action.  Subject attributes are to be provided in an instance of the class `AdrSubjectAttributes`, e.g.:

```java
AdrSubjectAttributes subjectAttrs = new AdrSubjectAttributes(
        "7600000000000",                // subject ID, e.g. a GLN 
        NameQualifier.PROFESSIONAL,     // subject name qualifier
        SubjectRole.PROFESSIONAL,       // subject role
        List.of("urn:oid:1.1.1.11"),    // list of organization OIDs where the subject is a member 
        PurposeOfUse.NORMAL,            // purpose of use
        "urn:oid:1.1.1");               // ID of the community from where the ADR request originates 
```

Classes for resource attributes are specific for each possible ADR trigger event:
* Trigger event XDS/RMU — `AdrResourceXdsAttributes`
* Trigger event PPQ — `AdrResourcePpqAttributes`
* Trigger event ATC — `AdrResourceAtcAttributes`

Examples:

```java
AdrResourceXdsAttributes xdsResourceAttrs = new AdrResourceXdsAttributes(
        "765012345678901234",       // EPR-SPID           
        "urn:oid:2.2.2");           // ID of the community where the documents reside

AdrResourcePpqAttributes ppqResourceAttrs = new AdrResourcePpqAttributes(
        UUID.randomUUID().toString(),                               // ID of the policy set being accessed or modified 
        "765777777777777777",                                       // EPR-SPID
        "urn:e-health-suisse:2015:policies:access-level:normal",    // ID of the referenced policy set
        null,                                                       // no from-date in the new policy set    
        LocalDate.of(2025, Month.DECEMBER, 31));                    // to-date of the new policy set

AdrResourceAtcAttributes atcResourceAttrs = new AdrResourceAtcAttributes(
        "765012345678901234");      // EPR-SPID
```

Therewith, we have everything for a test.  To perform it, we call the method `AdrTest.doTest()`.
Parameters of this method are: the Policy Repository instance, attributes of the ADR request
(subject, resource(s), action), and the expected decisions.  The number of the expected decisions
shall correspond to the number of results in the ADR response, which is always 3 for the trigger
event XDS/RMU and 1 for thr trigger events PPQ and ATC: 

```java
// "permit" on "normal" because of the policies 301 and 302
// "permit" on "restricted" because of the policy 302
// "not applicable" on "secret" because there is no corresponding policy 
doTest(pr, subjectAttrs, resourceAttrs, PpqConstants.ActionIds.ITI_18,  
       DecisionType.PERMIT, DecisionType.PERMIT, DecisionType.NOT_APPLICABLE);

// "indeterminate" because there are no policies for the given EPR-SPID
doTest(pr, subjectAttrs, ppqResourceAttrs, PpqConstants.ActionIds.PPQ_1_UPDATE, 
       DecisionType.INDETERMINATE);

// "not applicable" because healthcare practitioners never use CH:ATC
doTest(pr, subjectAttrs, atcResourceAttrs, PpqConstants.ActionIds.ITI_81, 
       DecisionType.NOT_APPLICABLE);
```

## Logging

Loading of policies and policy sets is logged with the level `INFO` by the logger 
`ch.admin.foph.epr.policies.PolicyRepository`.

Payload of ADR requests and responses is logged with the level `INFO` by the logger 
`ch.admin.foph.epr.policies.AdrProvider`.

Decision process is logged with the level `DEBUG` by the logger `org.herasaf`.

Logging levels are defined in the file `src/test/resources/log4j.xml`.


