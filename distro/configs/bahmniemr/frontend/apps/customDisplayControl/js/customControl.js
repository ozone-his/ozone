'use strict';

angular.module('bahmni.common.displaycontrol.custom')
    .directive('patientAppointmentsDashboard', ['$http', '$q', '$window','appService', 'virtualConsultService', function ($http, $q, $window, appService, virtualConsultService) {
    var link = function ($scope) {
        $scope.contentUrl = appService.configBaseUrl() + "/customDisplayControl/views/patientAppointmentsDashboard.html";
        var getUpcomingAppointments = function () {
            var params = {
                q: "bahmni.sqlGet.upComingAppointments",
                v: "full",
                patientUuid: $scope.patient.uuid
            };
            return $http.get('/openmrs/ws/rest/v1/bahmnicore/sql', {
                method: "GET",
                params: params,
                withCredentials: true
            });
        };
        var getPastAppointments = function () {
            var params = {
                q: "bahmni.sqlGet.pastAppointments",
                v: "full",
                patientUuid: $scope.patient.uuid
            };
            return $http.get('/openmrs/ws/rest/v1/bahmnicore/sql', {
                method: "GET",
                params: params,
                withCredentials: true
            });
        };

        var transformDate = function (dateTimeArray) {
            var dateTime = dateTimeArray.slice();
            dateTime[1] = dateTime[1] - 1
            return Bahmni.Common.Util.DateUtil.formatDateWithoutTimeToLocal(dateTime)
        }

        var transformTime = function (dateTime) {
            return Bahmni.Common.Util.DateUtil.formatTimeToLocal(dateTime);
        }

        var getAppointmentDateAndSlot = function (startTimeInMillseconds, endTimeInMillseconds) {
            let appointmentStartDate = transformDate(startTimeInMillseconds);
            let timeSlot = transformTime(startTimeInMillseconds) + " - " + transformTime(endTimeInMillseconds) ;
            return [appointmentStartDate, timeSlot];
        }

        $q.all([getUpcomingAppointments(), getPastAppointments()]).then(function (response) {
            $scope.upcomingAppointments = response[0].data;
            $scope.upcomingAppointmentsUUIDs = [];
            $scope.teleconsultationAppointments = [];
            $scope.upcomingAppointmentsLinks = [];
            for (var i=0; i<$scope.upcomingAppointments.length; i++) {
                $scope.upcomingAppointmentsUUIDs[i] = $scope.upcomingAppointments[i].uuid;
                $scope.teleconsultationAppointments[i] = 'Virtual' === $scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_KIND;
                delete $scope.upcomingAppointments[i].uuid;
                const [date, timeSlot] = getAppointmentDateAndSlot($scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_START_DATE_IN_UTC_KEY, $scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_END_DATE_IN_UTC_KEY);
                delete $scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_START_DATE_IN_UTC_KEY;
                delete $scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_END_DATE_IN_UTC_KEY;
                delete $scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_START_DATE_KEY;
                delete $scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_END_DATE_KEY;
                $scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_DATE_KEY = date;
                $scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_SLOT_KEY = timeSlot;
                $scope.upcomingAppointmentsLinks[i] = $scope.upcomingAppointments[i].tele_health_video_link || "";
                delete $scope.upcomingAppointments[i].DASHBOARD_APPOINTMENTS_KIND;
                delete $scope.upcomingAppointments[i].tele_health_video_link;
            }
            $scope.upcomingAppointmentsHeadings = _.keys($scope.upcomingAppointments[0]);
            $scope.pastAppointments = response[1].data;
            for (let i = 0; i < $scope.pastAppointments.length; i++) {
                const [date, timeSlot] = getAppointmentDateAndSlot($scope.pastAppointments[i].DASHBOARD_APPOINTMENTS_START_DATE_IN_UTC_KEY, $scope.pastAppointments[i].DASHBOARD_APPOINTMENTS_END_DATE_IN_UTC_KEY);
                delete $scope.pastAppointments[i].DASHBOARD_APPOINTMENTS_START_DATE_IN_UTC_KEY;
                delete $scope.pastAppointments[i].DASHBOARD_APPOINTMENTS_END_DATE_IN_UTC_KEY;
                $scope.pastAppointments[i].DASHBOARD_APPOINTMENTS_DATE_KEY = date;
                $scope.pastAppointments[i].DASHBOARD_APPOINTMENTS_SLOT_KEY = timeSlot;
            }
            $scope.pastAppointmentsHeadings = _.keys($scope.pastAppointments[0]);
        });

        $scope.goToListView = function () {
            $window.open('/appointments/#/home/manage/appointments/list');
        };
        $scope.openJitsiMeet = function (appointmentIndex) {
            var uuid = $scope.upcomingAppointmentsUUIDs[appointmentIndex];
            var link = $scope.upcomingAppointmentsLinks[appointmentIndex];
            virtualConsultService.launchMeeting(uuid, link);
        };
        $scope.showJoinTeleconsultationOption = function (appointmentIndex) {
            return $scope.upcomingAppointments[appointmentIndex].DASHBOARD_APPOINTMENTS_STATUS_KEY == 'Scheduled' &&
                    $scope.teleconsultationAppointments[appointmentIndex];
        }
    };
    return {
        restrict: 'E',
        link: link,
        scope: {
            patient: "=",
            section: "="
        },
        template: '<ng-include src="contentUrl"/>'
    };
}]);

