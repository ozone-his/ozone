import org.apache.commons.collections.Predicate
import org.bahmni.module.bahmnicore.contract.encounter.data.EncounterModifierData
import org.bahmni.module.bahmnicore.contract.encounter.data.EncounterModifierObservation
import org.bahmni.module.bahmnicore.encounterModifier.EncounterModifier
import org.bahmni.module.bahmnicore.service.impl.BahmniBridge
import org.codehaus.jackson.map.ObjectMapper
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction

import static org.apache.commons.collections.CollectionUtils.filter

public class TuberculosisIntakeTemplate extends EncounterModifier {

    public static final String TREATMENT_PLAN_CONCEPT_NAME = "Tuberculosis, Treatment Plan"
    public static final String WEIGHT_CONCEPT_NAME = "Weight"
    public BahmniBridge bahmniBridge;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public EncounterModifierData run(EncounterModifierData encounterModifierData) {

        this.bahmniBridge = BahmniBridge
                .create()
                .forPatient(encounterModifierData.getPatientUuid());

        def nowAsOfEncounter = encounterModifierData.getEncounterDateTime() != null ? encounterModifierData.getEncounterDateTime() : new Date();

        def weight = fetchLatestValueNumeric(WEIGHT_CONCEPT_NAME);
        if(weight == null){
            throw new RuntimeException("Patient Weight is not Available");
        }

        Collection<EncounterModifierObservation> bahmniObservations = encounterModifierData.getEncounterModifierObservations();

        EncounterModifierObservation treatmentPlanObservation = findObservation(TREATMENT_PLAN_CONCEPT_NAME, bahmniObservations);

        if (treatmentPlanObservation == null || treatmentPlanObservation.getValue() == null) {
            return encounterModifierData;
        }

        List<EncounterTransaction.DrugOrder> drugOrders = encounterModifierData.getDrugOrders();
        drugOrders.addAll(bahmniBridge.drugOrdersForRegimen(getCodedObsValue(treatmentPlanObservation.getValue())));


        switch (getCodedObsValue(treatmentPlanObservation.getValue())) {
            case "2HRZE + 4HR":
            case "3HRZE + 6HRE":
                if (isAdult(nowAsOfEncounter)) {
                    if (weight >= 20 && weight < 25) {
                        setDrugDose(drugOrders, "INH", 150);
                        setDrugDose(drugOrders, "RIF", 300);
                        setDrugDose(drugOrders, "PYZ", 625);
                        setDrugDose(drugOrders, "ETHAM", 500);
                    } else if (weight >= 25 && weight < 30) {
                        setDrugDose(drugOrders, "INH", 150);
                        setDrugDose(drugOrders, "RIF", 300);
                        setDrugDose(drugOrders, "PYZ", 750);
                        setDrugDose(drugOrders, "ETHAM", 600);
                    } else if (weight >= 30 && weight < 37) {
                        setDrugDose(drugOrders, "INH", 200);
                        setDrugDose(drugOrders, "RIF", 450);
                        setDrugDose(drugOrders, "PYZ", 750);
                        setDrugDose(drugOrders, "ETHAM", 600);
                    } else if (weight >= 37 && weight < 45) {
                        setDrugDose(drugOrders, "INH", 300);
                        setDrugDose(drugOrders, "RIF", 450);
                        setDrugDose(drugOrders, "PYZ", 1000);
                        setDrugDose(drugOrders, "ETHAM", 800);
                    } else if (weight >= 45 && weight < 48) {
                        setDrugDose(drugOrders, "INH", 300);
                        setDrugDose(drugOrders, "RIF", 600);
                        setDrugDose(drugOrders, "PYZ", 1000);
                        setDrugDose(drugOrders, "ETHAM", 800);
                    } else if (weight >= 48 && weight < 55) {
                        setDrugDose(drugOrders, "INH", 300);
                        setDrugDose(drugOrders, "RIF", 600);
                        setDrugDose(drugOrders, "PYZ", 1250);
                        setDrugDose(drugOrders, "ETHAM", 800);
                    } else if (weight >= 55) {
                        setDrugDose(drugOrders, "INH", 150);
                        setDrugDose(drugOrders, "RIF", 300);
                        setDrugDose(drugOrders, "PYZ", 750);
                        setDrugDose(drugOrders, "ETHAM", 600);
                    }
                }
                else{
                    if (weight <= 5) {
                        //TODO Compute this
                    } else if (weight > 5 && weight <= 7) {
                        setDrugDose(drugOrders, "INH", 50);
                        setDrugDose(drugOrders, "RIF", 75);
                        setDrugDose(drugOrders, "PYZ", 150);
                        setDrugDose(drugOrders, "ETHAM", 100);
                    } else if (weight > 7 && weight <= 10) {
                        setDrugDose(drugOrders, "INH", 100);
                        setDrugDose(drugOrders, "RIF", 150);
                        setDrugDose(drugOrders, "PYZ", 300);
                        setDrugDose(drugOrders, "ETHAM", 200);
                    } else if (weight > 10 && weight <= 15) {
                        setDrugDose(drugOrders, "INH", 150);
                        setDrugDose(drugOrders, "RIF", 300);
                        setDrugDose(drugOrders, "PYZ", 600);
                        setDrugDose(drugOrders, "ETHAM", 300);
                    } else if (weight > 15 && weight <= 20) {
                        setDrugDose(drugOrders, "INH", 200);
                        setDrugDose(drugOrders, "RIF", 300);
                        setDrugDose(drugOrders, "PYZ", 750);
                        setDrugDose(drugOrders, "ETHAM", 400);
                    } else if (weight > 20 && weight <= 25) {
                        setDrugDose(drugOrders, "INH", 250);
                        setDrugDose(drugOrders, "RIF", 450);
                        setDrugDose(drugOrders, "PYZ", 1000);
                        setDrugDose(drugOrders, "ETHAM", 500);
                    } else if (weight > 25 && weight <= 30) {
                        setDrugDose(drugOrders, "INH", 300);
                        setDrugDose(drugOrders, "RIF", 600);
                        setDrugDose(drugOrders, "PYZ", 1250);
                        setDrugDose(drugOrders, "ETHAM", 600);
                    } else if (weight > 30 && weight <= 35) {
                        setDrugDose(drugOrders, "INH", 300);
                        setDrugDose(drugOrders, "RIF", 600);
                        setDrugDose(drugOrders, "PYZ", 1250);
                        setDrugDose(drugOrders, "ETHAM", 700);
                    }

                }
                break;

            case "12RZFqE":
                break;

            case "9H":
                if (isAdult(nowAsOfEncounter)) {
                    if (weight >= 20 && weight < 25) {
                        setDrugDose(drugOrders, "INH", 150);
                    } else if (weight >= 25 && weight < 30) {
                        setDrugDose(drugOrders, "INH", 150);
                    } else if (weight >= 30 && weight < 37) {
                        setDrugDose(drugOrders, "INH", 200);
                    } else if (weight >= 37 && weight < 45) {
                        setDrugDose(drugOrders, "INH", 300);
                    } else if (weight >= 45 && weight < 48) {
                        setDrugDose(drugOrders, "INH", 300);
                    } else if (weight >= 48 && weight < 55) {
                        setDrugDose(drugOrders, "INH", 300);
                    } else if (weight >= 55) {
                        setDrugDose(drugOrders, "INH", 150);
                    }
                }
                else{
                    if (weight <= 5) {
                        //TODO Compute this
                    } else if (weight > 5 && weight <= 7) {
                        setDrugDose(drugOrders, "INH", 50);
                    } else if (weight > 7 && weight <= 10) {
                        setDrugDose(drugOrders, "INH", 100);
                    } else if (weight > 10 && weight <= 15) {
                        setDrugDose(drugOrders, "INH", 150);
                    } else if (weight > 15 && weight <= 20) {
                        setDrugDose(drugOrders, "INH", 200);
                    } else if (weight > 20 && weight <= 25) {
                        setDrugDose(drugOrders, "INH", 250);
                    } else if (weight > 25 && weight <= 30) {
                        setDrugDose(drugOrders, "INH", 300);
                    } else if (weight > 30 && weight <= 35) {
                        setDrugDose(drugOrders, "INH", 300);
                    }
                }
                break;
        }
        filterDrugOrdersWithBaseDoseNotSet(drugOrders)
        encounterModifierData.setDrugOrders(drugOrders);

        return encounterModifierData;
    }

