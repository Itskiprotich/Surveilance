package com.icl.demo.homepage

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.icl.demo.R
import com.icl.demo.forms.ParentActivity
import com.icl.demo.models.LayoutMode
import com.icl.demo.models.NavigationNode
import com.icl.demo.models.ReportingAction
import com.icl.demo.models.ReportingConfig
import com.icl.demo.utils.FormatterClass
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SelectorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SelectorFragment : Fragment() {
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

    private val viewModel: ConfigNavigatorViewModel by viewModels()
    private var lastBackPressTime = 0L
    private val EXIT_INTERVAL = 2000L // 2 seconds

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val recycler = view.findViewById<RecyclerView>(R.id.recycler)

        // Layout mode can come from config; fallback = linear
        val isGrid = shouldUseGridLayout()
        recycler.layoutManager = if (isGrid) {
            GridLayoutManager(requireContext(), 2)
        } else {
            LinearLayoutManager(requireContext())
        }

        val adapter = ConfigRecyclerAdapter(
            onCategoryClick = { item ->
                val navigated = viewModel.navigateInto(item)
                if (!navigated) {
                    Toast.makeText(
                        requireContext(),
                        "${item.name} is coming soon",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onActionClick = { action ->
                if (action.comingSoon) {
                    Toast.makeText(
                        requireContext(),
                        "${action.label} is coming soon",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    handleAction(action)
                }
            },

            factory = DefaultViewHolderFactory(layoutInflater = layoutInflater)
        )

        recycler.adapter = adapter

        viewModel.currentNodes.observe(viewLifecycleOwner) { nodes ->
            adapter.submit(nodes)
            // Determine layout
            val layoutMode = when {
                nodes.isNotEmpty() && nodes[0] is NavigationNode.Category -> {
                    val item = (nodes[0] as NavigationNode.Category).item
                    when (item.layout?.lowercase()) {
                        "grid" -> LayoutMode.GRID
                        else -> LayoutMode.LINEAR
                    }
                }

                else -> LayoutMode.LINEAR
            }

            applyLayoutMode(recycler, layoutMode)
        }

        // Load config and start navigation
        val config = loadReportingConfig()
        viewModel.start(config.reporting)

        setupBackNavigation()

        val greeting = view.findViewById<TextView>(R.id.greetingText)
        val usernameText = view.findViewById<TextView>(R.id.usernameText)
        val time = FormatterClass().getTimeOfDay()
        val isRoot = !viewModel.canGoBack()
        if (isRoot) {
            greeting.visibility = View.VISIBLE
            // Replace with your actual user fetch
            val username = "Japheth"
            usernameText.text = username

            greeting.text = "$time,"
        } else {
            // Hide greeting + time
            greeting.visibility = View.INVISIBLE
            // Show the *current node name*
            val parentNode = viewModel.currentTitle()
            usernameText.text = parentNode
        }
        viewModel.currentTitle.observe(viewLifecycleOwner) { title ->
            if (viewModel.canGoBack()) {
                greeting.visibility = View.INVISIBLE

                usernameText.text = title     // <-- sets sub-node title correctly
            } else {
                greeting.visibility = View.VISIBLE
                usernameText.text = "Japheth"
                greeting.text = "$time,"
            }
        }

    }

    private fun applyLayoutMode(recycler: RecyclerView, mode: LayoutMode) {
        recycler.layoutManager = when (mode) {
            LayoutMode.LINEAR -> LinearLayoutManager(requireContext())
            LayoutMode.GRID -> GridLayoutManager(requireContext(), 2)
        }
    }

    private fun setupBackNavigation() {

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!viewModel.canGoBack()) {
                // You are at the root level
                val current = System.currentTimeMillis()
                if (current - lastBackPressTime < EXIT_INTERVAL) {
                    requireActivity().finish() // exit app
                } else {
                    lastBackPressTime = current
                    Snackbar.make(requireView(), "Press back again to exit", Snackbar.LENGTH_SHORT)
                        .show()
                }
            } else {
                // Normal navigation back
                viewModel.goBack()
            }
        }

    }

    private fun shouldUseGridLayout(): Boolean {
        // TODO: Dynamically read from config or arguments
        return false
    }

    private fun handleAction(action: ReportingAction) {
        when (action.type) {
            "add" -> openQuestionnaire(action.questionnaire)
            "view" -> openCasesView(action)
        }
    }

    private fun openQuestionnaire(fileName: String?) {
        if (fileName == null) return
        startActivity(Intent(requireContext(), ParentActivity::class.java))
    }

    private fun openCasesView(action: ReportingAction) {
        // TODO: Launch case list UI
    }

    private fun loadReportingConfig(): ReportingConfig {
        val jsonString = requireContext()
            .assets
            .open("app-config.json")
            .bufferedReader()
            .use { it.readText() }

        return Json.decodeFromString<ReportingConfig>(jsonString)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SelectorFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SelectorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}