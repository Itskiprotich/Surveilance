/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.icl.demo.data

import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.revInclude
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.SyncDataParams
import com.google.android.fhir.sync.download.DownloadRequest
import com.icl.demo.DemoDataStore
import com.icl.demo.utils.FormatterClass
import com.icl.demo.utils.LocationLevel
import com.icl.demo.utils.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType


class TimestampBasedDownloadWorkManagerImpl(
    private val dataStore: DemoDataStore,
    val context: Context,
    val fhirEngine: FhirEngine
) : DownloadWorkManager {
    private val resourceTypeList = ResourceType.values().map { it.name }

    private var urls: LinkedList<String> = LinkedList()
    private val generalResources = LinkedList(
        listOf(
            "Practitioner?_sort=_lastUpdated",
        )
    )


    init {
        getRespectiveFilteredResources(context) { filteredUrls ->
            urls = LinkedList<String>().apply {
                addAll(generalResources)
                addAll(filteredUrls)
            }
        }
    }

    override suspend fun getNextRequest(): DownloadRequest? {
        var url = urls.poll() ?: return null

        val resourceTypeToDownload =
            ResourceType.fromCode(url.findAnyOf(resourceTypeList, ignoreCase = true)!!.second)
        dataStore.getLasUpdateTimestamp(resourceTypeToDownload)?.let {
            url = affixLastUpdatedTimestamp(url, it)
        }
        return DownloadRequest.of(url)
    }

    override suspend fun getSummaryRequestUrls(): Map<ResourceType, String> {
        return urls.associate { url ->
            val resourceType = ResourceType.fromCode(url.substringBefore("?"))
            if (resourceType == ResourceType.Patient) {
                resourceType to url.plus("&${SyncDataParams.SUMMARY_KEY}=${SyncDataParams.SUMMARY_COUNT_VALUE}")
            } else {
                resourceType to url
            }
        }
    }

    override suspend fun processResponse(response: Resource): Collection<Resource> {
        // As per FHIR documentation :
        // If the search fails (cannot be executed, not that there are no matches), the
        // return value SHALL be a status code 4xx or 5xx with an OperationOutcome.
        // See https://www.hl7.org/fhir/http.html#search for more details.
        if (response is OperationOutcome) {
            throw FHIRException(response.issueFirstRep.diagnostics)
        }

        // If the resource returned is a List containing Patients, extract Patient references and fetch
        // all resources related to the patient using the $everything operation.
        if (response is ListResource) {

            for (entry in response.entry) {

                val reference = Reference(entry.item.reference)
                if (reference.referenceElement.resourceType.equals("Patient")) {
                    val patientUrl = "${entry.item.reference}/\$everything"
                    urls.add(patientUrl)
                }

            }
        }

        // If the resource returned is a Bundle, check to see if there is a "next" relation referenced
        // in the Bundle.link component, if so, append the URL referenced to list of URLs to download.
        if (response is Bundle) {
            for (entry in response.entry) {
                val type = entry.resource.resourceType.toString()
                if (type == "Patient") {
                    val patientUrl = "${entry.fullUrl}/\$everything"
                    urls.add(patientUrl)
                }
            }

            val nextUrl =
                response.link.firstOrNull { component -> component.relation == "next" }?.url
            if (nextUrl != null) {
                urls.add(nextUrl)
            }
        }

        // Finally, extract the downloaded resources from the bundle.
        var bundleCollection: Collection<Resource> = mutableListOf()
        if (response is Bundle && response.type == Bundle.BundleType.SEARCHSET) {
            bundleCollection = response.entry.map { it.resource }
                .also { extractAndSaveLastUpdateTimestampToFetchFutureUpdates(it) }
        }
        return bundleCollection
    }

    private suspend fun extractAndSaveLastUpdateTimestampToFetchFutureUpdates(
        resources: List<Resource>,
    ) {
        resources.groupBy { it.resourceType }.entries.map { map ->
            dataStore.saveLastUpdatedTimestamp(
                map.key,
                map.value.maxOfOrNull { it.meta.lastUpdated }?.toTimeZoneString() ?: "",
            )
        }
    }

    fun getFacilitiesByLevel(
        startId: String,
        level: LocationLevel,
        onResult: (List<String>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val facilityIds = mutableListOf<String>()
                val locationIdsToProcess = mutableListOf(startId)

                when (level) {
                    LocationLevel.FACILITY -> {
                        // Already at facility level
                        facilityIds.add(startId)
                    }

                    LocationLevel.WARD -> {
                        for (wardId in locationIdsToProcess) {
                            // Step 1: Try loading from SharedPreferences
                            val cachedFacilityIds = FormatterClass().getFacilityIds(context, wardId)
                            if (cachedFacilityIds != null && cachedFacilityIds.isNotEmpty()) {
                                facilityIds.addAll(cachedFacilityIds)
                                println("Loaded ${cachedFacilityIds.size} facilities for ward $wardId from cache")
                                continue
                            }

                            // Step 2: If not cached, fetch from FHIR engine
                            val facilities = fhirEngine.search<Location> {
                                filter(Location.PARTOF, { value = "Location/$wardId" })
                            }
                            val fetchedIds = facilities.map { it.resource.logicalId }

                            // Step 3: Save fetched IDs to SharedPreferences
                            FormatterClass().saveFacilityIds(context, wardId, fetchedIds)

                            facilityIds.addAll(fetchedIds)
                            println("Fetched ${fetchedIds.size} facilities for ward $wardId from DB")
                        }
                    }

                    LocationLevel.SUB_COUNTY -> {
                        // Check cache first
                        val cachedFacilities =
                            FormatterClass().getFacilityIdsForWard(context, startId)
                        if (cachedFacilities != null && cachedFacilities.isNotEmpty()) {
                            println("✅ Using cached facilities for SubCounty $startId — ${cachedFacilities.size} records found")
                            facilityIds.addAll(cachedFacilities)
                        } else {

                            println("🔍 Cache miss for SubCounty $startId — running FHIR query")

                            // Single FHIR query using revInclude (wards + facilities)
                            val wards = fhirEngine.search<Location> {
                                filter(Location.PARTOF, { value = "Location/$startId" })
                                revInclude<Location>(Location.PARTOF)
                            }

                            println("Total Wards:::: ${wards.size} for Location $startId")

                            val allFacilityIds = mutableListOf<String>()

                            if (wards.isNotEmpty()) {
                                wards.forEach { ward ->
                                    ward.revIncluded?.get(ResourceType.Location to Location.PARTOF.paramName)
                                        ?.let { facilities ->
                                            println("🏥 Facilities found under ward ${ward.resource.name}: ${facilities.size}")
                                            allFacilityIds.addAll(facilities.map { it.logicalId })
                                        }
                                }
                            }

                            println("💾 Caching ${allFacilityIds.size} facilities for SubCounty $startId")
                            FormatterClass().saveFacilityIdsForWard(
                                context,
                                startId,
                                allFacilityIds
                            )

                            facilityIds.addAll(allFacilityIds)
                        }
                    }


                    LocationLevel.COUNTY -> {
                        val cachedFacilities =
                            FormatterClass().getFacilityIdsForWard(context, startId)
                        val facilityIds = mutableListOf<String>()

                        if (cachedFacilities != null && cachedFacilities.isNotEmpty()) {
                            println("✅ Using cached facilities for County $startId — ${cachedFacilities.size} records found")
                            facilityIds.addAll(cachedFacilities)
                        } else {
                            println("🔍 Cache miss for County $startId — running FHIR query")

                            // Step 1: Get all sub-counties under this county
                            val subCounties = fhirEngine.search<Location> {
                                filter(Location.PARTOF, { value = "Location/$startId" })
                                revInclude<Location>(Location.PARTOF)
                            }
                            println("📍 Found ${subCounties.size} sub-counties for County $startId")

                            val allFacilityIds = mutableListOf<String>()

                            // Step 2: Loop through each sub-county and get wards + facilities
                            subCounties.forEach { subCounty ->
                                val wards =
                                    subCounty.revIncluded?.get(ResourceType.Location to Location.PARTOF.paramName)
                                wards?.forEach { ward ->
                                    val facilities = fhirEngine.search<Location> {
                                        filter(
                                            Location.PARTOF,
                                            { value = "Location/${ward.logicalId}" })
                                    }
                                    allFacilityIds.addAll(facilities.map { it.resource.logicalId })
                                }
                            }

                            println("💾 Caching ${allFacilityIds.size} facilities for County $startId")
                            FormatterClass().saveFacilityIdsForWard(
                                context,
                                startId,
                                allFacilityIds
                            )
                            facilityIds.addAll(allFacilityIds)
                        }
                    }

                    LocationLevel.NATIONAL -> {
                        // Get all counties first
                        val counties = fhirEngine.search<Location> {
                            // Optional: Add filter by type = "county" if available
                        }

                        val countyIds = counties.map { it.resource.logicalId }

                        for (countyId in countyIds) {
                            // Recursive call for each county
                            getFacilitiesByLevel(countyId, LocationLevel.COUNTY) { ids ->
                                facilityIds.addAll(ids)
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(facilityIds)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        }
    }


    fun getRespectiveFilteredResources(
        context: Context, onResult: (LinkedList<String>) -> Unit
    ) {
        val formatter = FormatterClass()
        val storedRole = formatter.getSharedPref("practitionerRole", context)
        val userRole = UserRole.fromAny(storedRole ?: "")

        when (userRole) {
            UserRole.FACILITY_SURVEILLANCE_FOCAL_PERSON, UserRole.SUPERVISOR, UserRole.VACCINATOR -> {
                val facilityId = formatter.getSharedPref("facility", context)
                val urls = if (facilityId != null) {
                    listOf(
                        "Patient?_tag=Location/$facilityId&_sort=_lastUpdated",
                        "QuestionnaireResponse?_tag=Location/$facilityId&_sort=_lastUpdated",
                        "MeasureReport?_tag=Location/$facilityId&_sort=_lastUpdated",
                        "Encounter?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500",
                        "Observation?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                    )
                } else emptyList()

                onResult(LinkedList(urls))
            }

            UserRole.SUBCOUNTY_DISEASE_SURVEILLANCE_OFFICER -> {
                val subCounty = formatter.getSharedPref("subCounty", context)
                if (subCounty != null) {
                    getFacilitiesByLevel(subCounty, LocationLevel.SUB_COUNTY) { facilities ->
                        val patientQueries = facilities.map { facilityId ->
                            "Patient?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                            "QuestionnaireResponse?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                            "MeasureReport?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                            "Encounter?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                            "Observation?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                        }
                        val combinedResources = LinkedList(patientQueries)
                        onResult(combinedResources)
                    }
                } else {
                    onResult(LinkedList()) // subCounty was null
                }
            }

            UserRole.COUNTY_DISEASE_SURVEILLANCE_OFFICER -> {
                val subCounty = formatter.getSharedPref("county", context)
                if (subCounty != null) {
                    getFacilitiesByLevel(subCounty, LocationLevel.COUNTY) { facilities ->
                        val patientQueries = facilities.map { facilityId ->
                            "Patient?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                            "QuestionnaireResponse?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                            "MeasureReport?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                            "Encounter?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                            "Observation?_tag=Location/$facilityId&_sort=_lastUpdated&_count=500"
                        }
                        val combinedResources = LinkedList(patientQueries)
                        onResult(combinedResources)
                    }
                } else {
                    onResult(LinkedList()) // County was null
                }
            }

            null -> {
                onResult(LinkedList()) // unknown role
            }

            UserRole.ADMINISTRATOR -> {

            }

            UserRole.SUPERUSER -> {


            }
        }
    }


    /**
     * Affixes the last updated timestamp to the request URL.
     *
     * If the request URL includes the `$everything` parameter, the last updated timestamp will be
     * attached using the `_since` parameter. Otherwise, the last updated timestamp will be attached
     * using the `_lastUpdated` parameter.
     */
    private fun affixLastUpdatedTimestamp(url: String, lastUpdated: String): String {
        var downloadUrl = url

        // Affix lastUpdate to a $everything query using _since as per:
        // https://hl7.org/fhir/operation-patient-everything.html
        if (downloadUrl.contains("\$everything")) {
            downloadUrl = "$downloadUrl?_since=$lastUpdated"
        }
        if (!downloadUrl.contains("\$everything")) {
            downloadUrl = if (downloadUrl.contains("&_count=")) {
                url
            } else if (downloadUrl.contains("&_lastUpdated")) {
                url
            } else if (downloadUrl.contains("sort")) {
                "$downloadUrl&_lastUpdated=gt$lastUpdated"
            } else {
                "$downloadUrl?_lastUpdated=gt$lastUpdated"
            }
        }

        // Do not modify any URL set by a server that specifies the token of the page to return.
        if (downloadUrl.contains("&page_token")) {
            downloadUrl = url
        }
        return downloadUrl
    }


    private fun Date.toTimeZoneString(): String {
        val simpleDateFormat =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
        return simpleDateFormat.format(this.toInstant())
    }
}