    def setDrugDose(List<EncounterTransaction.DrugOrder> drugOrders, String drugName, int totalDrugDose) {
        switch(drugName){
            case "INH":
                if(totalDrugDose == 50){
                    setDoseAndQuantity(drugOrders, "Isoniazid 100mg", 0.5, 15);
                }
                else if(totalDrugDose == 100){
                    setDoseAndQuantity(drugOrders, "Isoniazid 100mg", 1, 30);
                }
                else if(totalDrugDose == 150){
                    setDoseAndQuantity(drugOrders, "Isoniazid 300mg", 0.5, 15);
                }
                else if(totalDrugDose == 200){
                    setDoseAndQuantity(drugOrders, "Isoniazid 100mg", 2, 60);
                }
                else if(totalDrugDose == 250){
                    setDoseAndQuantity(drugOrders, "Isoniazid 100mg", 2.5, 75);
                }
                else if(totalDrugDose == 300){
                    setDoseAndQuantity(drugOrders, "Isoniazid 300mg", 1, 30);
                }
                break;
            case "RIF" :
                if(totalDrugDose == 75){
                    setDoseAndQuantity(drugOrders, "Rifampicin 150mg", 0.5, 30);
                }
                else if(totalDrugDose == 150){
                    setDoseAndQuantity(drugOrders, "Rifampicin 150mg", 1, 30);
                }
                else if(totalDrugDose == 300){
                    setDoseAndQuantity(drugOrders, "Rifampicin 150mg", 2, 60);
                }
                else if(totalDrugDose == 450){
                    setDoseAndQuantity(drugOrders, "Rifampicin 450mg", 1, 30);
                }
                else if(totalDrugDose == 600){
                    setDoseAndQuantity(drugOrders, "Rifampicin 450mg", 1, 30);
                    setDoseAndQuantity(drugOrders, "Rifampicin 150mg", 1, 30);
                }
                break;
            case "PYZ" :
                if(totalDrugDose == 150){
                    setDoseAndQuantity(drugOrders, "Pyrazinamide 300mg", 0.5, 15);
                }
                else if(totalDrugDose == 300){
                    setDoseAndQuantity(drugOrders, "Pyrazinamide 300mg", 1, 30);
                }
                else if(totalDrugDose == 600){
                    setDoseAndQuantity(drugOrders, "Pyrazinamide 300mg", 2, 60);
                }
                else if(totalDrugDose == 625){
                    setDoseAndQuantity(drugOrders, "Pyrazinamide 500mg", 1.25, 38);
                }
                else if(totalDrugDose == 750){
                    setDoseAndQuantity(drugOrders, "Pyrazinamide 750mg", 1, 30);
                }
                else if(totalDrugDose == 1000){
                    setDoseAndQuantity(drugOrders, "Pyrazinamide 500mg", 2, 60);
                }
                else if(totalDrugDose == 1250){
                    setDoseAndQuantity(drugOrders, "Pyrazinamide 750mg", 1, 30);
                    setDoseAndQuantity(drugOrders, "Pyrazinamide 500mg", 1, 30);
                }
                else if(totalDrugDose == 1500){
                    setDoseAndQuantity(drugOrders, "Pyrazinamide 750mg", 2, 60);
                }
                break;
            case "ETHAM" :
                if(totalDrugDose == 100){
                    setDoseAndQuantity(drugOrders, "Ethambutal 200mg", 0.5, 15);
                }
                else if(totalDrugDose == 200){
                    setDoseAndQuantity(drugOrders, "Ethambutal 200mg", 1, 30);
                }
                if(totalDrugDose == 300){
                    setDoseAndQuantity(drugOrders, "Ethambutal 200mg", 1.5, 45);
                }
                else if(totalDrugDose == 400){
                    setDoseAndQuantity(drugOrders, "Ethambutal 200mg", 2, 60);
                }
                else if(totalDrugDose == 500){
                    setDoseAndQuantity(drugOrders, "Ethambutal 400mg", 1, 30);
                    setDoseAndQuantity(drugOrders, "Ethambutal 200mg", 0.5, 15);
                }
                if(totalDrugDose == 600){
                    setDoseAndQuantity(drugOrders, "Ethambutal 600mg", 1, 30);
                }
                else if(totalDrugDose == 700){
                    setDoseAndQuantity(drugOrders, "Ethambutal 600mg", 1, 30);
                    setDoseAndQuantity(drugOrders, "Ethambutal 200mg", 0.5, 15);
                }
                else if(totalDrugDose == 800){
                    setDoseAndQuantity(drugOrders, "Ethambutal 400mg", 2, 60);
                }
                break;
        }
    }

