package com.imeja.surveilance.viewholders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.search
import com.imeja.surveilance.viewmodels.PatientListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.let

class ResponseDetailsViewModel(
    application: Application,
    private val fhirEngine: FhirEngine,
    private val questionnaireId: String,
) : AndroidViewModel(application) {
    val liveSummaryData = MutableLiveData<PatientListViewModel.CaseDetailSummaryData>()
    fun getInfoSummaryData(slug: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val patientData = getInfoSummary(slug)
            withContext(Dispatchers.Main) { liveSummaryData.value = patientData }
        }
    }

    private suspend fun getInfoSummary(slug: String): PatientListViewModel.CaseDetailSummaryData {

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)


        val searchResult =
            fhirEngine.search<QuestionnaireResponse> {
                filter(
                    Resource.RES_ID,
                    { value = of(questionnaireId) })
            }
        var logicalId = ""
        var observations = mutableListOf<PatientListViewModel.ObservationItem>()
        searchResult.first().let {
            logicalId = it.resource.logicalId

            it.resource.item.forEach { k ->
                k.item.forEach { j ->
                    val answer = j.answerFirstRep
                    val value = when {
                        answer.hasValueReference() -> {
                            val ref = answer.valueReference
                            ref.display ?: ref.reference ?: ""
                        }

                        answer.hasValueCoding() -> {
                            val coding = answer.valueCoding
                            coding.display ?: coding.code ?: ""
                        }

                        answer.hasValueStringType() -> answer.valueStringType.value ?: ""
                        answer.hasValueDateType() -> {
                            val date = answer.valueDateType.value
                            date?.let { dateFormatter.format(it) } ?: ""
                        }

                        answer.hasValueDateTimeType() -> {
                            val dateTime = answer.valueDateTimeType.value
                            dateTime?.let { dateFormatter.format(it) } ?: ""
                        }

                        answer.hasValueBooleanType() -> answer.valueBooleanType.booleanValue()
                            .toString()

                        answer.hasValueIntegerType() -> answer.valueIntegerType.value.toString()
                        answer.hasValueDecimalType() -> answer.valueDecimalType.value.toString()
                        else -> answer.value?.primitiveValue() ?: ""
                    }


                    val obs = PatientListViewModel.ObservationItem(
                        id = j.linkId,
                        code = j.linkId,
                        value = value ?: "",
                        created = j.linkId
                    )
                    observations.add(obs)
                }
            }

        }
        return PatientListViewModel.CaseDetailSummaryData(
            logicalId = logicalId,
            encounterId = "encounterId",
            name = "name",
            dob = "dob",
            sex = "sex",
            observations = observations,
            epidNo = "epidNo"
        )
    }
}