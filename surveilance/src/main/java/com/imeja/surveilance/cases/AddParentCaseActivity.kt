package com.imeja.surveilance.cases

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.material.button.MaterialButton
import com.imeja.surveilance.AddPatientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.imeja.surveilance.AddPatientFragment.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import com.imeja.surveilance.R
import com.imeja.surveilance.databinding.ActivityAddParentCaseBinding
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.helpers.LocationUtils
import com.imeja.surveilance.helpers.ProgressDialogManager
import com.imeja.surveilance.viewmodels.AddClientViewModel
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

class AddParentCaseActivity : AppCompatActivity() {
    private val viewModel: AddClientViewModel by viewModels()
    private lateinit var binding:
            ActivityAddParentCaseBinding // Binding class name is based on layout file name

    private fun getStringFromAssets(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddParentCaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val titleName = FormatterClass().getSharedPref("AddParentTitle", this@AddParentCaseActivity)
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
            this@AddParentCaseActivity,
        ) { _, _ ->
            onSubmitAction()
        }
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.CANCEL_REQUEST_KEY,
            this@AddParentCaseActivity,
        ) { _, _ ->
            onBackPressed()
        }
    }

    private fun onSubmitActionSubmit() {
        lifecycleScope.launch {
            val fragment = supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
                    as QuestionnaireFragment
            val questionnaireResponse = fragment.getQuestionnaireResponse()

            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val questionnaireResponseString =
                jsonParser.encodeResourceToString(questionnaireResponse)
            Log.d("extraction response", questionnaireResponseString)

            val questionnaire =
                jsonParser.parseResource(viewModel.questionnaireJson) as Questionnaire

            Log.d("Questionnaire Response::::", "$questionnaire")
            Log.d("Questionnaire Response::::: ", "$questionnaireResponse")
            val bundle = ResourceMapper.extract(questionnaire, questionnaireResponse)
            Log.d("Questionnaire Response::::", jsonParser.encodeResourceToString(bundle))
        }
    }

    private fun onSubmitAction() {
        ProgressDialogManager.show(this, "Please Wait.....")
        lifecycleScope.launch {
            val questionnaireFragment =
                supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
                        as QuestionnaireFragment

            val questionnaireResponse = questionnaireFragment.getQuestionnaireResponse()
            // Print the response to the log
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val questionnaireResponseString =
                jsonParser.encodeResourceToString(questionnaireResponse)

            saveCase(questionnaireFragment.getQuestionnaireResponse(), questionnaireResponseString)
        }
    }


    private fun showCancelScreenerQuestionnaireAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(getString(R.string.cancel_questionnaire_message))
            setPositiveButton(getString(android.R.string.yes)) { _, _ ->
                this@AddParentCaseActivity.finish()
            }
            setNegativeButton(getString(android.R.string.no)) { _, _ -> }
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun saveCase(
        questionnaireResponse: QuestionnaireResponse,
        questionnaireResponseString: String
    ) {
        val case = FormatterClass().getSharedPref("currentCase", this@AddParentCaseActivity)
        // print case
        Log.d("Questionnaire Response::::", "$case")
        when (case) {
            "Mpox - Supervisor Checklist" -> {
                viewModel.saveUserResponse(questionnaireResponse, case, this@AddParentCaseActivity)

            }

            else -> {
                viewModel.savePatientData(
                    questionnaireResponse,
                    questionnaireResponseString,
                    this@AddParentCaseActivity
                )
            }
        }

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

    private fun addQuestionnaireFragment() {
        supportFragmentManager.commit {
            add(
                R.id.add_patient_container,
                QuestionnaireFragment.builder()
                    .setQuestionnaire(viewModel.questionnaireJson)
                    .setShowCancelButton(true)
                    .setSubmitButtonText("Submit")
                    .build(),
                QUESTIONNAIRE_FRAGMENT_TAG,
            )
        }
    }

    private fun observePatientSaveAction() {
        viewModel.isPatientSaved.observe(this) {
            ProgressDialogManager.dismiss()

            if (!it) {
                Toast.makeText(this, "Please Enter all Required Fields.", Toast.LENGTH_SHORT).show()
                return@observe
            }
            showSuccessDialog(this@AddParentCaseActivity)

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
            this@AddParentCaseActivity.finish()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_finish).setOnClickListener {
            // handle finish action
            this@AddParentCaseActivity.finish()
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun updateArguments() {
        val json = FormatterClass().getSharedPref("questionnaire", this@AddParentCaseActivity)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, json)
    }

    override fun onSupportNavigateUp(): Boolean {
        showCancelScreenerQuestionnaireAlertDialog()
        return true
    }
}