package com.icl.demo.viewmodels


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
import com.google.android.fhir.search.search
import com.ibm.icu.text.SimpleDateFormat
import com.icl.demo.FhirApplication
import com.icl.demo.forms.ParentActivity.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.demo.models.SpecimenConfig
import com.icl.demo.utils.Constants.ALL_LINK_IDS
import com.icl.demo.utils.Constants.ALL_MPOX_LINK_IDS
import com.icl.demo.utils.Constants.WEEK_ENDING_DATE
import com.icl.demo.utils.FormatterClass
import com.icl.demo.utils.QuestionnaireHelper
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
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Specimen
import org.hl7.fhir.r4.model.StringType
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale


class AddCaseViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {

    private var _questionnaireJson: String? = null
    val questionnaireJson: String
        get() = fetchQuestionnaireJson()

    val isPatientSaved = MutableLiveData<Boolean>()

    private val questionnaire: Questionnaire
        get() = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            .parseResource(questionnaireJson) as Questionnaire

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
                ).values.flatten().any { it is Invalid }
            ) {
                isPatientSaved.value = false
                return@launch
            }

            withContext(Dispatchers.IO) {

                val latitude = FormatterClass().getSharedPref("latitude", context)
                val longitude = FormatterClass().getSharedPref("longitude", context)

                val locationIdentifier = QuestionnaireHelper().createFullFhirIdentifier(
                    codeData = "geo-location",
                    valueData = "lat:${latitude},lon:${longitude}",
                    systemData = "geo-location-details",
                    displayData = "Latitude: $latitude, Longitude: $longitude"
                )

                val responseType = QuestionnaireHelper().createFullFhirIdentifier(
                    codeData = "supervisor_checklist",
                    valueData = "supervisor_checklist",
                    systemData = "supervisor_checklist",
                    displayData = "Supervisor Checklist"
                )

                questionnaireResponse.identifier = locationIdentifier
                val extension = Extension().apply {
                    url = "supervisor_checklist"
                    setValue(responseType)
                }
                val facility = FormatterClass().getSharedPref("facility", context)
                if (facility != null) {
                    questionnaireResponse.addExtension(
                        sourceExtension(
                            "questionnaire",
                            facility,
                            context
                        )
                    )
                }
                fhirEngine.create(questionnaireResponse)
            }

            withContext(Dispatchers.Main) { isPatientSaved.value = true }

        }

    }

    // Returns a NEW QuestionnaireResponse (immutable approach)
    private fun populateReportingSiteAnswers(
        originalResponse: QuestionnaireResponse,
        context: Context
    ): QuestionnaireResponse {
        // Create a deep copy first
        val updatedResponse = originalResponse.copy()

        val reportingSiteGroup = updatedResponse.item.firstOrNull { item ->
            item.linkId == "151479012557" && item.text == "Reporting Site"
        }

        // Modify the copy
        reportingSiteGroup?.item?.forEach { question ->
            question.answer.clear()
            when (question.linkId) {

                "294367770999" -> question.addAnswer().apply {
                    value = StringType(FormatterClass().getSharedPref("countyName", context))
                }

                "819946803642" -> question.addAnswer().apply {
                    value = StringType(FormatterClass().getSharedPref("subCountyName", context))
                }

                "819943434" -> question.addAnswer().apply {
                    value = StringType(FormatterClass().getSharedPref("wardName", context))
                }

                "819946803677" -> question.addAnswer().apply {
                    value = StringType(FormatterClass().getSharedPref("facilityName", context))
                }

                "438862163919" -> question.addAnswer().apply {
                    value = StringType(FormatterClass().getSharedPref("countyName", context))
                }
            }
        }

        return updatedResponse // Return the modified copy
    }

    fun updatePatientData(
        updatedResponse: QuestionnaireResponse,
        context: Context,
        measureReport: MeasureReport
    ) {
        viewModelScope.launch {
            val qh = QuestionnaireHelper()
            val formatter = FormatterClass()
            val facility = formatter.getSharedPref("facility", context)
            val questionnaireResponse = populateReportingSiteAnswers(updatedResponse, context)
            if (QuestionnaireResponseValidator.validateQuestionnaireResponse(
                    questionnaire,
                    questionnaireResponse,
                    getApplication(),
                ).values.flatten().any { it is Invalid }
            ) {
                isPatientSaved.value = false
                return@launch
            }
            // Print the response to the log
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val questionnaireResponseString =
                jsonParser.encodeResourceToString(questionnaireResponse)

            if (facility != null) {
                questionnaireResponse.addExtension(
                    sourceExtension(
                        "questionnaire",
                        facility,
                        context
                    )
                )
                questionnaireResponse.meta = Meta().apply {
                    tag = listOf(
                        sourceMetaTag("questionnaire", facility, context)
                    )
                }
            }
            val idPart = FormatterClass().getSharedPref(
                "activeResponse", context
            )
            questionnaireResponse.id = idPart
            val practitionerId = formatter.getSharedPref("fhirPractitionerId", context)
            if (practitionerId != null) {
                questionnaireResponse.author = Reference("Practitioner/$practitionerId")
            }
            val jsonObject = JSONObject(questionnaireResponseString)
            val extractedAnswers =
                FormatterClass().extractStructuredAnswersOnlyFromItems(jsonObject)


            // Extract Patient & Encounter Ids
            val patientId = questionnaireResponse.subject.reference.split("/")[1]
            val encounterId = questionnaireResponse.encounter.reference.split("/")[1]

            val subjectReference = Reference("Patient/$patientId")
            val encounterReference = Reference("Encounter/$encounterId")

            withContext(Dispatchers.IO) {
                try {
                    val observations = fhirEngine.search<Observation> {
                        filter(
                            Observation.SUBJECT,
                            { value = "Patient/$patientId" })
                        filter(
                            Observation.ENCOUNTER,
                            { value = "Encounter/$encounterId" })

                    }.take(500)

                    observations.forEach { d ->
                        println("Existing Observation: ${d.resource.code.codingFirstRep.code}")
                    }


                    extractedAnswers.forEach {
                        val measureCodeableConcept = CodeableConcept()
                        measureCodeableConcept.codingFirstRep.code = it.linkId
                        measureCodeableConcept.codingFirstRep.display = it.text
                        measureCodeableConcept.codingFirstRep.system = "questionnaire-answers"
                        measureCodeableConcept.text = it.text

                        val compo = MeasureReportGroupPopulationComponent()
                        compo.code = measureCodeableConcept

                        if (it.linkId == WEEK_ENDING_DATE) {
                            val date =
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).parse(it.answer)
                            if (date != null) {
                                measureReport.date = date
                            }

                        }
                        compo.id = it.linkId

                        try {
                            compo.count = it.answer.toInt()
                        } catch (e: Exception) {
                            compo.count = 0
                        }
                        if (!ALL_LINK_IDS.contains(it.linkId)) {
                            measureReport.groupFirstRep.addPopulation(compo)
                        }
                        // Create new Observations
                        val obs = qh.codingQuestionnaire(
                            it.linkId, it.text, it.answer
                        )
                        createResource(obs, subjectReference, encounterReference, context)

                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            fhirEngine.update(questionnaireResponse)
            fhirEngine.update(measureReport)

            withContext(Dispatchers.Main) { isPatientSaved.value = true }
        }


    }

    fun savePatientData(
        updatedResponse: QuestionnaireResponse,
        context: Context
    ) {
        viewModelScope.launch {
            val questionnaireResponse = populateReportingSiteAnswers(updatedResponse, context)

            if (QuestionnaireResponseValidator.validateQuestionnaireResponse(
                    questionnaire,
                    questionnaireResponse,
                    getApplication(),
                ).values.flatten().any { it is Invalid }
            ) {
                isPatientSaved.value = false
                return@launch
            }
            // Print the response to the log
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val questionnaireResponseString =
                jsonParser.encodeResourceToString(questionnaireResponse)

            val identifierSystem0 = Identifier()
            val typeCodeableConcept0 = CodeableConcept()
            val codingList0 = ArrayList<Coding>()
            val coding0 = Coding()
            coding0.system = "system-creation"
            coding0.code = "system_creation"
            coding0.display = "System Creation"
            codingList0.add(coding0)
            typeCodeableConcept0.coding = codingList0
            typeCodeableConcept0.text = FormatterClass().formatDateTime(Date())

            identifierSystem0.value = FormatterClass().formatDateTime(Date())
            identifierSystem0.system = "system-creation"
            identifierSystem0.type = typeCodeableConcept0

            val patientId = generateUuid()
            val subjectReference = Reference("Patient/$patientId")
            val formatter = FormatterClass()

            val facility = formatter.getSharedPref("facility", context)

            val jsonObject = JSONObject(questionnaireResponseString)
            val extractedAnswers =
                FormatterClass().extractStructuredAnswersOnlyFromItems(jsonObject)


            val reasonCode = FormatterClass().getSharedPref(
                "currentCase", context
            )
            val patient = Patient()
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
            if (facility != null) {
                patient.addExtension(
                    sourceExtension("patient", facility, context)
                )
                patient.meta = Meta().apply {
                    tag = listOf(
                        sourceMetaTag("patient", facility, context)
                    )
                }
                enc.addExtension(sourceExtension("encounter", facility, context))
                enc.meta = Meta().apply {
                    tag = listOf(
                        sourceMetaTag("encounter", facility, context)
                    )
                }
                measure.addExtension(
                    sourceExtension(
                        "measure",
                        facility,
                        context
                    )
                )
                measure.meta = Meta().apply {
                    tag = listOf(
                        sourceMetaTag("measure", facility, context)
                    )
                }
                questionnaireResponse.addExtension(
                    sourceExtension(
                        "questionnaire",
                        facility,
                        context
                    )
                )
                questionnaireResponse.meta = Meta().apply {
                    tag = listOf(
                        sourceMetaTag("questionnaire", facility, context)
                    )
                }
            }
            val practitionerId = formatter.getSharedPref("fhirPractitionerId", context)
            if (practitionerId != null) {
                questionnaireResponse.author = Reference("Practitioner/$practitionerId")
                measure.reporter = Reference("Practitioner/$practitionerId")
                patient.generalPractitionerFirstRep.reference = "Practitioner/$practitionerId"
                enc.participantFirstRep.individual = Reference("Practitioner/$practitionerId")
            }

            when (case) {

                "mpox-register" -> {
                    val patientFNameEntry =
                        extractedAnswers.find { it.linkId == "873240407472" }
                    val patientMNameEntry =
                        extractedAnswers.find { it.linkId == "246751846436" }
                    val patientLNameEntry =
                        extractedAnswers.find { it.linkId == "486402457213" }
                    if (patientLNameEntry != null) {
                        patient.nameFirstRep.family = patientLNameEntry.answer
                    }

                    if (patientFNameEntry != null) {
                        patient.nameFirstRep.addGiven(patientFNameEntry.answer)
                    }

                    if (patientMNameEntry != null) {
                        patient.nameFirstRep.addGiven(patientMNameEntry.answer)
                    }
                    val dobEntry = extractedAnswers.find { it.linkId == "257830485990" }
                    val genderEntry = extractedAnswers.find { it.linkId == "929966324957" }
                    val subCountyEntry = extractedAnswers.find { it.linkId == "a3-sub-county" }
                    val centerEntry =
                        extractedAnswers.find { it.linkId == "vaccination_center" }
                    val countyEntry = extractedAnswers.find { it.linkId == "a4-county" }
                    var county = ""
                    var subCounty = ""
                    var center = ""
                    val currentYear = LocalDate.now().year
                    if (centerEntry != null) {
                        center = centerEntry.answer
                    }
                    if (dobEntry != null) {
                        try {
                            patient.birthDate =
                                SimpleDateFormat("yyyy-MM-dd").parse(dobEntry.answer)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

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
                    if (genderEntry != null) {
                        val gender = when (genderEntry.answer.lowercase()) {
                            "male" -> Enumerations.AdministrativeGender.MALE
                            "female" -> Enumerations.AdministrativeGender.FEMALE
                            else -> Enumerations.AdministrativeGender.UNKNOWN
                        }
                        patient.gender = gender
                    }
                    val originEntry = extractedAnswers.find { it.linkId == "country_of_origin" }
                    val pPhoneEntry = extractedAnswers.find { it.linkId == "754217593839" }
                    val parentPhone = ContactPoint()
                    if (pPhoneEntry != null) {

                        parentPhone.value = pPhoneEntry.answer
                        parentPhone.system = ContactPoint.ContactPointSystem.PHONE
                        parentPhone.use = ContactPoint.ContactPointUse.MOBILE
                    }

                    val epid = if (originEntry != null) {
                        val countryCode = originEntry.answer.padEnd(3, 'X').take(3).uppercase()
                        "$countryCode-${
                            center.padEnd(3, 'X').take(3).uppercase()
                        }-$currentYear-MPOVAC-"
                    } else {
                        val countyCode =
                            FormatterClass().generateInitials(county)// county.padEnd(3, 'X').take(3).uppercase()
                        val subCountyCode =
                            FormatterClass().generateInitials(subCounty)//.padEnd(3, 'X').take(3).uppercase()
                        "KEN-$countyCode-$subCountyCode-$currentYear-MPOVAC-"
                    }

                    patient.addTelecom(parentPhone)


                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference, context)
                }

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

                    val countyCode =
                        FormatterClass().generateInitials(county)///padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode =
                        FormatterClass().generateInitials(subCounty)//.padEnd(3, 'X').take(3).uppercase()


                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-RTT-"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference, context)
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
                    val patientFNameEntry =
                        extractedAnswers.find { it.linkId == "873240407472" }
                    val patientMNameEntry =
                        extractedAnswers.find { it.linkId == "246751846436" }
                    val patientLNameEntry =
                        extractedAnswers.find { it.linkId == "486402457213" }
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

                    val countyCode =
                        FormatterClass().generateInitials(county)//.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode =
                        FormatterClass().generateInitials(subCounty)//.padEnd(3, 'X').take(3).uppercase()
                    var linked = "MEA-"

                    if (linkedEntry != null) {
                        linked = when (linkedEntry.answer.lowercase()) {
                            "yes" -> "MEA-L"
                            else -> "MEA-"
                        }
                    }

                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-$linked"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference, context)

                    try {
                        if (dobEntry != null) {
                            patient.birthDate =
                                SimpleDateFormat("yyyy-MM-dd").parse(dobEntry.answer)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Define specimen types and their corresponding linkIds
                    val specimenConfigs = mutableListOf(
                        SpecimenConfig("Blood", "918495737998", "8962468583341"),
                        SpecimenConfig("Urine", "433195098993", "915783129731"),
                        SpecimenConfig("Respiratory Sample", "270749570400", "183705125522"),
                    )

                    val otherSpecimenEntry =
                        extractedAnswers.find { it.linkId == "258912872921" }
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
                                        subjectReference, context
                                    )
                                }
                            }
                        }
                    }

                    for (config in specimenConfigs) {
                        val specimenEntry =
                            extractedAnswers.find { it.linkId == config.entryLinkId }
                        if (specimenEntry?.answer?.lowercase() == "yes") {
                            val dateEntry =
                                extractedAnswers.find { it.linkId == config.dateLinkId }
                            if (dateEntry != null) {
                                createSpecimenResource(
                                    specimenEntry.linkId,
                                    dateEntry.answer,
                                    config.type,
                                    subjectReference, context
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
                    val specimenDateEntry =
                        extractedAnswers.find { it.linkId == "737703942433" }


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

                    val countyCode =
                        FormatterClass().generateInitials(county)//.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode =
                        FormatterClass().generateInitials(subCounty)//.padEnd(3, 'X').take(3).uppercase()


                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-AFP-"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference, context)


                    if (specimenDateEntry != null) {

                        createSpecimenResource(
                            specimenDateEntry.linkId,
                            specimenDateEntry.answer,
                            "Stool",
                            subjectReference, context
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
                    val contactPhoneEntry =
                        extractedAnswers.find { it.linkId == "354738003178" }

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

                    val countyCode =
                        FormatterClass().generateInitials(county)//.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode =
                        FormatterClass().generateInitials(subCounty)//.padEnd(3, 'X').take(3).uppercase()


                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-VL-"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference, context)
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

                    val countyCode =
                        FormatterClass().generateInitials(county)//.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode =
                        FormatterClass().generateInitials(subCounty)//.padEnd(3, 'X').take(3).uppercase()
                    var linked = "MOH-505-"
                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-$linked"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference, context)

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

                    val countyCode =
                        FormatterClass().generateInitials(county)//.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode =
                        FormatterClass().generateInitials(subCounty)//.padEnd(3, 'X').take(3).uppercase()
                    val linked = "Mpox-"
                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-$linked"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference, context)

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
                    questionnaireResponse.id = generateUuid()
                    questionnaireResponse.subject = subjectReference
                    questionnaireResponse.encounter = encounterReference
                    fhirEngine.create(questionnaireResponse)

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
                            val date =
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).parse(it.answer)
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
                            it.linkId, it.text, it.answer
                        )

                        createResource(obs, subjectReference, encounterReference, context)
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

    private fun sourceExtension(
        resource: String,
        facility: String,
        context: Context
    ): Extension {
        return Extension().apply {
            url = "http://example.org/fhir/StructureDefinition/$resource-managingLocation"
            setValue(
                Reference().apply {
                    reference = "Location/$facility"
                    display = FormatterClass().getSharedPref("facilityName", context)
                })
        }
    }

    private fun sourceMetaTag(
        resource: String,
        facility: String,
        context: Context
    ): Coding {
        return Coding().apply {
            system = "http://example.org/fhir/StructureDefinition/$resource-managingLocation"
            code = "Location/$facility"
            display = FormatterClass().getSharedPref("facilityName", context)
        }
    }

    private suspend fun createSpecimenResource(
        linkId: String,
        dateAnswer: String,
        string: String,
        subjectReference: Reference,
        context: Context
    ) {
        val specimenCoding = Coding()
        specimenCoding.code = linkId
        specimenCoding.system = "specimen-details"
        specimenCoding.display = string

        val specimenType = CodeableConcept()
        specimenType.text = string
        specimenType.addCoding(specimenCoding)

        try {
            val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                dateAnswer
            )

            val calendar = Calendar.getInstance()
            calendar.time = dateOnly!!
            val now = Calendar.getInstance()
            calendar.set(
                Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY)
            )
            calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, now.get(Calendar.SECOND))
            calendar.set(
                Calendar.MILLISECOND, now.get(Calendar.MILLISECOND)
            )


            val dateTime = DateTimeType()
            dateTime.value = calendar.time

            val collection = Specimen.SpecimenCollectionComponent()
            collection.setCollected(dateTime)

            val specimen = Specimen()
            specimen.subject = subjectReference
            specimen.type = specimenType
            specimen.collection = collection
            val facility = FormatterClass().getSharedPref("facility", context)
            if (facility != null) {
                specimen.meta = Meta().apply {
                    tag = listOf(
                        sourceMetaTag("measure", facility, context)
                    )
                }
                specimen.addExtension(sourceExtension("specimen", facility, context))
            }
            fhirEngine.create(specimen)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun String.toSlug(): String {
        return this.trim() // remove leading/trailing spaces
            .lowercase() // make all lowercase
            .replace("[^a-z0-9\\s-]".toRegex(), "") // remove special characters
            .replace("\\s+".toRegex(), "-") // replace spaces with hyphens
            .replace("-+".toRegex(), "-") // collapse multiple hyphens
    }


    private suspend fun createResource(
        obs: Observation, subjectReference: Reference, encounterReference: Reference,
        context: Context
    ) {
        try {
            val practitioner = FormatterClass().getSharedPref("fhirPractitionerId", context)
            obs.id = generateUuid()
            obs.subject = subjectReference
            obs.encounter = encounterReference
            if (practitioner != null) {
                obs.performerFirstRep.reference = "Practitioner/$practitioner"
            }
            obs.issued = Date()
            val facility = FormatterClass().getSharedPref("facility", context)
            if (facility != null) {
                obs.meta = Meta().apply {
                    tag = listOf(
                        sourceMetaTag("measure", facility, context)
                    )
                }
                obs.addExtension(sourceExtension("observation", facility, context))
            }
            fhirEngine.create(obs)

            println("Observation created: ${obs.id}")
        } catch (e: Exception) {
            Log.e("SavePatient", "Error saving patient", e)
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