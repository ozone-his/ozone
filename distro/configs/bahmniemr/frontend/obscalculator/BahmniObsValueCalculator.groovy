import org.apache.commons.lang.StringUtils
import org.hibernate.Query
import org.hibernate.SessionFactory;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.api.context.Context;
import org.openmrs.module.bahmniemrapi.obscalculator.ObsValueCalculator;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.openmrs.util.LocaleUtility;

import org.joda.time.LocalDate;
import org.joda.time.Months;

public class BahmniObsValueCalculator implements ObsValueCalculator {

    static Double BMI_VERY_SEVERELY_UNDERWEIGHT = 16.0;
    static Double BMI_SEVERELY_UNDERWEIGHT = 17.0;
    static Double BMI_UNDERWEIGHT = 18.5;
    static Double BMI_NORMAL = 25.0;
    static Double BMI_OVERWEIGHT = 30.0;
    static Double BMI_OBESE = 35.0;
    static Double BMI_SEVERELY_OBESE = 40.0;
    static Double ZERO = 0.0;
    static Map<BahmniObservation, BahmniObservation> obsParentMap = new HashMap<BahmniObservation, BahmniObservation>();

    public static enum BmiStatus {
        VERY_SEVERELY_UNDERWEIGHT("Very Severely Underweight"),
        SEVERELY_UNDERWEIGHT("Severely Underweight"),
        UNDERWEIGHT("Underweight"),
        NORMAL("Normal"),
        OVERWEIGHT("Overweight"),
        OBESE("Obese"),
        SEVERELY_OBESE("Severely Obese"),
        VERY_SEVERELY_OBESE("Very Severely Obese");

        private String status;

        BmiStatus(String status) {
            this.status = status
        }

        @Override
        public String toString() {
            return status;
        }
    }


    public void run(BahmniEncounterTransaction bahmniEncounterTransaction) {
        calculateAndAdd(bahmniEncounterTransaction);
    }

