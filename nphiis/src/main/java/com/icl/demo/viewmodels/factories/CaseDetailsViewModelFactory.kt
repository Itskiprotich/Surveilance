package com.icl.demo.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.icl.demo.viewmodels.CaseDetailsViewModel

class CaseDetailsViewModelFactory(
    private val application: Application,
    private val fhirEngine: FhirEngine,
    private val patientId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CaseDetailsViewModel::class.java)) {
            "Unknown ViewModel class"
        }
        return CaseDetailsViewModel(application, fhirEngine, patientId) as T
    }
}

//class ResponseDetailsViewModelFactory(
//    private val application: Application,
//    private val fhirEngine: FhirEngine,
//    private val questionnaireId: String,
//) : ViewModelProvider.Factory {
//    @Suppress("UNCHECKED_CAST")
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        require(modelClass.isAssignableFrom(ResponseDetailsViewModel::class.java)) {
//            "Unknown ViewModel class"
//        }
//        return ResponseDetailsViewModel(application, fhirEngine, questionnaireId) as T
//    }
//}
