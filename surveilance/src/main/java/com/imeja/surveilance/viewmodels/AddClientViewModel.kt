package com.imeja.surveilance.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.ibm.icu.text.SimpleDateFormat
import com.imeja.surveilance.AddPatientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.imeja.surveilance.FhirApplication
import com.imeja.surveilance.helpers.Constants.ALL_LINK_IDS
import com.imeja.surveilance.helpers.Constants.ALL_MPOX_LINK_IDS
import com.imeja.surveilance.helpers.Constants.WEEK_ENDING_DATE
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.helpers.QuestionnaireHelper
import com.imeja.surveilance.models.QuestionnaireAnswer
import com.imeja.surveilance.models.SpecimenConfig
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Specimen
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class AddClientViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {

    private var _questionnaireJson: String? = null
    val questionnaireJson: String
        get() = fetchQuestionnaireJson()

    val isPatientSaved = MutableLiveData<Boolean>()

    private val questionnaire: Questionnaire
        get() =
            FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
                .parseResource(questionnaireJson)
                    as Questionnaire

    private var fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)

    /**
     * Saves patient registration questionnaire response into the application database.
     *
     * @param questionnaireResponse patient registration questionnaire response
     */

    fun saveUserResponse(
        questionnaireResponse: QuestionnaireResponse,
        questionnaireResponseString: String,
        context: Context
    ) {
        viewModelScope.launch {
            if (QuestionnaireResponseValidator.validateQuestionnaireResponse(
                    questionnaire,
                    questionnaireResponse,
                    getApplication(),
                )
                    .values
                    .flatten()
                    .any { it is Invalid }
            ) {
                isPatientSaved.value = false
                return@launch
            }

            withContext(Dispatchers.IO) {

                val latitude = FormatterClass().getSharedPref("latitude", context)
                val longitude = FormatterClass().getSharedPref("longitude", context)

                questionnaireResponse.identifier = QuestionnaireHelper().createFullFhirIdentifier(
                    codeData = "geo-location",
                    valueData = "lat:${latitude},lon:${longitude}",
                    systemData = "geo-location-details",
                    displayData = "Latitude: $latitude, Longitude: $longitude"
                )

                fhirEngine.create(questionnaireResponse)
            }

            withContext(Dispatchers.Main) { isPatientSaved.value = true }

        }

    }

    fun savePatientData(
        questionnaireResponse: QuestionnaireResponse,
        questionnaireResponseString: String,
        context: Context
    ) {
        viewModelScope.launch {
            if (QuestionnaireResponseValidator.validateQuestionnaireResponse(
                    questionnaire,
                    questionnaireResponse,
                    getApplication(),
                )
                    .values
                    .flatten()
                    .any { it is Invalid }
            ) {
                isPatientSaved.value = false
                return@launch
            }


            val identifierSystem0 = Identifier()
            val typeCodeableConcept0 = CodeableConcept()
            val codingList0 = ArrayList<Coding>()
            val coding0 = Coding()
            coding0.system = "system-creation"
            coding0.code = "system_creation"
            coding0.display = "System Creation"
            codingList0.add(coding0)
            typeCodeableConcept0.coding = codingList0
            typeCodeableConcept0.text = FormatterClass().formatCurrentDateTime(Date())

            identifierSystem0.value = FormatterClass().formatCurrentDateTime(Date())
            identifierSystem0.system = "system-creation"
            identifierSystem0.type = typeCodeableConcept0

            val patientId = generateUuid()
            val subjectReference = Reference("Patient/$patientId")

            val jsonObject = JSONObject(questionnaireResponseString)
            val extractedAnswers = extractStructuredAnswersOnlyFromItems(jsonObject)


            val reasonCode = FormatterClass().getSharedPref(
                "currentCase",
                context
            )
            var patient = Patient()
            patient.id = patientId

            val qh = QuestionnaireHelper()
            val encounterId = generateUuid()
            val enc = qh.generalEncounter(null, encounterId)
            enc.id = encounterId
            enc.subject = subjectReference
            enc.reasonCodeFirstRep.codingFirstRep.code = "$reasonCode"
            enc.identifier.add(identifierSystem0)


            var case = "case-info"
            if (reasonCode != null) {
                case = reasonCode.toSlug()

            }

            val codeableConcept = CodeableConcept()
            codeableConcept.codingFirstRep.code = "case-information"
            codeableConcept.codingFirstRep.display = "case-information"
            codeableConcept.codingFirstRep.system = "case-information"
            codeableConcept.text = "case-information"
            enc.addReasonCode(codeableConcept)

            var pfirstName: String? = null
            var psecondName: String? = null
            var potherNames: List<String> = emptyList()

            val encounterReference = Reference("Encounter/$encounterId")

            val measure = MeasureReport()

            when (case) {

                "social-listening-and-rumor-tracking-tool" -> {

                    val subCountyEntry = extractedAnswers.find { it.linkId == "a3-sub-county" }
                    val countyEntry = extractedAnswers.find { it.linkId == "a4-county" }
                    var county = ""
                    var subCounty = ""
                    val currentYear = LocalDate.now().year

                    if (subCountyEntry != null) {
                        subCounty = subCountyEntry.answer
                        patient.addressFirstRep.state = subCounty
                        patient.addressFirstRep.addLine(subCounty)
                    }
                    if (countyEntry != null) {
                        county = countyEntry.answer
                        patient.addressFirstRep.city = county
                        patient.addressFirstRep.addLine(county)
                    }

                    val countyCode = county.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()


                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-RTT-"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference)
                }

                "measles-case-information" -> {
                    val genderEntry = extractedAnswers.find { it.linkId == "929966324957" }
                    val dobEntry = extractedAnswers.find { it.linkId == "257830485990" }
                    val parentEntry = extractedAnswers.find { it.linkId == "parent" }
                    val residenceEntry = extractedAnswers.find { it.linkId == "242811643559" }
                    val pNeighborEntry = extractedAnswers.find { it.linkId == "946232932304" }
                    val pStreetEntry = extractedAnswers.find { it.linkId == "424111786438" }
                    val pTownEntry = extractedAnswers.find { it.linkId == "110761799063" }
                    val pSubCountyEntry = extractedAnswers.find { it.linkId == "885995384353" }
                    val pCountyEntry = extractedAnswers.find { it.linkId == "301322368614" }
                    val pPhoneEntry = extractedAnswers.find { it.linkId == "754217593839" }
                    val patientFNameEntry = extractedAnswers.find { it.linkId == "873240407472" }
                    val patientMNameEntry = extractedAnswers.find { it.linkId == "246751846436" }
                    val patientLNameEntry = extractedAnswers.find { it.linkId == "486402457213" }
                    val subCountyEntry = extractedAnswers.find { it.linkId == "a3-sub-county" }
                    val countyEntry = extractedAnswers.find { it.linkId == "a4-county" }
                    val linkedEntry = extractedAnswers.find { it.linkId == "865158268604" }

                    if (patientLNameEntry != null) {
                        patient.nameFirstRep.family = patientLNameEntry.answer
                    }

                    if (patientFNameEntry != null) {
                        patient.nameFirstRep.addGiven(patientFNameEntry.answer)
                    }

                    if (patientMNameEntry != null) {
                        patient.nameFirstRep.addGiven(patientMNameEntry.answer)
                    }

                    parentEntry?.answer?.let { fullName ->
                        val parts = fullName.trim().split("\\s+".toRegex())
                        when (parts.size) {
                            1 -> {
                                pfirstName = parts[0]
                            }

                            2 -> {
                                pfirstName = parts[0]
                                psecondName = parts[1]
                            }

                            else -> {
                                pfirstName = parts[0]
                                psecondName = parts[1]
                                potherNames = parts.drop(2)
                            }
                        }
                    }

                    if (genderEntry != null) {
                        val gender = when (genderEntry.answer) {
                            "Male" -> Enumerations.AdministrativeGender.MALE
                            "Female" -> Enumerations.AdministrativeGender.FEMALE
                            else -> Enumerations.AdministrativeGender.UNKNOWN
                        }
                        patient.gender = gender
                    }

                    val parentPhone = ContactPoint()
                    if (pPhoneEntry != null) {

                        parentPhone.value = pPhoneEntry.answer
                        parentPhone.system = ContactPoint.ContactPointSystem.PHONE
                        parentPhone.use = ContactPoint.ContactPointUse.MOBILE
                    }
                    val parentAddress = Address()

                    if (residenceEntry != null) {
                        parentAddress.addLine(residenceEntry.answer)
                    }
                    if (pNeighborEntry != null) {
                        parentAddress.addLine(pNeighborEntry.answer)
                    }
                    if (pStreetEntry != null) {
                        parentAddress.addLine(pStreetEntry.answer)
                    }
                    if (pTownEntry != null) {
                        parentAddress.addLine(pTownEntry.answer)
                    }
                    if (pSubCountyEntry != null) {
                        parentAddress.addLine(pSubCountyEntry.answer)
                    }
                    if (pCountyEntry != null) {
                        parentAddress.addLine(pCountyEntry.answer)
                    }


                    val parentName = HumanName()
                    if (pfirstName != null) {
                        parentName.family = pfirstName

                    }
                    if (psecondName != null) {
                        parentName.addGiven(psecondName)
                    }
                    if (potherNames.isNotEmpty()) {
                        potherNames.forEach {
                            parentName.addGiven(it)
                        }
                    }

                    patient.contactFirstRep.name = parentName
                    patient.contactFirstRep.address = parentAddress
                    patient.contactFirstRep.addTelecom(parentPhone)


                    var county = ""
                    var subCounty = ""
                    val currentYear = LocalDate.now().year

                    if (subCountyEntry != null) {
                        subCounty = subCountyEntry.answer
                        patient.addressFirstRep.state = subCounty
                        patient.addressFirstRep.addLine(subCounty)
                    }
                    if (countyEntry != null) {
                        county = countyEntry.answer
                        patient.addressFirstRep.city = county
                        patient.addressFirstRep.addLine(county)
                    }

                    val countyCode = county.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()
                    var linked = "MEA-"

                    if (linkedEntry != null) {
                        linked = when (linkedEntry.answer.lowercase()) {
                            "yes" -> "MEA-L"
                            else -> "MEA-"
                        }
                    }

                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-$linked"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference)

                    try {
                        if (dobEntry != null) {
                            patient.birthDate =
                                SimpleDateFormat("yyyy-MM-dd").parse(dobEntry.answer)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Define specimen types and their corresponding linkIds
                    val specimenConfigs = mutableListOf<SpecimenConfig>(
                        SpecimenConfig("Blood", "918495737998", "8962468583341"),
                        SpecimenConfig("Urine", "433195098993", "915783129731"),
                        SpecimenConfig("Respiratory Sample", "270749570400", "183705125522"),
                    )

                    val otherSpecimenEntry = extractedAnswers.find { it.linkId == "258912872921" }
                    if (otherSpecimenEntry != null) {

                        if (otherSpecimenEntry.answer.lowercase() == "yes") {
                            val otherSpecifyEntry =
                                extractedAnswers.find { it.linkId == "340507649387" }
                            if (otherSpecifyEntry != null) {
                                val otherDateEntry =
                                    extractedAnswers.find { it.linkId == "699353598445" }
                                if (otherDateEntry != null) {
                                    createSpecimenResource(
                                        otherSpecifyEntry.linkId,
                                        otherDateEntry.answer,
                                        otherSpecifyEntry.answer,
                                        subjectReference
                                    )
                                }
                            }
                        }
                    }

                    for (config in specimenConfigs) {
                        val specimenEntry =
                            extractedAnswers.find { it.linkId == config.entryLinkId }
                        if (specimenEntry?.answer?.lowercase() == "yes") {
                            val dateEntry = extractedAnswers.find { it.linkId == config.dateLinkId }
                            if (dateEntry != null) {
                                createSpecimenResource(
                                    specimenEntry.linkId,
                                    dateEntry.answer,
                                    config.type,
                                    subjectReference
                                )
                            }
                        }
                    }


                }

                "afp-case-information" -> {
                    val fNameEntry = extractedAnswers.find { it.linkId == "873240407472" }
                    val mNameEntry = extractedAnswers.find { it.linkId == "246751846436" }
                    val lNameEntry = extractedAnswers.find { it.linkId == "486402457213" }
                    val genderEntry = extractedAnswers.find { it.linkId == "929966324957" }
                    val dobEntry = extractedAnswers.find { it.linkId == "257830485990" }
                    val subCountyEntry = extractedAnswers.find { it.linkId == "a3-sub-county" }
                    val countyEntry = extractedAnswers.find { it.linkId == "a4-county" }
                    val specimenDateEntry = extractedAnswers.find { it.linkId == "737703942433" }


                    if (genderEntry != null) {
                        val gender = when (genderEntry.answer.lowercase()) {
                            "male" -> Enumerations.AdministrativeGender.MALE
                            "female" -> Enumerations.AdministrativeGender.FEMALE
                            else -> Enumerations.AdministrativeGender.UNKNOWN
                        }
                        patient.gender = gender
                    }
                    if (fNameEntry != null) {
                        patient.nameFirstRep.family = fNameEntry.answer
                    }
                    if (mNameEntry != null) {
                        patient.nameFirstRep.addGiven(mNameEntry.answer)
                    }
                    if (lNameEntry != null) {
                        patient.nameFirstRep.addGiven(lNameEntry.answer)
                    }
                    val guardianEntry = extractedAnswers.find { it.linkId == "856448027666" }
                    val fullName = guardianEntry?.answer?.trim().orEmpty()
                    val parts = fullName.split("\\s+".toRegex()).filter { it.isNotBlank() }

                    val parentName = HumanName()
                    when {
                        parts.isEmpty() -> {

                        }

                        parts.size == 1 -> {
                            parentName.family = parts[0]
                        }

                        else -> {
                            parentName.family = parts[0]
                            parentName.addGiven(parts.drop(1).joinToString(" "))
                        }
                    }
                    patient.contactFirstRep.name = parentName
                    val phoneEntry = extractedAnswers.find { it.linkId == "576318206363" }
                    val parentPhone = ContactPoint()
                    if (phoneEntry != null) {

                        parentPhone.value = phoneEntry.answer
                        parentPhone.system = ContactPoint.ContactPointSystem.PHONE
                        parentPhone.use = ContactPoint.ContactPointUse.MOBILE
                    }
                    val parentAddress = Address()
                    patient.contactFirstRep.address = parentAddress
                    patient.contactFirstRep.addTelecom(parentPhone)
                    var county = ""
                    var subCounty = ""
                    val currentYear = LocalDate.now().year

                    if (subCountyEntry != null) {
                        subCounty = subCountyEntry.answer
                        patient.addressFirstRep.state = subCounty
                        patient.addressFirstRep.addLine(subCounty)
                    }
                    if (countyEntry != null) {
                        county = countyEntry.answer
                        patient.addressFirstRep.city = county
                        patient.addressFirstRep.addLine(county)
                    }

                    val countyCode = county.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()


                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-AFP-"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference)


                    if (specimenDateEntry != null) {

                        createSpecimenResource(
                            specimenDateEntry.linkId,
                            specimenDateEntry.answer,
                            "Stool",
                            subjectReference
                        )
                    }


                    try {
                        if (dobEntry != null) {
                            patient.birthDate =
                                SimpleDateFormat("yyyy-MM-dd").parse(dobEntry.answer)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }


                }


                "vl-case-information" -> {
                    val fNameEntry = extractedAnswers.find { it.linkId == "817903655885" }
                    val mNameEntry = extractedAnswers.find { it.linkId == "164840483828" }
                    val lNameEntry = extractedAnswers.find { it.linkId == "606848143908" }
                    val genderEntry = extractedAnswers.find { it.linkId == "543806612685" }
                    val dobEntry = extractedAnswers.find { it.linkId == "257830485990" }
                    val phoneEntry = extractedAnswers.find { it.linkId == "760016167907" }
                    val contactNameEntry = extractedAnswers.find { it.linkId == "657999955440" }
                    val contactPhoneEntry = extractedAnswers.find { it.linkId == "354738003178" }

                    val casePhone = ContactPoint()
                    val parentPhone = ContactPoint()
                    if (phoneEntry != null) {
                        casePhone.value = phoneEntry.answer
                        casePhone.system = ContactPoint.ContactPointSystem.PHONE
                        casePhone.use = ContactPoint.ContactPointUse.MOBILE
                        patient.addTelecom(parentPhone)
                    }
                    if (contactPhoneEntry != null) {
                        parentPhone.value = contactPhoneEntry.answer
                        parentPhone.system = ContactPoint.ContactPointSystem.PHONE
                        parentPhone.use = ContactPoint.ContactPointUse.MOBILE
                        patient.contactFirstRep.addTelecom(parentPhone)
                    }

                    if (genderEntry != null) {
                        val gender = when (genderEntry.answer.lowercase()) {
                            "male" -> Enumerations.AdministrativeGender.MALE
                            "female" -> Enumerations.AdministrativeGender.FEMALE
                            else -> Enumerations.AdministrativeGender.UNKNOWN
                        }
                        patient.gender = gender
                    }
                    if (fNameEntry != null) {
                        patient.nameFirstRep.family = fNameEntry.answer
                    }
                    if (mNameEntry != null) {
                        patient.nameFirstRep.addGiven(mNameEntry.answer)
                    }
                    if (lNameEntry != null) {
                        patient.nameFirstRep.addGiven(lNameEntry.answer)
                    }

                    val subCountyEntry = extractedAnswers.find { it.linkId == "a4-county" }
                    val countyEntry = extractedAnswers.find { it.linkId == "a3-sub-county" }
                    var county = ""
                    var subCounty = ""
                    val currentYear = LocalDate.now().year

                    if (subCountyEntry != null) {
                        subCounty = subCountyEntry.answer
                        patient.addressFirstRep.state = subCounty
                        patient.addressFirstRep.addLine(subCounty)
                    }
                    if (countyEntry != null) {
                        county = countyEntry.answer
                        patient.addressFirstRep.city = county
                        patient.addressFirstRep.addLine(county)
                    }

                    val countyCode = county.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()


                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-VL-"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference)
                    try {
                        if (dobEntry != null) {
                            patient.birthDate =
                                SimpleDateFormat("yyyy-MM-dd").parse(dobEntry.answer)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val fullName = contactNameEntry?.answer?.trim().orEmpty()
                    val parts = fullName.split("\\s+".toRegex()).filter { it.isNotBlank() }

                    val parentName = HumanName()
                    when {
                        parts.isEmpty() -> {

                        }

                        parts.size == 1 -> {
                            parentName.family = parts[0]
                        }

                        else -> {
                            parentName.family = parts[0]
                            parentName.addGiven(parts.drop(1).joinToString(" "))
                        }
                    }
                    patient.contactFirstRep.name = parentName
                }

                "moh-505-reporting-form" -> {

                    patient.nameFirstRep.family = "MOH-505"
                    patient.nameFirstRep.addGiven("MOH-505")

                    val subCountyEntry = extractedAnswers.find { it.linkId == "a3-sub-county" }
                    val countyEntry = extractedAnswers.find { it.linkId == "a4-county" }

                    measure.id = generateUuid()
                    measure.subject = subjectReference
                    measure.status = MeasureReport.MeasureReportStatus.COMPLETE
                    measure.type = MeasureReport.MeasureReportType.SUMMARY


                    var county = ""
                    var subCounty = ""
                    val currentYear = LocalDate.now().year

                    if (subCountyEntry != null) {
                        subCounty = subCountyEntry.answer
                        patient.addressFirstRep.state = subCounty
                        patient.addressFirstRep.addLine(subCounty)
                    }
                    if (countyEntry != null) {
                        county = countyEntry.answer
                        patient.addressFirstRep.city = county
                        patient.addressFirstRep.addLine(county)
                    }

                    val countyCode = county.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()
                    var linked = "MOH-505-"
                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-$linked"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference)

                }

                "mpox-tally-sheet" -> {

                    patient.nameFirstRep.family = "Mpox-Tally"
                    patient.nameFirstRep.addGiven("Mpox-Tally")

                    val subCountyEntry = extractedAnswers.find { it.linkId == "819946803642" }
                    val countyEntry = extractedAnswers.find { it.linkId == "294367770999" }

                    measure.id = generateUuid()
                    measure.subject = subjectReference
                    measure.status = MeasureReport.MeasureReportStatus.COMPLETE
                    measure.type = MeasureReport.MeasureReportType.SUMMARY


                    var county = ""
                    var subCounty = ""
                    val currentYear = LocalDate.now().year

                    if (subCountyEntry != null) {
                        subCounty = subCountyEntry.answer
                        patient.addressFirstRep.state = subCounty
                        patient.addressFirstRep.addLine(subCounty)
                    }
                    if (countyEntry != null) {
                        county = countyEntry.answer
                        patient.addressFirstRep.city = county
                        patient.addressFirstRep.addLine(county)
                    }

                    val countyCode = county.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()
                    var linked = "Mpox-"
                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-$linked"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference)

                }

            }
            withContext(Dispatchers.IO) {
                try {
                    val identifierSystem = Identifier()
                    val typeCodeableConcept = CodeableConcept()
                    val codingList = ArrayList<Coding>()
                    val coding = Coding()
                    coding.system = case
                    coding.code = case
                    coding.display = case
                    codingList.add(coding)
                    typeCodeableConcept.coding = codingList
                    typeCodeableConcept.text = encounterId

                    identifierSystem.value = encounterId
                    identifierSystem.system = case
                    identifierSystem.type = typeCodeableConcept


                    patient.identifier.add(identifierSystem0)
                    patient.identifier.add(identifierSystem)
                    fhirEngine.create(patient)
                    fhirEngine.create(enc)

                    extractedAnswers.forEach {

                        val measureCodeableConcept = CodeableConcept()
                        measureCodeableConcept.codingFirstRep.code = it.linkId
                        measureCodeableConcept.codingFirstRep.display = it.text
                        measureCodeableConcept.codingFirstRep.system = "questionnaire-answers"
                        measureCodeableConcept.text = it.text

                        val compo = MeasureReportGroupPopulationComponent()
                        compo.code = measureCodeableConcept
                        try {
                            compo.count = it.answer.toInt()
                        } catch (e: Exception) {
                            compo.count = 0
                        }

                        if (it.linkId == WEEK_ENDING_DATE) {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .parse(it.answer)
                            if (date != null) {
                                measure.date = date
                            }

                        }
                        compo.id = it.linkId
                        // check if the linkId is not in the excluded list and add to measure

                        when (case) {
                            "mpox-tally-sheet" -> {
                                if (!ALL_MPOX_LINK_IDS.contains(it.linkId)) {
                                    measure.groupFirstRep.addPopulation(compo)
                                }
                            }

                            else -> {
                                if (!ALL_LINK_IDS.contains(it.linkId)) {
                                    measure.groupFirstRep.addPopulation(compo)
                                }
                            }
                        }
                        val obs = qh.codingQuestionnaire(
                            it.linkId, it.text,
                            it.answer
                        )

                        createResource(obs, subjectReference, encounterReference)
                    }
                    when (case) {
                        "moh-505-reporting-form" -> {
                            fhirEngine.create(measure)
                        }

                        "mpox-tally-sheet" -> {
                            val latitude = FormatterClass().getSharedPref("latitude", context)
                            val longitude = FormatterClass().getSharedPref("longitude", context)

                            measure.addIdentifier(
                                QuestionnaireHelper().createFullFhirIdentifier(
                                    codeData = "geo-location",
                                    valueData = "lat:${latitude},lon:${longitude}",
                                    systemData = "geo-location-details",
                                    displayData = "Latitude: $latitude, Longitude: $longitude"
                                )
                            )
                            fhirEngine.create(measure)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TAG", "Error experienced ${e.message}}")
                }

                withContext(Dispatchers.Main) { isPatientSaved.value = true }
            }
        }
    }

    private suspend fun createSpecimenResource(
        linkId: String,
        dateAnswer: String,
        string: String,
        subjectReference: Reference
    ) {
        val specimenCoding = Coding()
        specimenCoding.code = linkId
        specimenCoding.system = "specimen-details"
        specimenCoding.display = string

        val specimenType = CodeableConcept()
        specimenType.text = string
        specimenType.addCoding(specimenCoding)

        try {
            val dateOnly =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                    dateAnswer
                )

            val calendar = Calendar.getInstance()
            calendar.time = dateOnly!!
            val now = Calendar.getInstance()
            calendar.set(
                Calendar.HOUR_OF_DAY,
                now.get(Calendar.HOUR_OF_DAY)
            )
            calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, now.get(Calendar.SECOND))
            calendar.set(
                Calendar.MILLISECOND,
                now.get(Calendar.MILLISECOND)
            )


            val dateTime = DateTimeType()
            dateTime.value = calendar.time

            val collection = Specimen.SpecimenCollectionComponent()
            collection.setCollected(dateTime)

            val specimen = Specimen()
            specimen.subject = subjectReference
            specimen.type = specimenType
            specimen.collection = collection
            fhirEngine.create(specimen)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun extractStructuredAnswersOnlyFromItems(json: JSONObject): List<QuestionnaireAnswer> {
        val results = mutableListOf<QuestionnaireAnswer>()

        fun processItems(items: JSONArray) {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val linkId = item.optString("linkId", "")
                val text = item.optString("text", "")

                if (item.has("answer")) {
                    val answers = item.getJSONArray("answer")
                    val valueList = mutableListOf<String>()

                    for (j in 0 until answers.length()) {
                        val answerObj = answers.getJSONObject(j)

                        val value = when {
                            answerObj.has("valueString") -> answerObj.getString("valueString")
                            answerObj.has("valueInteger") -> answerObj.optString("valueInteger", "")
                            answerObj.has("valueDate") -> answerObj.optString("valueDate", "")
                            answerObj.has("valueDateTime") -> answerObj.optString(
                                "valueDateTime",
                                ""
                            )

                            answerObj.has("valueBoolean") -> answerObj.optString("valueBoolean", "")
                            answerObj.has("valueDecimal") -> answerObj.optString("valueDecimal", "")
                            answerObj.has("valueCoding") -> {
                                val coding = answerObj.getJSONObject("valueCoding")
                                coding.optString("display", coding.optString("code", ""))
                            }

                            answerObj.has("valueReference") -> {
                                val ref = answerObj.getJSONObject("valueReference")
                                ref.optString("display", ref.optString("reference", ""))
                            }

                            else -> null
                        }

                        if (!value.isNullOrBlank()) {
                            valueList.add(value)
                        }
                    }

                    if (valueList.isNotEmpty()) {
                        // Join multiple values with comma
                        results.add(QuestionnaireAnswer(linkId, text, valueList.joinToString(", ")))
                    }
                }

                if (item.has("item")) {
                    processItems(item.getJSONArray("item"))
                }
            }
        }

        if (json.has("item")) {
            processItems(json.getJSONArray("item"))
        }

        return results
    }



    private fun String.toSlug(): String {
        return this
            .trim() // remove leading/trailing spaces
            .lowercase() // make all lowercase
            .replace("[^a-z0-9\\s-]".toRegex(), "") // remove special characters
            .replace("\\s+".toRegex(), "-") // replace spaces with hyphens
            .replace("-+".toRegex(), "-") // collapse multiple hyphens
    }

    fun extractStructuredAnswers(response: JSONObject): List<QuestionnaireAnswer> {
        val results = mutableListOf<QuestionnaireAnswer>()

        fun extractFromItems(items: JSONArray?) {
            if (items == null) return

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val linkId = item.optString("linkId", "N/A")
                val text = item.optString("text", "N/A")

                if (item.has("answer")) {
                    val answerArray = item.getJSONArray("answer")
                    for (j in 0 until answerArray.length()) {
                        val answer = answerArray.getJSONObject(j)

                        val value = when {
                            answer.has("valueString") -> answer.optString("valueString", "")
                            answer.has("valueInteger") -> answer.optString("valueInteger", "")
                            answer.has("valueDate") -> answer.optString("valueDate", "")
                            answer.has("valueDateTime") -> answer.optString("valueDateTime", "")
                            answer.has("valueBoolean") -> answer.optString("valueBoolean", "")
                            answer.has("valueDecimal") -> answer.optString("valueDecimal", "")
                            answer.has("valueCoding") -> {
                                val coding = answer.getJSONObject("valueCoding")
                                coding.optString("display", coding.optString("code", ""))
                            }

                            else -> null
                        }

                        if (value != null && value.isNotBlank()) {
                            results.add(QuestionnaireAnswer(linkId, text, value))
                        } else {
                            println("Skipped: linkId=$linkId, text=$text, value=$value")
                        }
                    }
                }

                // Recurse only if there are nested questionnaire items
                if (item.has("item")) {
                    extractFromItems(item.getJSONArray("item"))
                }
            }
        }

        extractFromItems(response.optJSONArray("item"))
        return results
    }

    fun extractStructuredAnswersOld(response: JSONObject): List<QuestionnaireAnswer> {
        val results = mutableListOf<QuestionnaireAnswer>()

        fun extractFromItems(items: JSONArray?) {
            if (items == null) return

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val linkId = item.optString("linkId", "N/A")
                val text = item.optString("text", "N/A")

                // Extract answers
                if (item.has("answer")) {
                    val answerArray = item.getJSONArray("answer")
                    for (j in 0 until answerArray.length()) {
                        val answer = answerArray.getJSONObject(j)
                        val value = when {
                            answer.has("valueInteger") -> answer.getString("valueInteger")
                            answer.has("valueString") -> answer.getString("valueString")
                            answer.has("valueDate") -> answer.getString("valueDate")
                            answer.has("valueDateTime") -> answer.getString("valueDateTime")
                            answer.has("valueCoding") -> {
                                val coding = answer.getJSONObject("valueCoding")
                                coding.optString("display", coding.optString("code", ""))
                            }

                            else -> "Unsupported answer type"
                        }
                        results.add(QuestionnaireAnswer(linkId, text, value))
                    }
                }

                // Recurse into nested items
                if (item.has("item")) {
                    extractFromItems(item.getJSONArray("item"))
                }
            }
        }

        extractFromItems(response.optJSONArray("item"))
        return results
    }


    private suspend fun createResource(
        obs: Observation,
        subjectReference: Reference,
        encounterReference: Reference
    ) {
        try {
            obs.id = generateUuid()
            obs.subject = subjectReference
            obs.encounter = encounterReference
            obs.issued = Date()
            fhirEngine.create(obs)

            println("Observation created: ${obs.id}")
        } catch (e: Exception) {
            Log.e("SavePatient", "Error saving patient", e)
        }
    }

    private fun extractResponseCode(obj: JSONObject, key: String): String {
        return try {
            val answer = obj.getJSONArray("answer").getJSONObject(0)
            val coding = answer.getJSONObject(key)
            coding.optString("display", coding.optString("code", ""))
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractResponse(obj: JSONObject, key: String): String {
        return try {
            val answer = obj.getJSONArray("answer").getJSONObject(0)
            answer.optString(key, "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun fetchQuestionnaireJson(): String {
        _questionnaireJson?.let {
            return it
        }
        _questionnaireJson =
            getApplication<Application>().assets.open(state[QUESTIONNAIRE_FILE_PATH_KEY]!!)
                .bufferedReader().use { it.readText() }
        return _questionnaireJson!!
    }

    private fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }
}