    static def calculateAndAdd(BahmniEncounterTransaction bahmniEncounterTransaction) {
        Collection<BahmniObservation> observations = bahmniEncounterTransaction.getObservations()
        def nowAsOfEncounter = bahmniEncounterTransaction.getEncounterDateTime() != null ? bahmniEncounterTransaction.getEncounterDateTime() : new Date();

        BahmniObservation heightObservation = find("Height", observations, null)
        BahmniObservation weightObservation = find("Weight", observations, null)
        BahmniObservation parent = null;

        if (hasValue(heightObservation) || hasValue(weightObservation)) {
            def heightObs = null, weightObs = null;
            Encounter encounter = Context.getEncounterService().getEncounterByUuid(bahmniEncounterTransaction.getEncounterUuid());
            if (encounter != null) {
                Set<Obs> latestObsOfEncounter = encounter.getObsAtTopLevel(true);
                latestObsOfEncounter.each { Obs latestObs ->
                    for (Obs groupMember : latestObs.groupMembers) {
                        heightObs = heightObs ? heightObs : (groupMember.concept.getName().name.equalsIgnoreCase("HEIGHT") ? groupMember : null);
                        weightObs = weightObs ? weightObs : (groupMember.concept.getName().name.equalsIgnoreCase("WEIGHT") ? groupMember : null);
                    }
                }
                if (isSameObs(heightObservation, heightObs) && isSameObs(weightObservation, weightObs)) {
                    return;
                }
            }


            BahmniObservation bmiDataObservation = find("BMI Data", observations, null)
            BahmniObservation bmiObservation = find("BMI", bmiDataObservation ? [bmiDataObservation] : [], null)
            BahmniObservation bmiAbnormalObservation = find("BMI Abnormal", bmiDataObservation ? [bmiDataObservation]: [], null)

            BahmniObservation bmiStatusDataObservation = find("BMI Status Data", observations, null)
            BahmniObservation bmiStatusObservation = find("BMI Status", bmiStatusDataObservation ? [bmiStatusDataObservation] : [], null)
            BahmniObservation bmiStatusAbnormalObservation = find("BMI Status Abnormal", bmiStatusDataObservation ? [bmiStatusDataObservation]: [], null)

            Patient patient = Context.getPatientService().getPatientByUuid(bahmniEncounterTransaction.getPatientUuid())
            def patientAgeInMonthsAsOfEncounter = Months.monthsBetween(new LocalDate(patient.getBirthdate()), new LocalDate(nowAsOfEncounter)).getMonths()

            parent = obsParent(heightObservation, parent)
            parent = obsParent(weightObservation, parent)

            if ((heightObservation && heightObservation.voided) && (weightObservation && weightObservation.voided)) {
                voidObs(bmiDataObservation);
                voidObs(bmiObservation);
                voidObs(bmiStatusDataObservation);
                voidObs(bmiStatusObservation);
                voidObs(bmiAbnormalObservation);
                return
            }

            def previousHeightValue = fetchLatestValue("Height", bahmniEncounterTransaction.getPatientUuid(), heightObservation, nowAsOfEncounter)
            def previousWeightValue = fetchLatestValue("Weight", bahmniEncounterTransaction.getPatientUuid(), weightObservation, nowAsOfEncounter)

            Double height = hasValue(heightObservation) && !heightObservation.voided ? heightObservation.getValue() as Double : previousHeightValue
            Double weight = hasValue(weightObservation) && !weightObservation.voided ? weightObservation.getValue() as Double : previousWeightValue
            Date obsDatetime = getDate(weightObservation) != null ? getDate(weightObservation) : getDate(heightObservation)

            if (height == null || weight == null) {
                voidObs(bmiDataObservation)
                voidObs(bmiObservation)
                voidObs(bmiStatusDataObservation)
                voidObs(bmiStatusObservation)
                voidObs(bmiAbnormalObservation)
                return
            }

            if(encounter != null) {
                voidPreviousBMIObs(encounter.getObsAtTopLevel(false));
                voidPreviousBMIObs(encounter.getObs());
            }

            bmiDataObservation = bmiDataObservation ?: createObs("BMI Data", null, bahmniEncounterTransaction, obsDatetime) as BahmniObservation
            bmiStatusDataObservation = bmiStatusDataObservation ?: createObs("BMI Status Data", null, bahmniEncounterTransaction, obsDatetime) as BahmniObservation

            def bmi = bmi(height, weight)
            bmiObservation = bmiObservation ?: createObs("BMI", bmiDataObservation, bahmniEncounterTransaction, obsDatetime) as BahmniObservation;
            bmiObservation.setValue(bmi);

            def bmiStatus = bmiStatus(bmi, patientAgeInMonthsAsOfEncounter, patient.getGender());
            bmiStatusObservation = bmiStatusObservation ?: createObs("BMI Status", bmiStatusDataObservation, bahmniEncounterTransaction, obsDatetime) as BahmniObservation;
            bmiStatusObservation.setValue(bmiStatus);

            def bmiAbnormal = bmiAbnormal(bmiStatus);
            bmiAbnormalObservation =  bmiAbnormalObservation ?: createObs("BMI Abnormal", bmiDataObservation, bahmniEncounterTransaction, obsDatetime) as BahmniObservation;
            bmiAbnormalObservation.setValue(bmiAbnormal);

            bmiStatusAbnormalObservation =  bmiStatusAbnormalObservation ?: createObs("BMI Status Abnormal", bmiStatusDataObservation, bahmniEncounterTransaction, obsDatetime) as BahmniObservation;
            bmiStatusAbnormalObservation.setValue(bmiAbnormal);

            return
        }

        BahmniObservation waistCircumferenceObservation = find("Waist Circumference", observations, null)
        BahmniObservation hipCircumferenceObservation = find("Hip Circumference", observations, null)
        if (hasValue(waistCircumferenceObservation) && hasValue(hipCircumferenceObservation)) {
            def calculatedConceptName = "Waist/Hip Ratio"
            BahmniObservation calculatedObs = find(calculatedConceptName, observations, null)
            parent = obsParent(waistCircumferenceObservation, null)

            Date obsDatetime = getDate(waistCircumferenceObservation)
            def waistCircumference = waistCircumferenceObservation.getValue() as Double
            def hipCircumference = hipCircumferenceObservation.getValue() as Double
            def waistByHipRatio = waistCircumference/hipCircumference
            if (calculatedObs == null)
                calculatedObs = createObs(calculatedConceptName, parent, bahmniEncounterTransaction, obsDatetime) as BahmniObservation

            calculatedObs.setValue(waistByHipRatio)
            return
        }

        BahmniObservation lmpObservation = find("Obstetrics, Last Menstrual Period", observations, null)
        def calculatedConceptName = "Estimated Date of Delivery"
        if (hasValue(lmpObservation)) {
            parent = obsParent(lmpObservation, null)
            def calculatedObs = find(calculatedConceptName, observations, null)

            Date obsDatetime = getDate(lmpObservation)

            LocalDate edd = new LocalDate(lmpObservation.getValue()).plusMonths(9).plusWeeks(1)
            if (calculatedObs == null)
                calculatedObs = createObs(calculatedConceptName, parent, bahmniEncounterTransaction, obsDatetime) as BahmniObservation
            calculatedObs.setValue(edd)
            return
        } else {
            def calculatedObs = find(calculatedConceptName, observations, null)
            if (hasValue(calculatedObs)) {
                voidObs(calculatedObs)
            }
        }
    }

