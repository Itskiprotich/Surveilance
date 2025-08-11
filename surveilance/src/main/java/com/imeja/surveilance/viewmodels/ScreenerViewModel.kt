package com.imeja.surveilance.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.imeja.surveilance.AddPatientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.imeja.surveilance.FhirApplication
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.helpers.QuestionnaireHelper
import com.imeja.surveilance.models.QuestionnaireAnswer

import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class ScreenerViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {
    val questionnaire: String
        get() = getQuestionnaireJson()

    val isResourcesSaved = MutableLiveData<Boolean>()

    private val questionnaireResource: Questionnaire
        get() =
            FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().parseResource(questionnaire)
                    as Questionnaire

    private var questionnaireJson: String? = null
    private var fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)

    /**
     * Saves screener encounter questionnaire response into the application database.
     *
     * @param questionnaireResponse screener encounter questionnaire response
     */



    fun completeContactAssessment(
        questionnaireResponse: QuestionnaireResponse,
        patientId: String,
        encounter: String,
        questionnaireResponseString: String,
    ) {
        viewModelScope.launch {
            val bundle =
                ResourceMapper.extract(questionnaireResource, questionnaireResponse)
            val context = FhirContext.forR4()
            val questionnaire =
                context.newJsonParser().encodeResourceToString(questionnaireResponse)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val title = "afp-contact-case-information"
                    val linkReference = Reference("Patient/$patientId")
                    val encounterId = generateUuid()
                    val contactId = generateUuid()

                    var contact = Patient()
                    contact.id = contactId


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


                    val identifierSystem = Identifier()
                    val typeCodeableConcept = CodeableConcept()
                    val codingList = ArrayList<Coding>()
                    val coding = Coding()
                    coding.system = title
                    coding.code = title
                    coding.display = title
                    codingList.add(coding)
                    typeCodeableConcept.coding = codingList
                    typeCodeableConcept.text = encounterId

                    identifierSystem.value = encounterId
                    identifierSystem.system = title
                    identifierSystem.type = typeCodeableConcept


                    contact.identifier.add(identifierSystem0)
                    contact.identifier.add(identifierSystem)
                    contact.linkFirstRep.other = linkReference

                    val subjectReference = Reference("Patient/$contactId")
                    val jsonObject = JSONObject(questionnaireResponseString)
                    val extractedAnswers = extractStructuredAnswersOnlyFromItems(jsonObject)

                    val nameEntry = extractedAnswers.find { it.linkId == "652156781680" }
                    val dobEntry = extractedAnswers.find { it.linkId == "833589441171" }
                    val genderEntry = extractedAnswers.find { it.linkId == "952250448507" }

                    val subCountyEntry = extractedAnswers.find { it.linkId == "a3-sub-county" }
                    val countyEntry = extractedAnswers.find { it.linkId == "a4-county" }

                    nameEntry?.answer?.let { fullName ->
                        val parts = fullName.trim().split("\\s+".toRegex())
                        when (parts.size) {
                            1 -> {
                                contact.nameFirstRep.family = parts[0]
                            }

                            2 -> {
                                contact.nameFirstRep.family = parts[0]
                                contact.nameFirstRep.addGiven(parts[1])
                            }

                            else -> {
                                contact.nameFirstRep.family = parts[0]
                                contact.nameFirstRep.addGiven(parts[1])
                                contact.nameFirstRep.addGiven(parts.drop(2).joinToString(" "))
                            }
                        }
                    }
                    if (genderEntry != null) {
                        val gender = when (genderEntry.answer.lowercase()) {
                            "male" -> Enumerations.AdministrativeGender.MALE
                            "female" -> Enumerations.AdministrativeGender.FEMALE
                            else -> Enumerations.AdministrativeGender.UNKNOWN
                        }
                        contact.gender = gender
                    }

                    fhirEngine.create(contact)


                    val qh = QuestionnaireHelper()
                    val enc = qh.generalEncounter(encounter, encounterId)
                    enc.id = encounterId
                    enc.subject = subjectReference
                    enc.reasonCodeFirstRep.codingFirstRep.code = title

                    val codeableConcept = CodeableConcept()
                    codeableConcept.codingFirstRep.code = "case-information"
                    codeableConcept.codingFirstRep.display = "case-information"
                    codeableConcept.codingFirstRep.system = "case-information"
                    codeableConcept.text = "case-information"
                    enc.addReasonCode(codeableConcept)
                    enc.identifier.add(identifierSystem0)

                    fhirEngine.create(enc)

                    val encounterReference = Reference("Encounter/$encounterId")
                    var county = ""
                    var subCounty = ""
                    val currentYear = LocalDate.now().year

                    if (subCountyEntry != null) {
                        subCounty = subCountyEntry.answer
                    }
                    if (countyEntry != null) {
                        county = countyEntry.answer
                    }

                    val countyCode = county.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()

                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-AFP-C"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    createResource(obs, subjectReference, encounterReference)

                    extractedAnswers.forEach {

                        val obs = qh.codingQuestionnaire(
                            it.linkId, it.text,
                            it.answer
                        )
                        createResource(obs, subjectReference, encounterReference)
                        println("Data Found LinkId: ${it.linkId}, Text: ${it.text}, Answer: ${it.answer}")
                    }

                    CoroutineScope(Dispatchers.Main).launch { isResourcesSaved.value = true }
                } catch (e: Exception) {

                    CoroutineScope(Dispatchers.Main).launch { isResourcesSaved.value = false }
                }
            }
        }
    }

    fun completeLabAssessment(
        questionnaireResponse: QuestionnaireResponse,
        patientId: String,
        encounter: String,
        title: String,
        questionnaireResponseString: String,
    ) {
        viewModelScope.launch {
            val bundle =
                ResourceMapper.extract(questionnaireResource, questionnaireResponse)
            val context = FhirContext.forR4()
            val questionnaire =
                context.newJsonParser().encodeResourceToString(questionnaireResponse)

            CoroutineScope(Dispatchers.IO).launch {
                try {
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

                    val subjectReference = Reference("Patient/$patientId")
                    val jsonObject = JSONObject(questionnaireResponseString)
                    val extractedAnswers = extractStructuredAnswersOnlyFromItems(jsonObject)

                    val qh = QuestionnaireHelper()
                    val encounterId = generateUuid()
                    val enc = qh.generalEncounter(encounter, encounterId)
                    enc.id = encounterId
                    enc.subject = subjectReference
                    enc.reasonCodeFirstRep.codingFirstRep.code = title
                    enc.identifier.add(identifierSystem0)

                    fhirEngine.create(enc)

                    val encounterReference = Reference("Encounter/$encounterId")
                    extractedAnswers.forEach {

                        val obs = qh.codingQuestionnaire(
                            it.linkId, it.text,
                            it.answer
                        )
                        createResource(obs, subjectReference, encounterReference)
                    }

                    CoroutineScope(Dispatchers.Main).launch { isResourcesSaved.value = true }
                } catch (e: Exception) {

                    CoroutineScope(Dispatchers.Main).launch { isResourcesSaved.value = false }
                }
            }
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
                            answerObj.has("valueDateTime") -> answerObj.optString("valueDateTime", "")
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


    fun extractStructuredAnswersOnlyFromItemsOld(json: JSONObject): List<QuestionnaireAnswer> {
        val results = mutableListOf<QuestionnaireAnswer>()

        fun processItems(items: JSONArray) {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val linkId = item.optString("linkId", "")
                val text = item.optString("text", "")

                // Extract from answer[] if available
                if (item.has("answer")) {
                    val answers = item.getJSONArray("answer")
                    for (j in 0 until answers.length()) {
                        val answerObj = answers.getJSONObject(j)

                        // Only extract from answer directly — not from answer.item[]
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

                            else -> null
                        }

                        if (!value.isNullOrBlank()) {
                            results.add(QuestionnaireAnswer(linkId, text, value))
                        }
                    }
                }

                // Recurse only into item.item[] (not answer.item[])
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

    fun extractStructuredAnswers(response: JSONObject): List<QuestionnaireAnswer> {
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

    private suspend fun saveResources(
        bundle: Bundle,
        subjectReference: Reference,
        encounterId: String,
        reason: String,
    ) {

        val encounterReference = Reference("Encounter/$encounterId")
        bundle.entry.forEach {
            when (val resource = it.resource) {
                is Observation -> {
                    if (resource.hasCode()) {
                        resource.id = generateUuid()
                        resource.subject = subjectReference
                        resource.encounter = encounterReference
                        resource.issued = Date()
                        saveResourceToDatabase(resource)
                    }
                }

                is Condition -> {
                    if (resource.hasCode()) {
                        resource.id = generateUuid()
                        resource.subject = subjectReference
                        resource.encounter = encounterReference
                        saveResourceToDatabase(resource)
                    }
                }

                is Encounter -> {
                    resource.subject = subjectReference
                    resource.id = encounterId
                    resource.reasonCodeFirstRep.text = reason
                    resource.reasonCodeFirstRep.codingFirstRep.code = reason
                    resource.status = Encounter.EncounterStatus.INPROGRESS
                    saveResourceToDatabase(resource)
                }
            }
        }
    }

    private fun isRequiredFieldMissing(bundle: Bundle): Boolean {
        bundle.entry.forEach {
            val resource = it.resource
            when (resource) {
                is Observation -> {
                    if (resource.hasValueQuantity() && !resource.valueQuantity.hasValueElement()) {
                        return true
                    }
                }
                // TODO check other resources inputs
            }
        }
        return false
    }

    private suspend fun saveResourceToDatabase(resource: Resource) {
        fhirEngine.create(resource)
    }

    private fun getQuestionnaireJson(): String {
        questionnaireJson?.let {
            return it!!
        }
        questionnaireJson = readFileFromAssets(state[QUESTIONNAIRE_FILE_PATH_KEY]!!)
        return questionnaireJson!!
    }

    private fun readFileFromAssets(filename: String): String {
        return getApplication<Application>().assets.open(filename).bufferedReader().use {
            it.readText()
        }
    }

    private fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }

    private companion object {
        const val ASTHMA = "161527007"
        const val LUNG_DISEASE = "13645005"
        const val DEPRESSION = "35489007"
        const val DIABETES = "161445009"
        const val HYPER_TENSION = "161501007"
        const val HEART_DISEASE = "56265001"
        const val HIGH_BLOOD_LIPIDS = "161450003"

        const val FEVER = "386661006"
        const val SHORTNESS_BREATH = "13645005"
        const val COUGH = "49727002"
        const val LOSS_OF_SMELL = "44169009"

        const val SPO2 = "59408-5"

        private val comorbidities: Set<String> =
            setOf(
                ASTHMA,
                LUNG_DISEASE,
                DEPRESSION,
                DIABETES,
                HYPER_TENSION,
                HEART_DISEASE,
                HIGH_BLOOD_LIPIDS,
            )
        private val symptoms: Set<String> =
            setOf(FEVER, SHORTNESS_BREATH, COUGH, LOSS_OF_SMELL)
    }
}