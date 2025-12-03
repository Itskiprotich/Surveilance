package com.icl.demo.forms

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.material.button.MaterialButton
import com.icl.demo.R
import com.icl.demo.databinding.ActivityParentBinding
import com.icl.demo.location.ContribQuestionnaireItemViewHolderFactoryMatchersProviderFactory
import com.icl.demo.utils.FormatterClass
import com.icl.demo.utils.ProgressDialogManager
import com.icl.demo.utils.UserRole
import com.icl.demo.viewmodels.AddCaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType

class ParentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityParentBinding
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val viewModel: AddCaseViewModel by viewModels()
    private fun getStringFromAssets(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityParentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val titleName = FormatterClass().getSharedPref("AddParentTitle", this@ParentActivity)
        supportActionBar?.apply {
            title = "$titleName"
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        updateArguments()
        if (savedInstanceState == null) {
            addQuestionnaireFragment()
        }
        observePatientSaveAction()

        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.SUBMIT_REQUEST_KEY,
            this@ParentActivity,
        ) { _, _ ->
            onSubmitAction()
        }
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.CANCEL_REQUEST_KEY,
            this@ParentActivity,
        ) { _, _ ->
            onBackPressed()
        }
    }

    private fun onSubmitAction() {
        ProgressDialogManager.show(this, "Please Wait.....")
        lifecycleScope.launch {
            val questionnaireFragment =
                supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
                        as QuestionnaireFragment
            saveCase(questionnaireFragment.getQuestionnaireResponse())
        }
    }

    private fun showCancelScreenerQuestionnaireAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(getString(R.string.cancel_questionnaire_message))
            setPositiveButton(getString(android.R.string.yes)) { _, _ ->
                this@ParentActivity.finish()
            }
            setNegativeButton(getString(android.R.string.no)) { _, _ -> }
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun saveCase(
        questionnaireResponse: QuestionnaireResponse
    ) {
        val case = FormatterClass().getSharedPref("currentCase", this@ParentActivity)

        when (case) {
            "Mpox - Supervisor Checklist" -> {
                viewModel.saveUserResponse(questionnaireResponse, case, this@ParentActivity)

            }

            else -> {
                viewModel.savePatientData(
                    questionnaireResponse,
                    this@ParentActivity
                )
            }
        }

    }

    private fun addUserCountyResponse(
        userCounty: String,
        county: String
    ): QuestionnaireResponse.QuestionnaireResponseItemComponent {
        val formatter = FormatterClass()
        val county = formatter.getSharedPref(county, this@ParentActivity)
        val item =
            QuestionnaireResponse.QuestionnaireResponseItemComponent()

        item.linkId = userCounty
        item.answerFirstRep.value = StringType(county)
        return item

    }

    fun createCountyAnswer(
        ref: String,
        dis: String,
        id: String,
        label: String
    ): QuestionnaireResponse.QuestionnaireResponseItemComponent {

        val reference = Reference().apply {
            reference = "Location/$ref"
            display = dis
        }

        return QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = id
            text = label
            answerFirstRep.value = reference
        }
    }

    fun getAssignedLocation(type: String): String {
        var value = ""
        val source = FormatterClass().getSharedPref(type, this@ParentActivity)
        if (source != null) {
            value = source
        }

        return value
    }

    private fun addQuestionnaireFragment() {
        lifecycleScope.launch(Dispatchers.Default) {

            // Prepare FHIR context ONCE in background
            val fhirContext = FhirContext.forR4Cached()
            val jsonParser = fhirContext.newJsonParser()

            // 1. Build QuestionnaireResponse object
            val resource = QuestionnaireResponse()
            val formatter = FormatterClass()
            val storedRole = formatter.getSharedPref("practitionerRole", this@ParentActivity)
            val userRole = UserRole.fromAny(storedRole ?: "")

            when (userRole) {

                UserRole.ADMINISTRATOR -> {
                    val childGroup =
                        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                            linkId = "151479012557"
                            text = "Reporting Site"
                        }
                    childGroup.addItem(
                        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                            linkId = "user_role"
                            text = "User Role"
                            answerFirstRep.value = StringType("ADMINISTRATOR")
                        }
                    )
                    resource.addItem(childGroup)
                }

                UserRole.COUNTY_DISEASE_SURVEILLANCE_OFFICER -> {
                    val childGroup =
                        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                            linkId = "151479012557"
                            text = "Reporting Site"
                        }
                    childGroup.addItem(
                        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                            linkId = "user_role"
                            text = "User Role"
                            answerFirstRep.value = StringType("VACCINATOR")
                        }
                    )

                    val userCounty = addUserCountyResponse("user_county", "county")
                    childGroup.addItem(userCounty)
                    resource.addItem(childGroup)
                }

                UserRole.SUBCOUNTY_DISEASE_SURVEILLANCE_OFFICER -> {
                    val childGroup =
                        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                            linkId = "variables"
                            text = "Variables"
                        }
                    childGroup.addItem(
                        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                            linkId = "user_role"
                            text = "User Role"
                            answerFirstRep.value = StringType("VACCINATOR")
                        }
                    )

                    childGroup.addItem(addUserCountyResponse("user_county", "county"))
                    childGroup.addItem(addUserCountyResponse("user_sub_county", "subCounty"))

                    resource.addItem(childGroup)
                }

                UserRole.FACILITY_SURVEILLANCE_FOCAL_PERSON,
                UserRole.SUPERVISOR,
                UserRole.VACCINATOR -> {

                    val childGroup =
                        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                            linkId = "151479012557"
                            text = "Reporting Site"
                        }

                    childGroup.addItem(
                        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                            linkId = "user_role"
                            text = "User Role"
                            answerFirstRep.value = StringType("VACCINATOR")
                        }
                    )

                    val county = createCountyAnswer(
                        getAssignedLocation("county"),
                        getAssignedLocation("countyName"),
                        "294367770999", "County"
                    )

                    val subCounty = createCountyAnswer(
                        getAssignedLocation("subCounty"),
                        getAssignedLocation("subCountyName"),
                        "819946803642", "Sub County"
                    )

                    val ward = createCountyAnswer(
                        getAssignedLocation("ward"),
                        getAssignedLocation("wardName"),
                        "819943434", "Ward"
                    )

                    val facility = createCountyAnswer(
                        getAssignedLocation("facility"),
                        getAssignedLocation("facilityName"),
                        "819946803677", "Health Facility"
                    )

                    childGroup.addItem(county)
                    childGroup.addItem(subCounty)
                    childGroup.addItem(ward)
                    childGroup.addItem(facility)

                    resource.addItem(childGroup)
                }

                else -> { /* No-op */
                }
            }

            // 2. Serialize resource INTO JSON (expensive → done on background thread)
            val questionnaireResponseJson = jsonParser.encodeResourceToString(resource)

            // 3. Prepare questionnaire JSON (if it’s very large)
            val questionnaireJson = viewModel.questionnaireJson

            // 4. Return to main thread for fragment transaction
            withContext(Dispatchers.Main) {
                if (supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) == null) {
                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        val fragmentBuilder = QuestionnaireFragment.builder().apply {
                            setShowSubmitAnywayButton(false)
                            setQuestionnaireResponse(questionnaireResponseJson)
                            setCustomQuestionnaireItemViewHolderFactoryMatchersProvider(
                                ContribQuestionnaireItemViewHolderFactoryMatchersProviderFactory
                                    .LOCATION_WIDGET_PROVIDER
                            )
                            setQuestionnaire(questionnaireJson)
                        }
                        add(
                            R.id.add_patient_container,
                            fragmentBuilder.build(),
                            QUESTIONNAIRE_FRAGMENT_TAG
                        )
                    }
                }
            }
        }
    }

    private fun observePatientSaveAction() {
        viewModel.isPatientSaved.observe(this) {
            ProgressDialogManager.dismiss()

            if (!it) {
                Toast.makeText(this, "Please Enter all Required Fields.", Toast.LENGTH_SHORT).show()
                return@observe
            }
            showSuccessDialog(this@ParentActivity)

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
            this@ParentActivity.finish()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_finish).setOnClickListener {
            // handle finish action
            this@ParentActivity.finish()
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun updateArguments() {
        val json = FormatterClass().getSharedPref("questionnaire", this@ParentActivity)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, json)
    }

    override fun onSupportNavigateUp(): Boolean {
        showCancelScreenerQuestionnaireAlertDialog()
        return true
    }

    companion object {
        const val QUESTIONNAIRE_FILE_PATH_KEY = "questionnaire-file-path-key"
        const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    }
}