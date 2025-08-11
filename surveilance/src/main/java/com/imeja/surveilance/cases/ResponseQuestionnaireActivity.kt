package com.imeja.surveilance.cases

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.imeja.surveilance.FhirApplication
import com.imeja.surveilance.adapters.GroupPagerAdapter
import com.imeja.surveilance.databinding.ActivityResponseQuestionnaireBinding
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.models.ChildItem
import com.imeja.surveilance.models.OutputGroup
import com.imeja.surveilance.models.OutputItem
import com.imeja.surveilance.models.QuestionnaireItem
import com.imeja.surveilance.viewholders.ResponseDetailsViewModel
import com.imeja.surveilance.viewholders.ResponseDetailsViewModelFactory

class ResponseQuestionnaireActivity : AppCompatActivity() {
    private lateinit var groups: MutableList<OutputGroup>
    private lateinit var binding: ActivityResponseQuestionnaireBinding
    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientDetailsViewModel: ResponseDetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResponseQuestionnaireBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fhirEngine = FhirApplication.fhirEngine(this@ResponseQuestionnaireActivity)
        val questionnaireId =
            FormatterClass().getSharedPref("resourceId", this@ResponseQuestionnaireActivity)
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout

        patientDetailsViewModel =
            ViewModelProvider(
                this,
                ResponseDetailsViewModelFactory(
                    this@ResponseQuestionnaireActivity.application, fhirEngine, "$questionnaireId"
                ),
            )
                .get(ResponseDetailsViewModel::class.java)


        groups =
            parseFromAssets(this, "mpox-supervisor-checklist.json").toMutableList()// this = Context
        patientDetailsViewModel.getInfoSummaryData("$questionnaireId")
        patientDetailsViewModel.liveSummaryData.observe(this) { data ->

            groups.forEach { group ->
                // For each item inside the group

                group.items.forEach { outputItem ->
                    // Try to find a matching observation

                    val matchingObservation = data.observations.find { obs ->
                        obs.code == outputItem.linkId
                    }
                    if (matchingObservation != null) {
                        outputItem.value = matchingObservation.value
                    }

                }
            }
            val adapter = GroupPagerAdapter(this, groups, emptyList())
            viewPager.adapter = adapter

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = adapter.getTabTitle(position)
            }.attach()
        }

    }

    fun parseFromAssets(context: Context, assets: String): List<OutputGroup> {
        var outputGroups: List<OutputGroup> = emptyList()

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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }


}