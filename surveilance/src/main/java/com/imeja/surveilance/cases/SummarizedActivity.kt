package com.imeja.surveilance.cases

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.imeja.surveilance.cases.child.LabResultsFragment
import com.imeja.surveilance.cases.child.RegionalLabResultsFragment
import com.imeja.surveilance.FhirApplication
import com.imeja.surveilance.adapters.GroupPagerAdapter
import com.imeja.surveilance.cases.child.ContactInformationFragment
import com.imeja.surveilance.cases.child.ITDLabFragment
import com.imeja.surveilance.cases.child.LocalLabFragment
import com.imeja.surveilance.cases.child.RegionalLabFragment
import com.imeja.surveilance.cases.child.VlFollowupFragment
import com.imeja.surveilance.cases.child.VlLabFragment
import com.imeja.surveilance.cases.child.VlTreatmentFragment
import com.imeja.surveilance.databinding.ActivitySummarizedBinding
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.models.ChildItem
import com.imeja.surveilance.models.OutputGroup
import com.imeja.surveilance.models.OutputItem
import com.imeja.surveilance.models.QuestionnaireItem
import com.imeja.surveilance.viewholders.ClientDetailsViewModel
import com.imeja.surveilance.viewholders.PatientDetailsViewModelFactory
import com.imeja.surveilance.viewmodels.PatientListViewModel
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class SummarizedActivity : AppCompatActivity() {
    private lateinit var groups: MutableList<OutputGroup>
    private lateinit var binding: ActivitySummarizedBinding
    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientDetailsViewModel: ClientDetailsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySummarizedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val patientId = FormatterClass().getSharedPref("resourceId", this@SummarizedActivity)
        val currentCase = FormatterClass().getSharedPref("currentCase", this)
        val latestEncounter = FormatterClass().getSharedPref("latestEncounter", this)
        val isCase = FormatterClass().getSharedPref("isCase", this)

        fhirEngine = FhirApplication.fhirEngine(this@SummarizedActivity)
        patientDetailsViewModel =
            ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    this@SummarizedActivity.application, fhirEngine, "$patientId"
                ),
            )
                .get(ClientDetailsViewModel::class.java)

        if (latestEncounter != null) {

            groups = parseFromAssets(this, latestEncounter).toMutableList()// this = Context


            val viewPager = binding.viewPager
            val tabLayout = binding.tabLayout

            if (currentCase != null) {
                val slug = currentCase.toSlug()
                val key = when (slug) {
                    "rcce" -> {
                        val encounterQuestionnaire = FormatterClass().getSharedPref(
                            "encounterQuestionnaire",
                            this@SummarizedActivity
                        )
                        "$encounterQuestionnaire"
                    }

                    "mpox-information" -> "mpox-tally-sheet"

                    else -> slug
                }
                patientDetailsViewModel.getPatientInfoSummaryData(key)
            }

            var customFragments = when (latestEncounter) {
                "measles-case-information" -> {
                    listOf(
                        "Laboratory Information" to LabResultsFragment(),
                        "Regional Laboratory Information" to RegionalLabResultsFragment()
                    )

                }

                "afp-case-information" -> {
                    listOf(
                        "Stool Specimen Results" to LocalLabFragment(),
                        "ITD Lab Results" to ITDLabFragment(),
                        "Final Laboratory Results" to RegionalLabFragment(),
                        "Contact Information" to ContactInformationFragment()
                    )
                }

                "vl-case-information" -> {
                    listOf(
                        "Laboratory Examination" to VlLabFragment(),
                        "Treatment/Hospitalization" to VlTreatmentFragment(),
                        "Six months followup examinations" to VlFollowupFragment()
                    )
                }

                else -> emptyList()
            }

            if (isCase != null) {
                if (isCase != "Case") {
                    val itemToRemove = groups.find { it.linkId == "271053545237" }
                    if (itemToRemove != null) {
                        groups.remove(itemToRemove)
                        customFragments = emptyList()

                    }
                }
            }
            patientDetailsViewModel.liveSummaryData.observe(this) { data ->

                groups.forEach { group ->
                    // For each item inside the group

                    group.items.forEach { outputItem ->
                        // Try to find a matching observation

                        val matchingObservation = data.observations.find { obs ->
                            obs.code == outputItem.linkId
                        }
                        when (outputItem.linkId) {

                            "992818778559" -> { // Retrieve EPID No.
                                outputItem.value = data.epidNo
                            }

                            "920645761660" -> { // Calculate Days since onset
                                outputItem.value = calculateDaysSinceOnset(data.observations)
                            }

                            "age-at-onset" -> {  // Calculate Age at Onset
                                outputItem.value = calculateAgeAtOnset(data.observations)
                            }

                            else ->
                                if (matchingObservation != null) {
                                    outputItem.value = matchingObservation.value
                                }
                        }
                    }
                }
                val adapter = GroupPagerAdapter(this, groups, customFragments)
                viewPager.adapter = adapter

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = adapter.getTabTitle(position)
                }.attach()
            }

        } else {
            Toast.makeText(this, "Please try again later!!", Toast.LENGTH_SHORT).show()
        }
    }

    fun String.toSlug(): String {
        return this
            .trim()
            .lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
    }

    fun calculateDaysSinceOnset(observations: List<PatientListViewModel.ObservationItem>): String {
        var age = "0"
        val date = observations.find { obs ->
            obs.code == "728034137219"
        }?.value
        val created = observations.find { obs ->
            obs.code == "728034137219"
        }?.created
        if (date == null || created == null) age = "0"
        try {

            // Parse the onset date (simple ISO format)
            val onsetDate = LocalDate.parse(date)
            // Parse the created date using a formatter
            val createdFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy")
            val createdDate = ZonedDateTime.parse(created, createdFormatter).toLocalDate()

            // Calculate the days between
            val daysBetween = ChronoUnit.DAYS.between(onsetDate, createdDate)

            println(" Date of Onset of Symptoms Days between: $daysBetween")
            age = "$daysBetween"
        } catch (e: Exception) {
            age = "0"
        }

        println("Date of Onset of Symptoms $date created $created")
        return age
    }

    fun calculateAgeAtOnset(observations: List<PatientListViewModel.ObservationItem>): String {
        var age = "0"
        val formatter = DateTimeFormatter.ISO_DATE // assumes date format is "yyyy-MM-dd"

        val dob = observations.find { obs ->
            obs.code == "257830485990"
        }?.value
        val onset = observations.find { obs ->
            obs.code == "728034137219"
        }?.value

        if (dob == null || onset == null) age = "0"
        try {
            val dobDate = LocalDate.parse(dob, formatter)
            val onsetDate = LocalDate.parse(onset, formatter)

            val period = Period.between(dobDate, onsetDate)

            age = "${period.years} years, ${period.months} months, ${period.days} days"
        } catch (e: Exception) {
            age = "0"
        }
        return age
    }

    fun parseFromAssets(context: Context, latestEncounter: String): List<OutputGroup> {
        var outputGroups: List<OutputGroup> = emptyList()

        val assets = when (latestEncounter) {
            "measles-case-information" -> "add-case.json"
            "afp-case-information" -> "afp-case.json"
            "vl-case-information" -> "vl-case.json"
            "moh-505-reporting-form" -> "moh505.json"
            "mpox-information" -> "mpox-tally-sheet.json"
            "mpox-tally-sheet" -> "mpox-tally-sheet.json"
            "social-listening-and-rumor-tracking-tool" -> "rumor-tracking-case.json"
            "rcce" -> {

                val encounterQuestionnaire = FormatterClass().getSharedPref(
                    "encounterQuestionnaire",
                    this@SummarizedActivity
                )
                println("This is the latest encounter $encounterQuestionnaire")
                when (encounterQuestionnaire) {
                    "rcce-community-questionnaire" -> "social-community.json"
                    "rcce-countysubcounty-interface" -> "social-county.json"
                    else -> ""

                }
            }

            else -> ""
        }
        try {
            if (assets.isNotEmpty()) {
                val jsonContent = context.assets.open(assets)
                    .bufferedReader()
                    .use { it.readText() }

                val gson = Gson()
                val questionnaire = gson.fromJson(jsonContent, QuestionnaireItem::class.java)

                outputGroups = questionnaire.item.map { group ->
                    OutputGroup(
                        linkId = group.linkId,
                        text = group.text,
                        type = group.type,
                        items = group.item?.flatMap { flattenItems(it) } ?: emptyList()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TAG", "File Error ${e.message}")
        }
        return outputGroups

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }


    fun flattenItems(
        item: ChildItem,
        parentConditions: Map<String, Pair<String, Boolean>> = emptyMap()
    ): List<OutputItem> {
        val currentConditions =
            mutableMapOf<String, Pair<String, Boolean>>().apply { putAll(parentConditions) }

        var enable = true
        var parentLink: String? = null
        var parentResponse: String? = null
        var enableOperator: String? = null
        item.enableWhen?.firstOrNull()?.let { condition ->
            parentLink = condition.question
            enableOperator = condition.operator
            val expectedAnswer = when {
                condition.answerCoding != null -> condition.answerCoding.display
                    ?: condition.answerCoding.code

                condition.answerString != null -> condition.answerString
                condition.answerBoolean != null -> condition.answerBoolean.toString()
                condition.answerDate != null -> condition.answerDate
                condition.answerInteger != null -> condition.answerInteger.toString()
                else -> null
            }
            parentResponse = expectedAnswer
            enable = false // assume not enabled unless condition is met at runtime
        }

        val children = item.item?.flatMap {
            flattenItems(it, currentConditions)
        } ?: emptyList()

        return if (item.type != "display") {

            val current = OutputItem(
                linkId = item.linkId,
                text = item.text,
                type = item.type,
                enable = enable,
                parentLink = parentLink,
                parentResponse = parentResponse,
                parentOperator = enableOperator
            )

            listOf(current) + children

        } else {
            children
        }
    }

}

