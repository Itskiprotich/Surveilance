package com.imeja.surveilance.cases.child

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.imeja.surveilance.AddPatientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.imeja.surveilance.FhirApplication
import com.imeja.surveilance.R
import com.imeja.surveilance.adapters.ContactsAdapter
import com.imeja.surveilance.cases.AddCaseActivity
import com.imeja.surveilance.databinding.FragmentContactInformationBinding
import com.imeja.surveilance.helpers.FormatterClass
import com.imeja.surveilance.models.ChildItem
import com.imeja.surveilance.models.OutputGroup
import com.imeja.surveilance.models.OutputItem
import com.imeja.surveilance.models.QuestionnaireItem
import com.imeja.surveilance.viewholders.ClientDetailsViewModel
import com.imeja.surveilance.viewholders.PatientDetailsViewModelFactory
import com.imeja.surveilance.viewmodels.PatientListViewModel
import kotlin.collections.forEach

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ContactInformationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ContactInformationFragment : Fragment() {
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

    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientDetailsViewModel: ClientDetailsViewModel
    private var _binding: FragmentContactInformationBinding? = null
    private lateinit var parentLayout: LinearLayout
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentContactInformationBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    fun String.toSlug(): String {
        return this
            .trim()
            .lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
    }

    override fun onResume() {
        super.onResume()
        try {
            val encounterId = FormatterClass().getSharedPref("encounterId", requireContext())
            val currentCase = FormatterClass().getSharedPref("currentCase", requireContext())

            val patientId = FormatterClass().getSharedPref("resourceId", requireContext())
            if (currentCase != null) {
                val slug = currentCase.toSlug()
                when (slug) {
                    "measles-case-information" -> {
                        patientDetailsViewModel.getPatientDiseaseData(
                            "Measles Lab Information", "$encounterId", false
                        )
                    }

                    "afp-case-information" -> {
                        patientDetailsViewModel.getLinkedContacts(
                            "$patientId",
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

    private lateinit var groups: List<OutputGroup>
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val patientId = FormatterClass().getSharedPref("resourceId", requireContext())
        val encounterId = FormatterClass().getSharedPref("encounterId", requireContext())
        val currentCase = FormatterClass().getSharedPref("currentCase", requireContext())
        binding.apply {
            getStartedButton.setOnClickListener {
                println("Click button here ")
                    handleCase(currentCase)
            }
            fab.setOnClickListener {
                println("Click button here fab")
                handleCase(currentCase)
            }
        }
        fhirEngine = FhirApplication.fhirEngine(requireContext())
        patientDetailsViewModel =
            ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    requireActivity().application, fhirEngine, "$patientId"
                ),
            )
                .get(ClientDetailsViewModel::class.java)

        groups = parseFromAssets(requireContext())
        val recyclerView: RecyclerView = binding.patientList
        val adapter = ContactsAdapter(this::onPatientItemClicked)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(ColorDrawable(Color.LTGRAY))
            },
        )
        patientDetailsViewModel.liveLinkedData.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.lnEmpty.visibility = View.VISIBLE
            } else {
                it.forEach { item ->
                    if (item.epid.isEmpty()) {
                        item.epid = getValueBasedOnId("EPID", item.observations)
                    }
                }

                adapter.submitList(it)

                binding.lnEmpty.visibility = View.GONE
                binding.fab.visibility = View.VISIBLE
            }
        }
        if (currentCase != null) {
            val slug = currentCase.toSlug()
            when (slug) {
                "measles-case-information" -> {
                    patientDetailsViewModel.getPatientDiseaseData(
                        "Measles Lab Information",
                        "$encounterId",
                        false
                    )
                }

                "afp-case-information" -> {
                    patientDetailsViewModel.getLinkedContacts(
                        "$patientId",
                    )
                }
            }
        }


    }

    private fun handleCase(currentCase: String?) {
        if (currentCase != null) {
            val slug = currentCase.toSlug()
            when (slug) {

                "afp-case-information" -> {
                    FormatterClass()
                        .saveSharedPref(
                            "questionnaire",
                            "afp-contact-tracing.json",
                            requireContext()
                        )
                    FormatterClass().saveSharedPref(
                        "title",
                        "AFP Contact Information",
                        requireContext()
                    )
                    val intent = Intent(requireContext(), AddCaseActivity::class.java)
                    intent.putExtra(
                        QUESTIONNAIRE_FILE_PATH_KEY,
                        "afp-contact-tracing.json"
                    )
                    startActivity(intent)
                }

                else -> {
                    Toast.makeText(requireContext(), "Coming Soon!!", Toast.LENGTH_SHORT)
                        .show()
                }

            }
        } else {
            Toast.makeText(requireContext(), "please try again", Toast.LENGTH_SHORT).show()
        }

    }

    private fun handleViews(
        linkId: String,
        items: List<PatientListViewModel.ObservationItem>
    ): Boolean {
        var show = true
        val children = listOf("963850812313")
        val parent = "724335771065"
        val answerParent = items.find { it.code == parent }?.value
        if (linkId in children && answerParent != "index-006") {
            show = false
        }
        return show
    }

    private fun getValueBasedOnId(
        item: String,
        items: List<PatientListViewModel.ObservationItem>
    ): String {
        var response = ""
        items.forEach { outputItem ->
            val matchingObservation = items.find { obs ->
                obs.code == item
            }

            if (matchingObservation != null) {
                response = matchingObservation.value
            }
        }
        return response
    }

    fun parseFromAssets(context: Context): List<OutputGroup> {
        var outputGroups: List<OutputGroup> = emptyList()

        try {
            val jsonContent = context.assets.open("afp-contact-tracing.json")
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

        item.enableWhen?.firstOrNull()?.let { condition ->
            parentLink = condition.question
            val expectedAnswer = when {
                condition.answerCoding != null -> condition.answerCoding.display
                    ?: condition.answerCoding.code

                condition.answerString != null -> condition.answerString
                condition.answerBoolean != null -> condition.answerBoolean.toString()
                condition.answerDate != null -> condition.answerDate
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
                parentResponse = parentResponse
            )
            listOf(current) + children
        } else {
            children
        }
    }

    private fun createCustomField(item: OutputItem): View {
        // Create the main LinearLayout to hold the views
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }

        // Create the horizontal LinearLayout for the two TextViews
        val horizontalLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // First TextView (label)
        val label = TextView(requireContext()).apply {
            text = item.text
            textSize = 12f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        horizontalLayout.addView(label)

        // Second TextView (dynamic content)
        val tvEpiLink = TextView(requireContext()).apply {
            id = R.id.tv_epi_link // Set ID if needed for further reference
            text = item.value // Assuming item.text is the dynamic text you want to show
            textSize = 13f
            textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        horizontalLayout.addView(tvEpiLink)

        // Add the horizontal layout with two TextViews to the main layout
        layout.addView(horizontalLayout)

        // Add a separator View (divider)
        val divider = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1 // Divider thickness
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            setBackgroundColor(Color.parseColor("#CCCCCC"))
        }
        layout.addView(divider)

        return layout
    }


    private fun createCustomLabel(item: String): View {
        // Create the main LinearLayout to hold the views
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }

        // Create the horizontal LinearLayout for the two TextViews
        val horizontalLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // First TextView (label)
        val label = TextView(requireContext()).apply {
            text = item
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_200))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        horizontalLayout.addView(label)


        // Add the horizontal layout with two TextViews to the main layout
        layout.addView(horizontalLayout)

        // Add a separator View (divider)
        val divider = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1 // Divider thickness
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            setBackgroundColor(Color.parseColor("#CCCCCC"))
        }
        layout.addView(divider)

        return layout
    }


    private fun onPatientItemClicked(patientItem: PatientListViewModel.ContactResults) {


        val dialog = AlertDialog.Builder(requireContext()).create()
        val view = layoutInflater.inflate(R.layout.dialog_full_screen, null)
        val lnInfo = view.findViewById<LinearLayout>(R.id.ln_info)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)
        dialog.setView(view)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        groups = parseFromAssets(requireContext())
        lnInfo.removeAllViews()
        try {
            for (group in groups) {
                val fieldView = createCustomLabel(group.text)
//                lnInfo.addView(fieldView)
                for (item in group.items) {
                    if (item.linkId == "992818778559") {
                        item.value = patientItem.epid
                    } else {
                        item.value = getValueBasedOnId(item.linkId, patientItem.observations)
                    }
//                    val show = handleViews(item.linkId, patientItem.observations)
                    val childFieldView = createCustomField(item)
                    var show = true
                    if (!item.enable) {
                        show = false
                        show = checkIfParentAnswerMatches(
                            item.parentLink,
                            item.parentResponse,
                            group.items
                        )
                    }
                    if (show) {
                        lnInfo.addView(childFieldView)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        btnClose.apply {
            setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

    }

    private fun checkIfParentAnswerMatches(
        parentLink: String?,
        parentResponse: String?,
        items: List<OutputItem>
    ): Boolean {
        var response = false
        if (parentLink != null && parentResponse != null) {
            val parentAnswer = items.find { it.linkId == parentLink }?.value
            if (parentAnswer != null) {
                if (parentAnswer.trim() == parentResponse.trim()) {
                    response = true
                }
            }
        }
        return response
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ContactInformationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ContactInformationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}