    private static BahmniObservation obsParent(BahmniObservation child, BahmniObservation parent) {
        if (parent != null) return parent;

        if(child != null) {
            return obsParentMap.get(child)
        }
    }

    private static Date getDate(BahmniObservation observation) {
        return hasValue(observation) && !observation.voided ? observation.getObservationDateTime() : null;
    }

    private static boolean isSameObs(BahmniObservation observation, Obs editedObs) {
        if(observation && editedObs) {
            return  (editedObs.uuid == observation.encounterTransactionObservation.uuid && editedObs.valueNumeric == observation.value);
        } else if(observation == null && editedObs == null) {
            return true;
        }
        return false;
    }

    private static boolean hasValue(BahmniObservation observation) {
        return observation != null && observation.getValue() != null && !StringUtils.isEmpty(observation.getValue().toString());
    }

    private static void voidObs(BahmniObservation bmiObservation) {
        if (hasValue(bmiObservation)) {
            bmiObservation.voided = true
        }
    }

    private static void voidPreviousBMIObs(Set<Obs> bmiObs) {
        if(bmiObs) {
            bmiObs.each { Obs obs ->
                Concept concept = Context.getConceptService().getConceptByUuid(obs.getConcept().uuid);
                if (concept.getName().name.equalsIgnoreCase("BMI Data") || concept.getName().name.equalsIgnoreCase("BMI") ||
                        concept.getName().name.equalsIgnoreCase("BMI ABNORMAL") || concept.getName().name.equalsIgnoreCase("BMI Status Data")
                        || concept.getName().name.equalsIgnoreCase("BMI STATUS") || concept.getName().name.equalsIgnoreCase("BMI STATUS ABNORMAL")) {

                    obs.voided = true;
                    obs.setVoidReason("Replaced with a new one because it was changed");
                    Context.getObsService().saveObs(obs, "Replaced with a new one because it was changed");
                }
            }
        }
    }

    static BahmniObservation createObs(String conceptName, BahmniObservation parent, BahmniEncounterTransaction encounterTransaction, Date obsDatetime) {
        def concept = Context.getConceptService().getConceptByName(conceptName)
        if (concept == null && !LocaleUtility.getDefaultLocale().equals(Context.getLocale())) {
            List<Concept> conceptsByName = Context.getConceptService().getConceptsByName(conceptName, LocaleUtility.getDefaultLocale(), false);
            if (!conceptsByName.isEmpty()) {
                concept = conceptsByName[0];
            }
        }
        BahmniObservation newObservation = new BahmniObservation()
        newObservation.setConcept(new EncounterTransaction.Concept(concept.getUuid(), conceptName))
        newObservation.setObservationDateTime(obsDatetime);
        parent == null ? encounterTransaction.addObservation(newObservation) : parent.addGroupMember(newObservation)
        return newObservation
    }

    static def bmi(Double height, Double weight) {
        if (height == ZERO) {
            throw new IllegalArgumentException("Please enter Height greater than zero")
        } else if (weight == ZERO) {
            throw new IllegalArgumentException("Please enter Weight greater than zero")
        }
        Double heightInMeters = height / 100;
        Double value = weight / (heightInMeters * heightInMeters);
        return new BigDecimal(value).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    };

    static def bmiStatus(Double bmi, Integer ageInMonth, String gender) {
        BMIChart bmiChart = readCSV(OpenmrsUtil.getApplicationDataDirectory() + "obscalculator/BMI_chart.csv");
        def bmiChartLine = bmiChart.get(gender, ageInMonth);
        if(bmiChartLine != null ) {
            return bmiChartLine.getStatus(bmi);
        }

        if (bmi < BMI_VERY_SEVERELY_UNDERWEIGHT) {
            return BmiStatus.VERY_SEVERELY_UNDERWEIGHT;
        }
        if (bmi < BMI_SEVERELY_UNDERWEIGHT) {
            return BmiStatus.SEVERELY_UNDERWEIGHT;
        }
        if (bmi < BMI_UNDERWEIGHT) {
            return BmiStatus.UNDERWEIGHT;
        }
        if (bmi < BMI_NORMAL) {
            return BmiStatus.NORMAL;
        }
        if (bmi < BMI_OVERWEIGHT) {
            return BmiStatus.OVERWEIGHT;
        }
        if (bmi < BMI_OBESE) {
            return BmiStatus.OBESE;
        }
        if (bmi < BMI_SEVERELY_OBESE) {
            return BmiStatus.SEVERELY_OBESE;
        }
        if (bmi >= BMI_SEVERELY_OBESE) {
            return BmiStatus.VERY_SEVERELY_OBESE;
        }
        return null
    }

