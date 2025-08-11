package com.imeja.surveilance.cases

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.imeja.surveilance.FhirApplication
import com.imeja.surveilance.adapters.PatientItemRecyclerViewAdapter
import com.imeja.surveilance.adapters.PatientItemRecyclerViewAdapterRumor
import com.imeja.surveilance.databinding.ActivityCaseListingBinding
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.viewmodels.PatientListViewModel

class CaseListingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaseListingBinding
    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientListViewModel: PatientListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaseListingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val titleName = FormatterClass().getSharedPref("listingTitle", this)
        val currentCase = FormatterClass().getSharedPref("currentCase", this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.apply {
            title = " $titleName"
        }


        fhirEngine = FhirApplication.fhirEngine(this)
        patientListViewModel =
            ViewModelProvider(
                this,
                PatientListViewModel.PatientListViewModelFactory(
                    this.application, fhirEngine
                ),
            )
                .get(PatientListViewModel::class.java)
        val recyclerView: RecyclerView = binding.patientListContainer.patientList
        val adapter = PatientItemRecyclerViewAdapter(this::onPatientItemClicked, "$titleName", this)
        val adapterRumor = PatientItemRecyclerViewAdapterRumor(this::onRumorItemClicked)

        if (currentCase != null) {
            val slug = currentCase.toSlug()

            when (slug) {

                "social-listening-and-rumor-tracking-tool" -> {
                    patientListViewModel.handleCurrentRumorCaseListing(slug)
                    recyclerView.adapter = adapterRumor
                }

                else -> {
                    patientListViewModel.handleCurrentCaseListing(slug)
                    recyclerView.adapter = adapter
                }
            }

            when (slug) {
                "social-listening-and-rumor-tracking-tool" -> {
                    patientListViewModel.liveRumorCases.observe(this) {
                        binding.apply {
                            count.text = "Showing ${it.size} Results"
                            patientListContainer.pbProgress.visibility = View.GONE
                        }

                        if (it.isEmpty()) {
                            binding.apply {
                                patientListContainer.caseCount.visibility = View.VISIBLE
                            }
                        } else {
                            binding.apply { patientListContainer.caseCount.visibility = View.GONE }
                        }

                        adapterRumor.submitList(it)
                    }
                }

                else -> {
                    patientListViewModel.liveSearchedCases.observe(this) {
                        binding.apply {
                            count.text = "Showing ${it.size} Results"
                            patientListContainer.pbProgress.visibility = View.GONE
                        }

                        if (it.isEmpty()) {
                            binding.apply {
                                patientListContainer.caseCount.visibility = View.VISIBLE
                            }
                        } else {
                            binding.apply { patientListContainer.caseCount.visibility = View.GONE }
                        }

                        adapter.submitList(it)
                    }
                }
            }
        }
    }

    private fun onRumorItemClicked(patientItem: PatientListViewModel.RumorItem) {
        val currentCase = FormatterClass().getSharedPref("currentCase", this)
        FormatterClass().saveSharedPref("resourceId", patientItem.resourceId, this)
        FormatterClass().saveSharedPref("encounterId", patientItem.encounterId, this)
        FormatterClass().deleteSharedPref("isCase", this)
        if (currentCase != null) {
            val slug = currentCase.toSlug()

            FormatterClass().saveSharedPref("latestEncounter", slug, this)
            when (slug) {
                "social-listening-and-rumor-tracking-tool",
                "vl-case-information",
                "moh-505-reporting-form",
                "afp-case-information" -> {
                    startActivity(Intent(this@CaseListingActivity, SummarizedActivity::class.java))
                }

                else -> {

                    startActivity(
                        Intent(
                            this@CaseListingActivity,
                            FullCaseDetailsActivity::class.java
                        )
                    )
                }
            }
        } else {
            Toast.makeText(this, "Please try again later ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onPatientItemClicked(patientItem: PatientListViewModel.PatientItem) {
        val currentCase = FormatterClass().getSharedPref("currentCase", this)
        FormatterClass().saveSharedPref("resourceId", patientItem.resourceId, this)
        FormatterClass().saveSharedPref("encounterId", patientItem.encounterId, this)
        FormatterClass().saveSharedPref(
            "encounterQuestionnaire",
            patientItem.encounterQuestionnaire,
            this
        )
        FormatterClass().deleteSharedPref("isCase", this)
        FormatterClass().deleteSharedPref("isVaccinated", this)
        if (currentCase != null) {
            val slug = currentCase.toSlug()

            FormatterClass().saveSharedPref("latestEncounter", slug, this)
            val activityIntent = Intent(this@CaseListingActivity, SummarizedActivity::class.java)
            val activityIntent2 =
                Intent(this@CaseListingActivity, ResponseQuestionnaireActivity::class.java)
            when (slug) {

                "mpox-supervisor-checklist" -> {

                    startActivity(activityIntent2)

                }

                "social-listening-and-rumor-tracking-tool",
                "vl-case-information", "mpox-tally-sheet",
                "afp-case-information",
                "rcce" -> {
                    startActivity(activityIntent)
                }

                else -> {
                    FormatterClass().apply {
                        saveSharedPref("isCase", patientItem.caseList, this@CaseListingActivity)
                        saveSharedPref(
                            "isVaccinated",
                            patientItem.vaccinated,
                            this@CaseListingActivity
                        )
                    }
                    startActivity(activityIntent)
                }
            }

        } else {
            Toast.makeText(this, "Please try again later ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun String.toSlug(): String {
        return this
            .trim() // remove leading/trailing spaces
            .lowercase() // make all lowercase
            .replace("[^a-z0-9\\s-]".toRegex(), "") // remove special characters
            .replace("\\s+".toRegex(), "-") // replace spaces with hyphens
            .replace("-+".toRegex(), "-") // collapse multiple hyphens
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

