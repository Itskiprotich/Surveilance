package com.imeja.surveilance.cases

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.google.android.material.tabs.TabLayoutMediator
import com.imeja.surveilance.FhirApplication
import com.imeja.surveilance.R
import com.imeja.surveilance.databinding.ActivityFullCaseDetailsBinding
import com.imeja.surveilance.helpers.FormatterClass

class FullCaseDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullCaseDetailsBinding
    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientDetailsViewModel: ClientDetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFullCaseDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val patientId = FormatterClass().getSharedPref("resourceId", this@FullCaseDetailsActivity)
        val currentCase = FormatterClass().getSharedPref("currentCase", this)

        fhirEngine = FhirApplication.fhirEngine(this@FullCaseDetailsActivity)
        patientDetailsViewModel =
            ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    this@FullCaseDetailsActivity.application, fhirEngine, "$patientId"
                ),
            )
                .get(ClientDetailsViewModel::class.java)
        println("Started searching for cases *** $currentCase")
        if (currentCase != null) {
            val slug = currentCase.toSlug()
            patientDetailsViewModel.getPatientInfo(slug)
        }
        patientDetailsViewModel.livecaseData.observe(this) {

            binding.apply {
                val adapter = ViewPagerAdapter(this@FullCaseDetailsActivity)
                viewPager.adapter = adapter

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text =
                        when (position) {
                            0 -> "Reporting Site"
                            1 -> "Case Demographics"
                            2 -> "Case Information"
                            3 -> "Clinical History"
                            4 -> "Sample/Specimen Information"
                            5 -> "Laboratory Information"
                            6 -> "Regional Laboratory Information"
                            else -> "Reporting Site"
                        }
                }
                    .attach()

                epidNo.text = it.epid
                tvName.text = it.name
                tvSex.text = it.sex
                tvDob.text = it.dob
                tvFacility.text = it.facility
                tvType.text = it.type
                tvCounty.text = it.county
                tvSubCounty.text = it.subCounty
                tvDisease.text = it.disease
                tvOnset.text = it.onset
                tvResidence.text = it.residence
                tvFirstSeen.text = it.dateFirstSeen
                tvSubCounty.text = it.dateSubCountyNotified
                tvHospitalized.text = it.hospitalized
                tvIpOp.text = it.ipNo
                tvDiagnosis.text = it.diagnosis
                tvMeans.text = it.diagnosisMeans
                tvOtherSpecify.text = it.diagnosisMeansOther
                tvVaccinated.text = it.wasPatientVaccinated
                tvTwoMonths.text = it.twoMonthsVaccination
                tvStatus.text = it.patientStatus
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
//            R.id.action_refresh -> {
//                FormatterClass().saveSharedPref("questionnaire", "measles-case.json", this)
//                FormatterClass().saveSharedPref("title", "Measles Case", this)
//                val intent = Intent(this, AddCaseActivity::class.java)
//                intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "measles-case.json")
//                startActivity(intent)
//                true
//            }
//
//            R.id.action_settings -> {
//                FormatterClass().saveSharedPref("questionnaire", "measles-lab-results.json", this)
//                FormatterClass().saveSharedPref("title", "Lab Results", this)
//                val intent = Intent(this, AddCaseActivity::class.java)
//                intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "measles-lab-results.json")
//                startActivity(intent)
//                true
//            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