angular.module('bahmni.common.displaycontrol.custom')
    .directive('patientPrintDashboard', ['$http', '$q', '$window','appService', function ($http, $q, $window, appService) {
        var link = async function ($scope) {
            $scope.printConstants = {};
            var request = {
                method: 'get',
                url: appService.configBaseUrl() + "/customDisplayControl/constants/printConstants.json",
                dataType: 'json',
                contentType: "application/json"
            };
            await $http(request)
                .success(function (jsonData) {
                    $scope.printConstants = jsonData;
                })
                .error(function () {

                });
            var {formNames, printControls,  doctorRegistrationFieldValue, providerIdentifier, practitionerType, patientAddress, addressAndLocationAttributes} = $scope.printConstants;

            $scope.patientAddress = {line1:"",line2:""};
            $scope.printControl = printControls;
            $scope.formFieldValues = {};
            $scope.contentUrl = appService.configBaseUrl() + "/customDisplayControl/views/printCertificate.html";
            $scope.today = new Date();
            $scope.loggedInUser = $scope.$root.currentUser;
            $scope.registeredClinicName = '';
            $scope.registeredClinicAddress = '';
            $scope.registrationNumber = '';
            $scope.doctorName = '';
            var buildAddress= function(regAddress,fieldValues){
                var addressValue = '';
                var count = 0;
                fieldValues.forEach((eachField) => {
                    if(regAddress[eachField]) {
                        addressValue += ((count ===0 ) ? '' : ', ') + capitalizeFirstLetter(regAddress[eachField]);
                        count++;
                    }
                });
                return addressValue;
            }
            var capitalizeFirstLetter = function (str) {
                if(!isNaN(str)) {
                    return str;
                }
                return str[0].toUpperCase() + str.slice(1).toLowerCase();
            }
            $scope.patientAddress.line1 = buildAddress($scope.patient.address, patientAddress.line1)
            $scope.patientAddress.line2 = buildAddress($scope.patient.address, patientAddress.line2)
            $scope.printCertificate = function (printId) {
                let printContents, styles;
                printContents = document.getElementById(printId).innerHTML;
                styles = '<link id="print-certificate-styles" rel="stylesheet" href="/bahmni_config/openmrs/apps/customDisplayControl/styles/print.css"/>';
                var frame1 = document.createElement('iframe');
                frame1.name = "frame1";
                frame1.style.position = "absolute";
                frame1.style.top = "-1000000px";
                document.body.appendChild(frame1);
                var frameDoc = frame1.contentWindow ? frame1.contentWindow : frame1.contentDocument.document ? frame1.contentDocument.document : frame1.contentDocument;
                frameDoc.document.open();
                frameDoc.document.write('<html><head>');
                frameDoc.document.write(`<div class="print-wrap">${styles}</div></head><body>`);
                frameDoc.document.write(printContents);
                frameDoc.document.write('</body></html>');
                frameDoc.document.close();
                setTimeout(function () {
                    window.frames["frame1"].focus();
                    window.frames["frame1"].print();
                    document.body.removeChild(frame1);
                }, 500);
                return false;
            }

            var getLoggedInUser = function () {
                var params = {
                    v: "full"

                };
                return $http.get('/openmrs/ws/rest/v1/provider', {
                    method: "GET",
                    params: params,
                    withCredentials: true
                });
            };

            var getVisits = function () {
                var params = {
                    v: "custom:(uuid,visitType,startDatetime,stopDatetime,location,encounters:(uuid))",
                    includeInactive: true,
                    patient: $scope.patient.uuid
                };
                return $http.get('/openmrs/ws/rest/v1/visit', {
                    method: "GET",
                    params: params,
                    withCredentials: true
                });
            };
            var getObservationsByVisitId = function (visitId) {
                var params = {
                    visitUuid: visitId,
                    patient: $scope.patient.uuid
                };
                return $http.get('/openmrs/ws/rest/v1/bahmnicore/observations', {
                    method: "GET",
                    params: params,
                    withCredentials: true
                });
            };
            var getClinicLocation = function () {
                var params = {
                    operator: "ALL",
                    s: "byTags",
                    tags: 'Facility',
                    v: 'full'
                };
                return $http.get('/openmrs/ws/rest/v1/location', {
                    method: "GET",
                    params: params,
                    withCredentials: true
                });
            };
            var getAttributeValue = function (attributeList,attributeFieldValue) {
                 var selectedAttribute = attributeList.find(attribute =>
                                            (!attribute.voided) && attribute.attributeType.display === attributeFieldValue
                                        );
                                        if (selectedAttribute) {
                                            return selectedAttribute.value;
                                        }
                                        return "";

            };
            var getLatestEncounterForForm = function (observationList, formName) {
                if (observationList.length == 0) {
                    return observationList;
                }
                $scope.printControl[formName] = observationList.length === 0;
                observationList.sort((b, a) => sortDate(a["encounterDateTime"], b["encounterDateTime"]));

                var latestEncounterId = observationList[0]["encounterUuid"];
                return observationList.filter(item => item["encounterUuid"] === latestEncounterId);
            }
            var sortDate = function (a, b) {
                return (a === null && b === null) ? 0
                    : (a === null) ? 1
                        : (b === null) ? -1
                            : (a > b)
                                ? 1 : ((b) > a) ? -1 : 0;
            }

            $q.all([getLoggedInUser(), getVisits(), getClinicLocation()]).then(function (response) {
                var data = response[0].data;
                var observationData = response[1].data;
                var locationsData = response[2].data;
                var personDetails;

               if (data.results.length > 0) {
                   personDetails = data.results.find(provider => provider.person.uuid == $scope.loggedInUser.person.uuid);
                   var doctor = personDetails.attributes.find(attribute => (attribute.display.includes($scope.printConstants.practitionerType) && attribute.display.includes($scope.printConstants.providerIdentifier)));
                   if (personDetails) {
                       if(doctor){
                       $scope.doctorName = personDetails.person.display;
                       }
                      $scope.registrationNumber = getAttributeValue(personDetails.attributes, doctorRegistrationFieldValue);
                   }
               }

                if (observationData.results.length > 0) {
                    var visitId = observationData.results[0].uuid
                    $q.all([getObservationsByVisitId(visitId)]).then(function (visitResponse) {
                        var observationsValue = visitResponse[0].data;
                        if (observationsValue.length > 0) {
                            var formObservations = formNames.map(form => {
                                var formObservation = {};
                                (getLatestEncounterForForm(observationsValue.filter(item => item.formFieldPath && item.formFieldPath.includes(form)), form).forEach(eachObservation => (formObservation[eachObservation.concept.name] = (isNaN(eachObservation.valueAsString) ? eachObservation.valueAsString : parseFloat(eachObservation.valueAsString)))));
                                return formObservation
                            });
                            $scope.formFieldValues = formObservations;

                        }
                    });
                }
                if (locationsData.results.length > 0) {
                    var location = locationsData.results[0];
                    $scope.registeredClinicName = location.name;
                    $scope.registeredClinicAddress = getAttributeValue(location.attributes, addressAndLocationAttributes);
                }
            });
        };

        return {
            restrict: 'E',
            link: link,
            scope: {
                patient: "=",
                section: "=",
                observation: "=?"
            },
            template: '<ng-include src="contentUrl"/>'
        };
    }]);
