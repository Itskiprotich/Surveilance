package com.imeja.surveilance.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.asStringValue
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.String

class PatientListViewModel(
    application: Application,
    private val fhirEngine: FhirEngine
) :
    AndroidViewModel(application) {
    val liveSearchedPatients = MutableLiveData<List<PatientItem>>()
    val liveSearchedCases = MutableLiveData<List<PatientItem>>()
    val liveRumorCases = MutableLiveData<List<RumorItem>>()
    val patientCount = MutableLiveData<Long>()


    init {
        updatePatientListAndPatientCount(
            { getSearchResults() },
            { searchedPatientCount() })
    }

    fun searchPatientsByName(nameQuery: String) {
        updatePatientListAndPatientCount(
            { getSearchResults(nameQuery) },
            { count(nameQuery) })
    }

    fun handleCurrentCaseListing(category: String) {
        viewModelScope.launch {
            liveSearchedCases.value = retrieveCasesByDisease(category)
//            patientCount.value = count()
        }
    }

    fun handleCurrentRumorCaseListing(category: String) {
        viewModelScope.launch {
            liveRumorCases.value = retrieveRumorCasesByDisease(category)
//            patientCount.value = count()
        }
    }

    /**
     * [updatePatientListAndPatientCount] calls the search and count lambda and updates the live data
     * values accordingly. It is initially called when this [ViewModel] is created. Later its called
     * by the client every time search query changes or data-sync is completed.
     */
    private fun updatePatientListAndPatientCount(
        search: suspend () -> List<PatientItem>,
        count: suspend () -> Long,
    ) {
        viewModelScope.launch {
            liveSearchedPatients.value = search()
            patientCount.value = count()
        }
    }

    /**
     * Returns count of all the [Patient] who match the filter criteria unlike [getSearchResults]
     * which only returns a fixed range.
     */
    private suspend fun count(nameQuery: String = ""): Long {
        return fhirEngine.count<Patient> {
            if (nameQuery.isNotEmpty()) {
                filter(
                    Patient.NAME,
                    {
                        modifier = StringFilterModifier.CONTAINS
                        value = nameQuery
                    },
                )
            }
        }
    }

    private suspend fun getSearchResults(
        nameQuery: String = "",
    ): List<PatientItem> {

        val patients: MutableList<PatientItem> = mutableListOf()
        fhirEngine
            .search<Patient> {
                if (nameQuery.isNotEmpty()) {
                    filter(
                        Patient.NAME,
                        {
                            modifier = StringFilterModifier.CONTAINS
                            value = nameQuery
                        },
                    )
                }
                sort(Patient.GIVEN, Order.ASCENDING)
                count = 100
                from = 0
            }
            .mapIndexed { index, fhirPatient ->
                var item = fhirPatient.resource.toPatientItem(index + 1)
                try {

                    val encounter = loadEncounter(item.resourceId)
                    val caseInfoEncounter =
                        encounter.firstOrNull {
                            it.reasonCodeFirstRep.codingFirstRep.code == "Case Information"
                        }

                    caseInfoEncounter?.let {

                        val childEncounter = loadChildEncounter(item.resourceId, it.logicalId)
                        val childCaseInfoEncounter =
                            childEncounter.firstOrNull {
                                it.reasonCode == "Measles Lab Information"
                            }

                        childCaseInfoEncounter?.let { kk ->
                            val obs1 =
                                fhirEngine.search<Observation> {
                                    filter(
                                        Observation.ENCOUNTER,
                                        { value = "Encounter/${kk.id}" })
                                }

                            val measlesIgm =
                                obs1.firstOrNull { it.resource.code.codingFirstRep.code == "measles-igm" }
                                    ?.resource
                                    ?.value
                                    ?.asStringValue() ?: ""


                            val finalClassification = when (measlesIgm.lowercase()) {
                                "positive" -> obs1.firstOrNull {
                                    it.resource.code.codingFirstRep.code == "final-confirm-classification"
                                }?.resource?.value?.asStringValue() ?: ""

                                "negative" -> obs1.firstOrNull {
                                    it.resource.code.codingFirstRep.code == "final-negative-classification"
                                }?.resource?.value?.asStringValue() ?: ""

                                else -> obs1.firstOrNull {
                                    it.resource.code.codingFirstRep.code == "final-classification"
                                }?.resource?.value?.asStringValue() ?: ""
                            }

                            item = item.copy(labResults = measlesIgm, status = finalClassification)
                        }

                        // pull all Obs for this Encounter
                        val obs =
                            fhirEngine.search<Observation> {
                                filter(
                                    Observation.ENCOUNTER,
                                    { value = "Encounter/${it.logicalId}" })
                            }

                        val epid =
                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "EPID" }
                                ?.resource
                                ?.value
                                ?.asStringValue() ?: "still loading"
                        val county =
                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "a4-county" }
                                ?.resource
                                ?.value
                                ?.asStringValue() ?: ""
                        val subCounty =
                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "a3-sub-county" }
                                ?.resource
                                ?.value
                                ?.asStringValue() ?: ""
                        val onset =
                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "728034137219" }
                                ?.resource
                                ?.value
                                ?.asStringValue() ?: ""

                        item =
                            item.copy(
                                encounterId = it.logicalId,
                                epid = epid,
                                subCounty = subCounty,
                                county = county,
                                caseOnsetDate = onset
                            )
                    }

                    println("Found : None for Now")

                } catch (e: Exception) {
                    e.printStackTrace()

                    println("Error Loading Page : ${e.message}")
                }
                item
            }
            .let {
                val sortedCases = it.sortedByDescending { q -> q.lastUpdated }

                patients.addAll(sortedCases)
            }

        return patients
    }

    private suspend fun retrieveRumorCasesByDisease(
        nameQuery: String,
    ): List<RumorItem> {
        return fhirEngine
            .search<Patient> {
                sort(Patient.GIVEN, Order.ASCENDING)
                count = 500
                from = 0
            }
            .mapIndexedNotNull { index, fhirPatient ->
                val matchingIdentifier = fhirPatient.resource.identifier.find {
                    it.system == nameQuery
                }
                if (matchingIdentifier != null) {
                    // Convert the FHIR Patient resource to your PatientItem model
                    var data = fhirPatient.resource.toPatientItem(index + 1)

                    val logicalId = matchingIdentifier.value
                    val obs =
                        fhirEngine.search<Observation> {
                            filter(
                                Observation.ENCOUNTER,
                                { value = "Encounter/${logicalId}" })
                        }.take(500)
                    var mohName =
                        obs.firstOrNull { it.resource.code.codingFirstRep.code == "683805917262" }
                            ?.resource
                            ?.value
                            ?.asStringValue() ?: ""
                    val otherCadreName =
                        obs.firstOrNull { it.resource.code.codingFirstRep.code == "223529605110" }
                            ?.resource
                            ?.value
                            ?.asStringValue() ?: ""
                    if (mohName.contains("Other")) {
                        mohName = otherCadreName
                    }
                    var agency =
                        obs.firstOrNull { it.resource.code.codingFirstRep.code == "683805917111" }
                            ?.resource
                            ?.value
                            ?.asStringValue() ?: ""
                    var agencyOther =
                        obs.firstOrNull { it.resource.code.codingFirstRep.code == "22311605110" }
                            ?.resource
                            ?.value
                            ?.asStringValue() ?: ""
                    if (agency.contains("Other")) {
                        agency = agencyOther
                    }

                    var response = RumorItem(
                        id = data.id,
                        resourceId = data.resourceId,
                        encounterId = matchingIdentifier.value,
                        mohName = mohName,

                        directorate = agency,
                        division = obs.firstOrNull { it.resource.code.codingFirstRep.code == "686990243396" }
                            ?.resource
                            ?.value
                            ?.asStringValue() ?: "",
                        village = obs.firstOrNull { it.resource.code.codingFirstRep.code == "871818396498" }
                            ?.resource
                            ?.value
                            ?.asStringValue() ?: "",
                        subCounty = obs.firstOrNull { it.resource.code.codingFirstRep.code == "a3-sub-county" }
                            ?.resource
                            ?.value
                            ?.asStringValue() ?: "",
                        county = obs.firstOrNull { it.resource.code.codingFirstRep.code == "a4-county" }
                            ?.resource
                            ?.value
                            ?.asStringValue() ?: "",
                        lastUpdated = data.lastUpdated
                    )


                    response
                } else {

                    null
                }

            }.sortedByDescending { it.lastUpdated }
    }

    fun getAnswerValueAsString(
        item: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
        linkId: String
    ): String {
        val answer = item
            .flatMap { it.item ?: emptyList() }
            .firstOrNull { it.linkId == linkId }
            ?.answer?.firstOrNull()
            ?.value
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)


        return when (answer) {
            is DateType -> answer.value?.let {
                dateFormat.format(it)
            } ?: "" // returns yyyy-MM-dd
            is DateTimeType -> answer.value?.let { dateFormat.format(it) } ?: ""
            is Reference -> answer.display ?: answer.reference ?: ""
            is StringType -> answer.value ?: ""
            is BooleanType -> answer.value.toString()
            is IntegerType -> answer.value.toString()
            is DecimalType -> answer.value.toString()
            is Coding -> answer.display ?: answer.code ?: ""
            else -> answer?.primitiveValue() ?: ""
        }
    }


    private suspend fun retrieveCasesByDisease(
        nameQuery: String,
    ): List<PatientItem> {
        val isSummary = nameQuery.contains("mpox")

        println("Current Workflow :::: $nameQuery")
        when (nameQuery) {
            "mpox-supervisor-checklist" -> {

                val questionnaireData: MutableList<PatientItem> = mutableListOf()
                fhirEngine.search<QuestionnaireResponse> {
                    sort(QuestionnaireResponse.AUTHORED, Order.ASCENDING)
                    count = 500
                    from = 0
                }.mapIndexed { index, fhirPatient ->

                    val county = getAnswerValueAsString(fhirPatient.resource.item, "294367770999")
                    val subCounty =
                        getAnswerValueAsString(fhirPatient.resource.item, "819946803642")
                    var caseOnsetDate =
                        getAnswerValueAsString(fhirPatient.resource.item, "728034137219")


                    var siteName =
                        getAnswerValueAsString(fhirPatient.resource.item, "site_name")

                    var teamNumber =
                        getAnswerValueAsString(fhirPatient.resource.item, "site_type")
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                    val authored = try {
                        val authoredDate: Date = fhirPatient.resource.authored
                        val localDate = authoredDate.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                        localDate.format(formatter)  // format here instead of toString()
                    } catch (e: Exception) {
                        ""
                    }

                    if (caseOnsetDate.isEmpty()) {
                        caseOnsetDate = try {
                            val authoredDate: Date = fhirPatient.resource.authored
                            val localDate = authoredDate.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            localDate.toString()
                        } catch (e: Exception) {
                            ""
                        }

                    }

                    val data = PatientItem(
                        id = (index + 1).toString(),
                        resourceId = fhirPatient.resource.logicalId,
                        encounterId = fhirPatient.resource.logicalId,
                        name = fhirPatient.resource.item.firstOrNull()?.item?.firstOrNull() { it.linkId == "294367770999" }?.answer?.firstOrNull()?.valueReference?.display
                            ?: "",
                        gender = "",
                        phone = "",
                        city = "",
                        country = "",
                        isActive = false,
                        epid = "",
                        county = county,
                        subCounty = subCounty,
                        caseOnsetDate = caseOnsetDate,
                        lastUpdated = authored,
                        isSummary = isSummary,
                        campaignDate = siteName,
                        teamNumber = teamNumber
                    )
                    data
                }.also {
                    questionnaireData.addAll(it)
                }

                return questionnaireData.sortedByDescending { it.lastUpdated }
            }

            else -> {

                return fhirEngine
                    .search<Patient> {
                        sort(Patient.GIVEN, Order.ASCENDING)
                        count = 500
                        from = 0
                    }
                    .mapIndexedNotNull { index, fhirPatient ->
                        // Only return the patient if one of the identifiers matches the system

                        val matchingIdentifier = when (nameQuery) {
                            "rcce" -> fhirPatient.resource.identifier.find {
                                it.system == "rcce-community-questionnaire" || it.system == "rcce-countysubcounty-interface"
                            }

                            else -> fhirPatient.resource.identifier.find {
                                it.system == nameQuery
                            }
                        }
                        val epidIdenfifier =
                            fhirPatient.resource.identifier.find { it.type.codingFirstRep.code == "EPID" }



                        if (matchingIdentifier != null) {
                            // Convert the FHIR Patient resource to your PatientItem model
                            var data = fhirPatient.resource.toPatientItem(index + 1)
                            val logicalId = matchingIdentifier.value
                            val encounterQuestionnaire = matchingIdentifier.system
                            val obs =
                                fhirEngine.search<Observation> {
                                    filter(
                                        Observation.ENCOUNTER,
                                        { value = "Encounter/${logicalId}" })
                                }.take(500)

                            val epid = if (epidIdenfifier != null) epidIdenfifier.value else
                                obs.firstOrNull { it.resource.code.codingFirstRep.code == "EPID" }
                                    ?.resource
                                    ?.value
                                    ?.asStringValue() ?: ""

                            val county =
                                if (fhirPatient.resource.hasAddress()) if (fhirPatient.resource.addressFirstRep.hasCity()) fhirPatient.resource.addressFirstRep.city else "" else
                                    obs.firstOrNull { it.resource.code.codingFirstRep.code == "a4-county" }
                                        ?.resource
                                        ?.value
                                        ?.asStringValue() ?: ""
                            val subCounty =
                                if (fhirPatient.resource.hasAddress()) if (fhirPatient.resource.addressFirstRep.hasState()) fhirPatient.resource.addressFirstRep.state else "" else
                                    obs.firstOrNull { it.resource.code.codingFirstRep.code == "a3-sub-county" }
                                        ?.resource
                                        ?.value
                                        ?.asStringValue() ?: ""
                            val onset =
                                obs.firstOrNull { it.resource.code.codingFirstRep.code == "728034137219" }
                                    ?.resource
                                    ?.value
                                    ?.asStringValue() ?: ""
                            val caseList =
                                obs.firstOrNull { it.resource.code.codingFirstRep.code == "865158268604" }
                                    ?.resource
                                    ?.value
                                    ?.asStringValue() ?: "Case"


                            val campaignDay =
                                obs.firstOrNull { it.resource.code.codingFirstRep.code == "campaign_day" }
                                    ?.resource
                                    ?.value
                                    ?.asStringValue() ?: ""
                            val teamNumber =
                                obs.firstOrNull { it.resource.code.codingFirstRep.code == "team_no" }
                                    ?.resource
                                    ?.value
                                    ?.asStringValue() ?: ""



                            println("Current Workflow :::: Campaign Day : $campaignDay")

                            // Loading Lab Results
                            val childEncounter = loadChildEncounter(data.resourceId, logicalId)

                            when (nameQuery) {


                                "moh-505-reporting-form" -> {

                                }

                                "vl-case-information" -> {

                                    val childCaseInfoEncounter =
                                        childEncounter.firstOrNull {
                                            it.reasonCode == "VL Laboratory Examination"
                                        }

                                    childCaseInfoEncounter?.let { kk ->
                                        val obs1 =
                                            fhirEngine.search<Observation> {
                                                filter(
                                                    Observation.ENCOUNTER,
                                                    { value = "Encounter/${kk.id}" })
                                            }
                                        var results = "Pending Results"
                                        val rapidResults =
                                            obs1.firstOrNull { it.resource.code.codingFirstRep.code == "286501145394" }
                                                ?.resource
                                                ?.value
                                                ?.asStringValue() ?: "Pending"
                                        val datResult =
                                            obs1.firstOrNull { it.resource.code.codingFirstRep.code == "839711142610" }
                                                ?.resource
                                                ?.value
                                                ?.asStringValue() ?: "Pending"

                                        val aResult =
                                            obs1.firstOrNull { it.resource.code.codingFirstRep.code == "108406555539" }
                                                ?.resource
                                                ?.value
                                                ?.asStringValue() ?: "Pending"
                                        val mResult =

                                            obs1.firstOrNull { it.resource.code.codingFirstRep.code == "320819009291" }
                                                ?.resource
                                                ?.value
                                                ?.asStringValue() ?: "Pending"

                                        var status =
                                            obs1.firstOrNull { it.resource.code.codingFirstRep.code == "655245793432" }
                                                ?.resource
                                                ?.value
                                                ?.asStringValue() ?: "Pending"
                                        val otherStatus =
                                            obs1.firstOrNull { it.resource.code.codingFirstRep.code == "843481153132" }
                                                ?.resource
                                                ?.value
                                                ?.asStringValue() ?: "Pending"

                                        if (status == "Other (specify)") {
                                            status = otherStatus
                                        }
                                        // Normalize to lowercase for easier comparison
                                        val allResults = listOf(
                                            rapidResults,
                                            datResult,
                                            aResult,
                                            mResult
                                        ).map { it.lowercase() }

                                        results = when {
                                            allResults.any { it == "positive" } -> "Positive"
                                            allResults.all { it == "negative" } -> "Negative"
                                            allResults.all { it == "not done" } -> "Not Done"
                                            else -> "Pending Results"
                                        }

                                        data = data.copy(
                                            labResults = results,
                                            status = status
                                        )
                                    }
                                }

                                "afp-case-information" -> {
                                    // CLASSIFICATION FOR A AFP CASE
                                    val childCaseInfoEncounter =
                                        childEncounter.firstOrNull {
                                            it.reasonCode == "AFP Final Lab Information"
                                        }

                                    childCaseInfoEncounter?.let { kk ->
                                        val obs1 =
                                            fhirEngine.search<Observation> {
                                                filter(
                                                    Observation.ENCOUNTER,
                                                    { value = "Encounter/${kk.id}" })
                                            }
                                        val afp =
                                            obs1.firstOrNull { it.resource.code.codingFirstRep.code == "329949474707" }
                                                ?.resource
                                                ?.value
                                                ?.asStringValue() ?: "Pending"

                                        data = data.copy(
                                            labResults = afp,
                                            status = when (afp) {
                                                "WPV", "cVDPV", "aVDPV", "iVDPV" -> "Confirmed by lab"
                                                "Discarded" -> "Discarded"
                                                "Compatible" -> "Compatible"
                                                else -> "Pending"
                                            }
                                        )
                                    }
                                }

                                else -> {
                                    var measlesIgm = "Pending"
                                    var finalClassification = "Pending Results"
                                    var maxDays = "No"
                                    val childCaseInfoEncounter =
                                        childEncounter.firstOrNull {
                                            it.reasonCode == "Measles Lab Information"
                                        }

                                    childCaseInfoEncounter?.let { kk ->
                                        val obs1 =
                                            fhirEngine.search<Observation> {
                                                filter(
                                                    Observation.ENCOUNTER,
                                                    { value = "Encounter/${kk.id}" })
                                            }

                                        measlesIgm =
                                            obs1.firstOrNull { it.resource.code.codingFirstRep.code == "measles-igm" }
                                                ?.resource
                                                ?.value
                                                ?.asStringValue() ?: "Pending"

                                        maxDays =
                                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "308128177300" }
                                                ?.resource
                                                ?.value
                                                ?.asStringValue() ?: ""


                                        finalClassification = when (measlesIgm.lowercase()) {
                                            "positive" -> {
                                                when (maxDays.lowercase()) {
                                                    "yes" -> "Pending"
                                                    else -> "Confirmed by lab"
                                                }
                                            }

                                            "negative" -> "Discarded"
                                            "indeterminate" -> "Compatible/Clinical/Probable"
                                            else -> "Pending Results"

                                        }

                                        data =
                                            data.copy(
                                                labResults = measlesIgm,
                                                status = finalClassification,
                                            )
                                    }
                                }
                            }
                            data =
                                data.copy(
                                    caseList = caseList,
                                    encounterId = logicalId,
                                    epid = epid,
                                    county = county,
                                    subCounty = subCounty,
                                    caseOnsetDate = onset,
                                    encounterQuestionnaire = encounterQuestionnaire,
                                    isSummary = isSummary,
                                    campaignDate = campaignDay,
                                    teamNumber = teamNumber
                                )
                            data
                        } else {
                            null // Not a match — exclude
                        }
                    }
                    .sortedByDescending { it.lastUpdated }
            }
        }
    }

    data class RumorItem(
        val id: String,
        val resourceId: String,
        val encounterId: String,
        val mohName: String,
        val directorate: String,
        val division: String,
        val village: String,
        val subCounty: String,
        val county: String,
        val lastUpdated: String
    )

    /** The Patient's details for display purposes. */
    data class PatientItem(
        val id: String,
        val resourceId: String,
        val encounterId: String,
        val name: String,
        val gender: String,
        val dob: LocalDate? = null,
        val phone: String,
        val city: String,
        val country: String,
        val isActive: Boolean,
        val epid: String,
        val county: String,
        val subCounty: String,
        val caseOnsetDate: String,
        val status: String = "Pending Results",
        val labResults: String = "Pending",
        val lastUpdated: String,
        val caseList: String = "Case",
        val vaccinated: String = "No",
        val encounterQuestionnaire: String = "",
        val isSummary: Boolean = false,
        val campaignDate: String = "",
        val teamNumber: String = "",
        val supervisorName: String = ""
    ) {
        override fun toString(): String = name
    }

    /** The Observation's details for display purposes. */
    data class ObservationItem(
        val id: String,
        val code: String,
        val value: String,
        val created: String
    ) {
        override fun toString(): String = code
    }

    data class CaseDiseaseData(
        val logicalId: String,
        val name: String,
        val fever: String = "",
        val rash: String = ""
    )

    data class LabResults(
        val encounterId: String,
        val observations: List<ObservationItem> = emptyList<ObservationItem>()
    )

    data class ContactResults(
        val parentIdId: String,
        val childId: String,
        val name: String,
        var epid: String,
        val observations: List<ObservationItem> = emptyList<ObservationItem>()
    )

    data class CaseLabResultsData(
        val logicalId: String,
        val reasonCode: String,
        val dateSpecimenReceived: String = "",
        val specimenCondition: String = "",
        val measlesIgM: String = "",
        val rubellaIgM: String = "",
        val dateLabSentResults: String = "",
        val finalClassification: String = "",
        val subcountyName: String = "",
        val subcountyDesignation: String = "",
        val subcountyPhone: String = "",
        val subcountyEmail: String = "",
        val formCompletedBy: String = "",
        val nameOfPersonCompletingForm: String = "",
        val designation: String = "",
        val sign: String = ""
    )

    interface PatientDetailData {
        val firstInGroup: Boolean
        val lastInGroup: Boolean
    }

    data class CaseId(
        val patientId: String,
        val eNo: String,
    )

    data class CaseDetailSummaryData(
        val name: String,
        val sex: String,
        val dob: String,
        val logicalId: String,
        val encounterId: String,
        val observations: List<ObservationItem> = emptyList<ObservationItem>(),
        val epidNo: String
    )

    data class ClinicalData(
        val onset: String,
        val symptoms: List<String> = emptyList<String>(),
        val rashDate: String,
        val rashType: String,
        val otherType: String,
        val vaccinated: String,
        val doses: String,
        val thirtyDays: String,
        val lastVaccination: String,
        val homeVisit: String,
        val homeDateVisit: String,
        val caseEpilinked: String,
        val epiName: String,
        val epiEPID: String,
    )

    data class PersonDetails(
        val name: String,
        val sex: String,
        val dob: String,
        val residence: String,
        val parent: String,
        val houseNo: String,
        val neighbour: String,
        val street: String,
        val town: String,
        val subCountyName: String,
        val countyName: String,
        val parentPhone: String
    )


    data class CaseDetailData(
        val logicalId: String,
        val name: String,
        val sex: String,
        val dob: String,
        val epid: String,
        val subCounty: String,
        val county: String,
        val country: String,
        val yearOfReporting: String,
        val healthFacility: String,
        val typeOfHealthFacility: String,
        val subcountyOfFacility: String,
        val countyOfFacility: String,

        val onset: String,
        val residence: String,
        val facility: String,
        val type: String,
        val disease: String,
        val parent: String,
        val houseNo: String,
        val neighbour: String,
        val street: String,
        val town: String,
        val subCountyName: String,
        val countyName: String,
        val parentPhone: String,
        val dateFirstSeen: String,
        val dateSubCountyNotified: String,
        val hospitalized: String,
        val admissionDate: String,
        val ipNo: String,
        val diagnosis: String,
        val diagnosisMeans: String,
        val diagnosisMeansOther: String,
        val targetDisease: String,
        val wasPatientVaccinated: String,
        val noOfDoses: String,
        val twoMonthsVaccination: String,
        val patientStatus: String,
        val vaccineDate: String,
        // Case Details
        val clinicalSymptoms: String,
        val rashDate: String,
        val rashType: String,
        val patientVaccinated: String,
        val patientDoses: String,
        val vaccineDateThirtyDays: String,
        val lastDoseDate: String,
        val homeVisited: String,
        val homeVisitedDate: String,
        val epiLinked: String,

        // Clinical

        val patientOutcome: String,
        val sampleCollected: String,
        val inPatientOutPatient: String,

        //    Lab Information
        val specimen: String,
        val noWhy: String,
        val collectionDate: String,
        val specimenType: String,
        val specimenTypeOther: String,
        val dateSent: String,
        val labName: String,
        val bloodSpecimenCollected: String,
        val noWhyBlood: String,
        val dateBloodSpecimen: String,
        val urineSpecimenCollected: String,
        val noWhyUrine: String,
        val dateUrineSpecimen: String,
        val respiratorySampleCollected: String,
        val dateRespiratorySample: String,
        val noWhyRespiratory: String,
        val otherSpecimenCollected: String,
        val specifyOtherSpecimen: String,
        val dateOtherSpecimen: String,
        val dateSpecimenSentToLab: String

    )

    data class PatientDetailOverview(
        val patient: PatientItem,
        override val firstInGroup: Boolean = false,
        override val lastInGroup: Boolean = false,
    ) : PatientDetailData

    data class EncounterItem(
        val id: String,
        val reasonCode: String,
        val status: String = "",
        val lastUpdated: String = "",
    ) {
        override fun toString(): String = reasonCode
    }

    data class ConditionItem(
        val id: String,
        val code: String,
        val effective: String,
        val value: String,
    ) {
        override fun toString(): String = code
    }

    class PatientListViewModelFactory(
        private val application: Application,
        private val fhirEngine: FhirEngine,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PatientListViewModel::class.java)) {
                return PatientListViewModel(application, fhirEngine) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private var patientGivenName: String? = null
    private var patientFamilyName: String? = null

    fun setPatientGivenName(givenName: String) {
        patientGivenName = givenName
        searchPatientsByParameter()
    }

    fun setPatientFamilyName(familyName: String) {
        patientFamilyName = familyName
        searchPatientsByParameter()
    }

    private fun searchPatientsByParameter() {
        viewModelScope.launch {
            liveSearchedPatients.value = searchPatients()
            patientCount.value = searchedPatientCount()
        }
    }

    private suspend fun searchPatients(): List<PatientItem> {
        val patients =
            fhirEngine
                .search<Patient> {
                    filter(
                        Patient.GIVEN,
                        {
                            modifier = StringFilterModifier.CONTAINS
                            this.value = patientGivenName ?: ""
                        },
                    )
                    filter(
                        Patient.FAMILY,
                        {
                            modifier = StringFilterModifier.CONTAINS
                            this.value = patientFamilyName ?: ""
                        },
                    )
                    sort(Patient.GIVEN, Order.ASCENDING)
                    count = 100
                    from = 0
                }
                .mapIndexed { index, fhirPatient ->

                    val item = fhirPatient.resource.toPatientItem(index + 1)
                    try {
                        val encounter = loadEncounter(item.resourceId)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("Error Loading Patient data ${e.message}")
                    }
                    item
                }
                .toMutableList()

        return patients
    }

    private suspend fun loadEncounter(patientId: String): List<Encounter> {
        return fhirEngine
            .search<Encounter> { filter(Encounter.SUBJECT, { value = "Patient/$patientId" }) }
            .map { it.resource }
    }

    private suspend fun loadChildEncounter(
        patientId: String,
        encounterId: String
    ): List<EncounterItem> {

        val patients: MutableList<EncounterItem> = mutableListOf()
        fhirEngine
            .search<Encounter> {
                filter(Encounter.SUBJECT, { value = "Patient/$patientId" })
                filter(Encounter.PART_OF, { value = "Encounter/$encounterId" })

            }
            .map {
                var data = EncounterItem(
                    id = it.resource.logicalId,
                    reasonCode = it.resource.reasonCodeFirstRep.codingFirstRep.code
                )
                var lastUpdated = ""
                try {
                    if (it.resource.hasIdentifier()) {
                        val id = it.resource.identifier.find { it.system == "system-creation" }
                        if (id != null) {
                            lastUpdated = id.value
                        }
                    } else {
                        lastUpdated = ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                data = data.copy(
                    lastUpdated = lastUpdated
                )
                data

            }
            .let {
                val sortedCases = it.sortedByDescending { q -> q.lastUpdated }

                patients.addAll(sortedCases)
            }

        return patients

    }

    private suspend fun searchedPatientCount(): Long {
        return fhirEngine.count<Patient> {
            filter(
                Patient.GIVEN,
                {
                    modifier = StringFilterModifier.CONTAINS
                    this.value = patientGivenName ?: ""
                },
            )
            filter(
                Patient.FAMILY,
                {
                    modifier = StringFilterModifier.CONTAINS
                    this.value = patientFamilyName ?: ""
                },
            )
        }
    }
}

internal fun Patient.toPatientItem(
    position: Int,
): PatientListViewModel.PatientItem {
    // Show nothing if no values available for gender and date of birth.

    val patientId = if (hasIdElement()) idElement.idPart else ""
    val name = if (hasName()) name[0].nameAsSingleString else ""
    val gender = if (hasGenderElement()) genderElement.valueAsString else ""
    val dob =
        if (hasBirthDateElement()) {
            birthDateElement.value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            null
        }
    val phone = if (hasTelecom()) telecom[0].value else ""
    val city = if (hasAddress()) address[0].city else ""
    val country = if (hasAddress()) address[0].country else ""
    val isActive = active
    var epid = ""
    var county = ""
    var subCounty = ""
    var caseOnsetDate = ""

    var lastUpdated = ""
    if (hasIdentifier()) {
        val id = identifier.find { it.system == "system-creation" }
        if (id != null) {
            lastUpdated = id.value
        }
    } else {
        lastUpdated = ""
    }



    return PatientListViewModel.PatientItem(
        id = position.toString(),
        encounterId = "encounterId",
        resourceId = patientId,
        name = " $name",
        gender = gender ?: "",
        dob = dob,
        phone = phone ?: "",
        city = city ?: "",
        country = country ?: "",
        isActive = isActive,
        epid = epid,
        county = county,
        subCounty = subCounty,
        caseOnsetDate = caseOnsetDate,
        lastUpdated = lastUpdated,

        )
}