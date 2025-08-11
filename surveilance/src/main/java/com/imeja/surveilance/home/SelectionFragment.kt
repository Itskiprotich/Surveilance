package com.imeja.surveilance.home


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.imeja.surveilance.AddPatientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.imeja.surveilance.FhirApplication
import com.imeja.surveilance.R
import com.imeja.surveilance.adapters.CaseOptionsAdapter
import com.imeja.surveilance.cases.AddParentCaseActivity
import com.imeja.surveilance.cases.CaseListingActivity
import com.imeja.surveilance.viewmodels.PatientListViewModel
import com.imeja.surveilance.databinding.FragmentSelectionBinding
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.helpers.SelectionBottomSheet
import com.imeja.surveilance.models.CaseOption
import com.imeja.surveilance.viewmodels.HomeViewModel

import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CaseSelectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SelectionFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private var _binding: FragmentSelectionBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientListViewModel: PatientListViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSelectionBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleName = FormatterClass().getSharedPref("grandTitle", requireContext())


        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        // Let the Fragment receive menu callbacks
        setHasOptionsMenu(true)

        fhirEngine = FhirApplication.fhirEngine(requireContext())
        patientListViewModel = ViewModelProvider(
            this,
            PatientListViewModel.PatientListViewModelFactory(
                requireActivity().application, fhirEngine
            ),
        ).get(PatientListViewModel::class.java)

        binding.apply {
            greeting.text = titleName
        }
        if (titleName != null) {
            setupRecyclerView()
        }


        childFragmentManager.setFragmentResultListener(
            SelectionBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val option = FormatterClass().getSharedPref("selected_option", requireContext())
            when (bundle.getInt(SelectionBottomSheet.ARG_CHOICE)) {
                SelectionBottomSheet.CHOICE_COUNTY -> {
                    val isMpox = option == "Add New Mpox Case"

                    val currentCase =
                        if (isMpox) "Mpox - Tally Sheet" else "RCCE - County/Subcounty Interface"
                    val addParentTitle =
                        if (isMpox) "Mpox - Tally Sheet" else "County/Subcounty Interface"
                    val questionnaireFile =
                        if (isMpox) "mpox-tally-sheet.json" else "social-county.json"

                    with(FormatterClass()) {
                        saveSharedPref("currentCase", currentCase, requireContext())
                        saveSharedPref("AddParentTitle", addParentTitle, requireContext())
                        saveSharedPref("questionnaire", questionnaireFile, requireContext())
                    }
                    launchCaseFlow(
                        requireContext(),
                        " $titleName",
                        currentCase,
                        addParentTitle,
                        questionnaireFile
                    )

                }

                SelectionBottomSheet.CHOICE_COMMUNITY -> {
                    val isMpox = option == "Add New Mpox Case"

                    val currentCase =
                        if (isMpox) "Mpox - Supervisor Checklist" else "RCCE - Community Questionnaire"
                    val addParentTitle =
                        if (isMpox) "Mpox - Supervisor Checklist" else "Community Questionnaire"
                    val questionnaireFile =
                        if (isMpox) "mpox-supervisor-checklist.json" else "social-community.json"

                    with(FormatterClass()) {
                        saveSharedPref("currentCase", currentCase, requireContext())
                        saveSharedPref("AddParentTitle", addParentTitle, requireContext())
                        saveSharedPref("questionnaire", questionnaireFile, requireContext())
                    }
                    launchCaseFlow(
                        requireContext(),
                        " $titleName",
                        currentCase,
                        addParentTitle,
                        questionnaireFile
                    )

                }


            }
        }

    }

    private fun launchCaseFlow(
        context: Context,
        titleName: String,
        currentCase: String,
        addParentTitle: String,
        questionnaireFile: String
    ) {
        with(FormatterClass()) {
            saveSharedPref("currentCase", currentCase, context)
            saveSharedPref("AddParentTitle", addParentTitle, context)
            saveSharedPref("questionnaire", questionnaireFile, context)
        }

        val intent = Intent(context, AddParentCaseActivity::class.java).apply {
            putExtra("AddParentTitle", " $titleName")
            putExtra(QUESTIONNAIRE_FILE_PATH_KEY, questionnaireFile)
        }
        startActivity(intent)
    }

    private fun setupRecyclerView() {
        val titleName = FormatterClass().getSharedPref("grandTitle", requireContext())

        val title = when (titleName) {
            "Visceral Leishmaniasis (Kala-azar) Case Management Form" -> "VL"
            "Visceral Leishmaniasis Case Management Form" -> "VL"
            "Social Listening and Rumor Tracking Tool" -> "SLR"
            "RCCE Tools" -> "RCCE"
            else -> titleName
        }
        val add = when (title) {
            "SLR" -> "Add New Report"
            "MOH 505" -> "Add New Record"
            "Social Investigation Form" -> "Add New Social Investigation Form"
            "Summary Sheet" -> "Add New Team Record"
            else -> "Add New $title Case"
        }
        val view = when (title) {
            "SLR" -> "View Reported Cases"
            "Social Investigation Form" -> "Social Investigation Reports"
            else -> "$title Case List"
        }
        val caseOptions = mutableListOf(
            CaseOption(add), CaseOption(
                view, showCount = true, count = 0
            )
        )

        val recyclerView = requireView().findViewById<RecyclerView>(R.id.sdcLayoutsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        FormatterClass().deleteSharedPref("selected_option", requireContext())
        recyclerView.adapter = CaseOptionsAdapter(caseOptions) { option ->
            when (option.title) {
                "Add New Team Record" -> {
                    val currentCase = "Mpox - Tally Sheet"
                    val addParentTitle = "Add New Team Record"
                    val questionnaireFile = "mpox-tally-sheet.json"

                    with(FormatterClass()) {
                        saveSharedPref("currentCase", currentCase, requireContext())
                        saveSharedPref("AddParentTitle", addParentTitle, requireContext())
                        saveSharedPref("questionnaire", questionnaireFile, requireContext())
                    }
                    launchCaseFlow(
                        requireContext(),
                        " $titleName",
                        currentCase,
                        addParentTitle,
                        questionnaireFile
                    )
                }

                "Add New Supervisor Checklist Case" -> {
                    val currentCase = "Mpox - Supervisor Checklist"
                    val addParentTitle = "Add New Supervisor Checklist"
                    val questionnaireFile = "mpox-supervisor-checklist.json"

                    with(FormatterClass()) {
                        saveSharedPref("currentCase", currentCase, requireContext())
                        saveSharedPref("AddParentTitle", addParentTitle, requireContext())
                        saveSharedPref("questionnaire", questionnaireFile, requireContext())
                    }
                    launchCaseFlow(
                        requireContext(),
                        " $titleName",
                        currentCase,
                        addParentTitle,
                        questionnaireFile
                    )
                }

                "Add New Mpox Case" -> {
                    FormatterClass().saveSharedPref(
                        "selected_option",
                        "Add New Mpox Case",
                        requireContext()
                    )
                    val sheet =
                        SelectionBottomSheet.newInstance("Tally Sheet", "Supervisor Checklist")
                    sheet.show(childFragmentManager, "SelectionBottomSheet")
                }

                "Add New Social Investigation Form" -> {
                    SelectionBottomSheet.show(childFragmentManager)

                }

                "Social Investigation Reports" -> {
                    FormatterClass().saveSharedPref(
                        "listingTitle", "Social Investigation Form", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase", "RCCE", requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "Add New Record" -> {
                    FormatterClass().saveSharedPref(
                        "currentCase", "MOH 505 Reporting Form", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "AddParentTitle",
                        "MOH 505 Reporting Form",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "questionnaire", "moh505.json", requireContext()
                    )
                    val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
                    intent.putExtra("AddParentTitle", " $titleName")
                    intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "moh505.json")
                    startActivity(intent)
                }

                "MOH 505 Case List" -> {
                    FormatterClass().saveSharedPref(
                        "listingTitle", "MOH 505 Reporting Form", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase", "MOH 505 Reporting Form", requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "Summary Sheet Case List" -> {
                    FormatterClass().saveSharedPref(
                        "listingTitle", "Mpox - Summary Sheet", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase", "Mpox - Tally Sheet", requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "Supervisor Checklist Case List" -> {
                    FormatterClass().saveSharedPref(
                        "listingTitle", "Mpox - Supervisor Checklist", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase", "Mpox - Supervisor Checklist", requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "Add New Report" -> {
                    FormatterClass().saveSharedPref(
                        "currentCase", "Social Listening and Rumor Tracking Tool", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "AddParentTitle",
                        "Social Listening and Rumor Tracking Tool",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "questionnaire", "rumor-tracking-case.json", requireContext()
                    )
                    val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
                    intent.putExtra("AddParentTitle", " $titleName")
                    intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "rumor-tracking-case.json")
                    startActivity(intent)
                }

                "Add New VL Case" -> {
                    FormatterClass().saveSharedPref(
                        "currentCase", "VL Case Information", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "AddParentTitle",
                        "Visceral Leishmaniasis Case Management Form",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "questionnaire", "vl-case.json", requireContext()
                    )
                    val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
                    intent.putExtra("AddParentTitle", " $titleName")
                    intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "vl-case.json")
                    startActivity(intent)
                }

                "Mpox Case List" -> {
                    FormatterClass().saveSharedPref(
                        "listingTitle", " ${option.title}", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase", "Mpox Information", requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "VL Case List" -> {
                    FormatterClass().saveSharedPref(
                        "listingTitle", " ${option.title}", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase", "VL Case Information", requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "View Reported Cases" -> {
                    FormatterClass().saveSharedPref(
                        "listingTitle", "Social Listening and Rumor Tracking Tool", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase", "Social Listening and Rumor Tracking Tool", requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "Add New AFP Case" -> {

                    FormatterClass().saveSharedPref(
                        "currentCase", "AFP Case Information", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "AddParentTitle", "Add $titleName Case", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "questionnaire", "afp-case.json", requireContext()
                    )
                    val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
                    intent.putExtra("AddParentTitle", "Add $titleName Case")
                    intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "afp-case.json")
                    startActivity(intent)
                }

                "Add New Measles Case" -> {

                    FormatterClass().saveSharedPref(
                        "currentCase", "Measles Case Information", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "AddParentTitle", "Add $titleName Case", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "questionnaire", "add-case.json", requireContext()
                    )
                    val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
                    intent.putExtra("AddParentTitle", "Add $titleName Case")
                    intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "add-case.json")
                    startActivity(intent)
                }

                "Measles Case List" -> {
                    FormatterClass().saveSharedPref(
                        "listingTitle", " ${option.title}", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase", "Measles Case Information", requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "AFP Case List" -> {
                    FormatterClass().saveSharedPref(
                        "listingTitle", " ${option.title}", requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase", "AFP Case Information", requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                else -> {
                    Toast.makeText(requireContext(), "Coming Soon", Toast.LENGTH_SHORT).show()
                }
            }


        }
        println("Case Type: $title")
        val caseType = when (title?.trim()) {
            "Measles" -> "measles-case-information"
            "AFP" -> "afp-case-information"
            "VL" -> "vl-case-information"
            "SLR" -> "social-listening-and-rumor-tracking-tool"
            "MOH 505" -> "moh-505-reporting-form"
            "RCCE" -> "rcce"
            "Mpox" -> "mpox-information"
            "Tally Sheet" -> "mpox-tally-sheet"
            "Supervisor Checklist" -> "mpox-supervisor-checklist"
            else -> null
        }
        println("Case Type: $caseType")

        caseType?.let {
            try {

                patientListViewModel.handleCurrentCaseListing(it)

                patientListViewModel.liveSearchedCases.observe(viewLifecycleOwner) { cases ->
                    caseOptions[1] = caseOptions[1].copy(count = cases.size)
                    recyclerView.adapter?.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button in the toolbar
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CaseSelectionFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) = SelectionFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PARAM1, param1)
                putString(ARG_PARAM2, param2)
            }
        }
    }
}