    static def bmiAbnormal(BmiStatus status) {
        return status != BmiStatus.NORMAL;
    };

    static Double fetchLatestValue(String conceptName, String patientUuid, BahmniObservation excludeObs, Date tillDate) {
        SessionFactory sessionFactory = Context.getRegisteredComponents(SessionFactory.class).get(0)
        def excludedObsIsSaved = excludeObs != null && excludeObs.uuid != null
        String excludeObsClause = excludedObsIsSaved ? " and obs.uuid != :excludeObsUuid" : ""
        Query queryToGetObservations = sessionFactory.getCurrentSession()
                .createQuery("select obs " +
                " from Obs as obs, ConceptName as cn " +
                " where obs.person.uuid = :patientUuid " +
                " and cn.concept = obs.concept.conceptId " +
                " and cn.name = :conceptName " +
                " and obs.voided = false" +
                " and obs.obsDatetime <= :till" +
                excludeObsClause +
                " order by obs.obsDatetime desc ");
        queryToGetObservations.setString("patientUuid", patientUuid);
        queryToGetObservations.setParameterList("conceptName", conceptName);
        queryToGetObservations.setParameter("till", tillDate);
        if (excludedObsIsSaved) {
            queryToGetObservations.setString("excludeObsUuid", excludeObs.uuid)
        }
        queryToGetObservations.setMaxResults(1);
        List<Obs> observations = queryToGetObservations.list();
        if (observations.size() > 0) {
            return observations.get(0).getValueNumeric();
        }
        return null
    }

    static BahmniObservation find(String conceptName, Collection<BahmniObservation> observations, BahmniObservation parent) {
        for (BahmniObservation observation : observations) {
            if (conceptName.equalsIgnoreCase(observation.getConcept().getName())) {
                obsParentMap.put(observation, parent);
                return observation;
            }
            BahmniObservation matchingObservation = find(conceptName, observation.getGroupMembers(), observation)
            if (matchingObservation) return matchingObservation;
        }
        return null
    }

    static BMIChart readCSV(String fileName) {
        def chart = new BMIChart();
        try {
            new File(fileName).withReader { reader ->
                def header = reader.readLine();
                reader.splitEachLine(",") { tokens ->
                    chart.add(new BMIChartLine(tokens[0], tokens[1], tokens[2], tokens[3], tokens[4], tokens[5]));
                }
            }
        } catch (FileNotFoundException e) {
        }
        return chart;
    }

    static class BMIChartLine {
        public String gender;
        public Integer ageInMonth;
        public Double third;
        public Double fifteenth;
        public Double eightyFifth;
        public Double ninetySeventh;

        BMIChartLine(String gender, String ageInMonth, String third, String fifteenth, String eightyFifth, String ninetySeventh) {
            this.gender = gender
            this.ageInMonth = ageInMonth.toInteger();
            this.third = third.toDouble();
            this.fifteenth = fifteenth.toDouble();
            this.eightyFifth = eightyFifth.toDouble();
            this.ninetySeventh = ninetySeventh.toDouble();
        }

        public BmiStatus getStatus(Double bmi) {
            if(bmi < third) {
                return BmiStatus.SEVERELY_UNDERWEIGHT
            } else if(bmi < fifteenth) {
                return BmiStatus.UNDERWEIGHT
            } else if(bmi < eightyFifth) {
                return BmiStatus.NORMAL
            } else if(bmi < ninetySeventh) {
                return BmiStatus.OVERWEIGHT
            } else {
                return BmiStatus.OBESE
            }
        }
    }

    static class BMIChart {
        List<BMIChartLine> lines;
        Map<BMIChartLineKey, BMIChartLine> map = new HashMap<BMIChartLineKey, BMIChartLine>();

        public add(BMIChartLine line) {
            def key = new BMIChartLineKey(line.gender, line.ageInMonth);
            map.put(key, line);
        }

        public BMIChartLine get(String gender, Integer ageInMonth) {
            def key = new BMIChartLineKey(gender, ageInMonth);
            return map.get(key);
        }
    }

    static class BMIChartLineKey {
        public String gender;
        public Integer ageInMonth;

        BMIChartLineKey(String gender, Integer ageInMonth) {
            this.gender = gender
            this.ageInMonth = ageInMonth
        }

        boolean equals(o) {
            if (this.is(o)) return true
            if (getClass() != o.class) return false

            BMIChartLineKey bmiKey = (BMIChartLineKey) o

            if (ageInMonth != bmiKey.ageInMonth) return false
            if (gender != bmiKey.gender) return false

            return true
        }

        int hashCode() {
            int result
            result = (gender != null ? gender.hashCode() : 0)
            result = 31 * result + (ageInMonth != null ? ageInMonth.hashCode() : 0)
            return result
        }
    }
}
