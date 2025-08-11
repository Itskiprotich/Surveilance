package com.imeja.surveilance.cases


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.material.button.MaterialButton
import com.imeja.surveilance.AddPatientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.imeja.surveilance.AddPatientFragment.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import com.imeja.surveilance.R
import com.imeja.surveilance.databinding.ActivityAddCaseBinding
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.helpers.LocationUtils
import com.imeja.surveilance.helpers.ProgressDialogManager
import com.imeja.surveilance.viewmodels.ScreenerViewModel
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse


class AddCaseActivity : AppCompatActivity() {

    private val viewModel: ScreenerViewModel by viewModels()
    private lateinit var binding:
            ActivityAddCaseBinding // Binding class name is based on layout file name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddCaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val titleName = FormatterClass().getSharedPref("title", this@AddCaseActivity)
        supportActionBar.apply { title = titleName }

        LocationUtils.requestCurrentLocation(
            this,
            onLocationReceived = { lat, lon ->
                println("Latitude: $lat, Longitude: $lon")

                val latitude = lat.toString()
                val longitude = lon.toString()
                FormatterClass().saveSharedPref("latitude", latitude, this)
                FormatterClass().saveSharedPref("longitude", longitude, this)
            },
            onError = { error ->
                println("Error: $error")
            }
        )

        updateArguments()
        if (savedInstanceState == null) {
            addQuestionnaireFragment()
        }
        observePatientSaveAction()
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.SUBMIT_REQUEST_KEY,
            this@AddCaseActivity,
        ) { _, _ ->
            onSubmitAction()
        }
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.CANCEL_REQUEST_KEY,
            this@AddCaseActivity,
        ) { _, _ ->
            onBackPressed()
        }
    }

    private fun onSubmitAction() {
        ProgressDialogManager.show(this, "Please wait.....")
        lifecycleScope.launch {
            val questionnaireFragment =
                supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
                        as QuestionnaireFragment

            val questionnaireResponse = questionnaireFragment.getQuestionnaireResponse()
            // Print the response to the log
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val questionnaireResponseString =
                jsonParser.encodeResourceToString(questionnaireResponse)
            Log.e("response", questionnaireResponseString)
            println("Response $questionnaireResponseString")
            saveCase(questionnaireFragment.getQuestionnaireResponse(), questionnaireResponseString)
        }
    }

    private fun saveCase(
        questionnaireResponse: QuestionnaireResponse,
        questionnaireResponseString: String
    ) {

        val patientId = FormatterClass().getSharedPref("resourceId", this@AddCaseActivity)
        val questionnaire = FormatterClass().getSharedPref("questionnaire", this@AddCaseActivity)
        val encounter = FormatterClass().getSharedPref("encounterId", this@AddCaseActivity)

        when (questionnaire) {

            "measles-lab-results.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "Measles Lab Information",
                    questionnaireResponseString
                )

            "measles-lab-reg-results.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "Measles Regional Lab Information",
                    questionnaireResponseString
                )

            "afp-case-stool-lab-results.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "AFP Stool Lab Information",
                    questionnaireResponseString
                )

            "afp-itd-lab.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "AFP ITD Lab Information",
                    questionnaireResponseString
                )

            "vl-case-lab-information.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "VL Laboratory Examination",
                    questionnaireResponseString
                )

            "vl-case-sixMonthsFollowup.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "VL Follow Up Information",
                    questionnaireResponseString
                )

            "vl-case-hospitilization.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "VL Hospitalization Information",
                    questionnaireResponseString
                )

            "afp-final-lab-results.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "AFP Final Lab Information",
                    questionnaireResponseString
                )

            "afp-contact-tracing.json" -> {
                viewModel.completeContactAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    questionnaireResponseString
                )
            }
        }
    }

    private fun addQuestionnaireFragment() {
        supportFragmentManager.commit {
            add(
                R.id.add_patient_container,
                QuestionnaireFragment.builder()
                    .setQuestionnaire(viewModel.questionnaire)
                    .setShowCancelButton(true)
                    .setSubmitButtonText("Submit")
                    .build(),
                QUESTIONNAIRE_FRAGMENT_TAG,
            )
        }
    }

    private fun observePatientSaveAction() {
        viewModel.isResourcesSaved.observe(this@AddCaseActivity) {
            ProgressDialogManager.dismiss()
            if (!it) {
                Toast.makeText(
                    this@AddCaseActivity, "Please Enter all Required Fields.", Toast.LENGTH_SHORT
                )
                    .show()
                return@observe
            }

            showSuccessDialog(this@AddCaseActivity)
        }
    }

    fun showSuccessDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.success_dialog, null)
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            alertDialog.dismiss()
            this@AddCaseActivity.finish()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_finish).setOnClickListener {
            // handle finish action
            this@AddCaseActivity.finish()
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun updateArguments() {
        val json = FormatterClass().getSharedPref("questionnaire", this@AddCaseActivity)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, json)
    }

    override fun onBackPressed() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                super.onBackPressed() // Exit the activity
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog
            }
            .create()

        dialog.show()
    }

}