    private boolean isAdult(Date nowAsOfEncounter) {
        def adult = bahmniBridge.ageInYears(nowAsOfEncounter) > 12 ? true : false;
        adult
    }

    private boolean adult(int patientAgeInYears) {
        patientAgeInYears > 12
    }

    private filterDrugOrdersWithBaseDoseNotSet(List<EncounterTransaction.DrugOrder> drugOrders) {
        filter(drugOrders, new Predicate() {
            @Override
            boolean evaluate(Object drugOrder) {
                return drugOrder.getDosingInstructions().getDose() != null
            }
        })
    }

    private void setDoseAndQuantity(List<EncounterTransaction.DrugOrder> drugOrders, String drugName, BigDecimal baseDose, BigDecimal baseQuantity) {
        def drugOrder = getDrugOrder(drugOrders, drugName)
        drugOrder.getDosingInstructions().setDose(baseDose)
        drugOrder.getDosingInstructions().setQuantity(baseQuantity)
    }

    private EncounterTransaction.DrugOrder getDrugOrder(List<EncounterTransaction.DrugOrder> drugOrders, String drugName) {
        for (EncounterTransaction.DrugOrder drugOrder : drugOrders) {
            if (drugOrder.getDrug().getName().equals(drugName)) {
                return drugOrder;
            }
        }
    }

    private static String getCodedObsValue(Object codeObsVal) {
        if (codeObsVal instanceof HashMap) {
            def value = ((HashMap) codeObsVal).get("name")
            if (value instanceof HashMap) {
                return (String) ((HashMap) value).get("name")
            } else {
                return value;
            }
        };
        return (String) codeObsVal;
    }

    private EncounterModifierObservation findObservation(String conceptName, Collection<EncounterModifierObservation> bahmniObservations) {
        for (EncounterModifierObservation bahmniObservation : bahmniObservations) {
            if (conceptName.equals(bahmniObservation.getConcept().getName())) {
                return bahmniObservation;
            } else if (bahmniObservation.getGroupMembers() != null) {
                EncounterModifierObservation observation = findObservation(conceptName, bahmniObservation.getGroupMembers());
                if (observation != null) {
                    return observation;
                }
            }
        }
        return null;
    }

    Double fetchLatestValueNumeric(String conceptName) {
        def obs = bahmniBridge.latestObs(conceptName)
        return obs ? obs.getValueNumeric() : null
    }
}
