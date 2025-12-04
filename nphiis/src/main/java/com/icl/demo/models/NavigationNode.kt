package com.icl.demo.models

import org.hl7.fhir.r4.model.Resource
import kotlinx.serialization.Serializable
import java.time.Instant

sealed class NavigationNode {
    data class Category(val item: ReportingItem) : NavigationNode()
    data class ActionNode(val action: ReportingAction) : NavigationNode()
}

@Serializable
data class ReportingConfig(
    val reporting: List<ReportingItem>
)

@Serializable
data class ReportingItem(
    val name: String,
    val code: String,
    val children: List<ReportingItem>? = null,
    val actions: List<ReportingAction>? = null,
    val comingSoon: Boolean = false,
    val layout: String? = null,
    val icon: String? = null,          // we ignore this except for future use
    val showIcon: Boolean = false
)

@Serializable
data class ReportingAction(
    val type: String,
    val label: String,
    val questionnaire: String? = null,
    val target: String? = null,
    val case: String? = null,
    val comingSoon: Boolean = false,
    val icon: String? = null,          // we ignore this except for future use
    val showIcon: Boolean = false
)

enum class LayoutMode {
    LINEAR,
    GRID
}

data class Notification(
    val id: String,
    val practitionerId: String,
    val encounterId: String,
    val investigationDate: String,
    val dueDate: String,
    val title: String,
    val body: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

data class NotificationResponse(
    val status: String? = null,
    val notifications: List<Notification>? = emptyList(),
    val total: Int? = 0
)

data class FCMToken(
    val token: String
)

data class DbSignIn(
    val idNumber: String,
    val password: String,
    val location: String,
)

data class DbResetPasswordData(val idNumber: String, val email: String)
data class FhirBundle(
    val resourceType: String,
    val id: String,
    val type: String,
    val link: List<FhirLink>?,
    val entry: List<FhirEntry>?
)

data class FhirLink(
    val relation: String,
    val url: String
)

data class FhirEntry(
    val fullUrl: String,
    val resource: LocationResource,
    val search: SearchInfo
)

data class LocationResource(
    val resourceType: String = "Location",
    val id: String,
    val meta: Meta,
    val name: String,
    val type: List<LocationType>,
    val partOf: PartOf? = null
)

data class Meta(
    val versionId: String,
    val lastUpdated: String,
    val source: String
)

data class LocationType(
    val coding: List<Coding>
)

data class Coding(
    val system: String,
    val code: String,
    val display: String
)

data class PartOf(
    val reference: String,
    val display: String
)


data class SearchInfo(
    val mode: String
)

data class DbSetPasswordReq(val resetCode: String, val idNumber: String, val password: String)
data class DbSignInResponse(
    val access_token: String,
    val expires_in: String,
    val refresh_expires_in: String,
    val refresh_token: String,
)

data class DbResetPassword(
    val status: String,
    val response: String,
)

data class DbResponseError(
    val status: String,
    val error: String,
)

data class UserResponse(
    val status: String,
    val user: User
)

data class User(
    val firstName: String?,                // Might not be returned
    val lastName: String?,                 // Might not be returned
    val fhirPractitionerId: String?,       // Can be null or missing
    val practitionerRole: String?,         // e.g. "VACCINATOR"
    val role: String?,                     // e.g. "VACCINATOR"
    val id: String?,                       // UUID
    val idNumber: String?,                 // e.g. "400300"
    val fullNames: String?,                // Sometimes missing
    val phone: String?,                    // Nullable (confirmed)
    val email: String?,                    // Nullable (confirmed)
    val status: Boolean?,                  // Present in API
    val locationInfo: LocationInfo?
)

data class LocationInfo(
    val facility: String?,
    val facilityName: String?,
    val ward: String?,
    val wardName: String?,
    val subCounty: String?,
    val subCountyName: String?,
    val county: String?,
    val countyName: String?,
    val country: String?,
    val countryName: String?
)

data class QuestionnaireAnswer(
    val linkId: String,
    val text: String,
    val answer: String
)

data class SpecimenConfig(
    val type: String,
    val entryLinkId: String,
    val dateLinkId: String
)

@Serializable
data class OutputGroup(
    val linkId: String,
    val text: String,
    val type: String,
    val items: List<OutputItem> = emptyList()
)

@Serializable
data class OutputItem(
    val linkId: String,
    val text: String,
    val type: String,
    var value: String? = "",
    var parentOperator: String? = "==",
    val enable: Boolean = true,
    val parentLink: String? = null,
    val parentResponse: String? = null,
)

@Serializable
data class QuestionnaireItem(
    val item: List<GroupItem>
)

@Serializable
data class QuestionnaireItemChild(
    val item: List<ChildItem>
)

@Serializable
data class GroupItem(
    val linkId: String,
    val text: String,
    val type: String,
    val item: List<ChildItem>? = null
)

@Serializable
data class ChildItem(
    val linkId: String,
    val text: String,
    val type: String,
    val item: List<ChildItem>? = null,
    val enableWhen: List<EnableWhen>? = null
)

@Serializable
data class EnableWhen(
    val question: String,
    val operator: String,
    val answerCoding: AnswerCoding? = null,
    val answerString: String? = null,
    val answerBoolean: Boolean? = null,
    val answerDate: String? = null,
    val answerInteger: Int?
)

@Serializable
data class AnswerCoding(
    val code: String,
    val display: String?
)

data class ResourceWithSyncStatus(
    val resource: Resource,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttempt: Instant? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

enum class SyncStatus {
    SYNCED,
    SYNCING,
    PENDING,
    FAILED,
    RETRYING
}

sealed class SyncResult {
    data class Success(val resourceId: String) : SyncResult()
    data class Failure(val error: String) : SyncResult()
}

data class SyncStats(
    val total: Int = 0,
    val synced: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0,
    val retrying: Int = 0
)

data class SyncFailure(
    val resourceId: String,
    val resourceType: String,
    val errorMessage: String,
    val timestamp: Instant = Instant.now(),
    val retryCount: Int = 0
)

data class BulkSyncResult(
    val total: Int,
    val successful: Int,
    val failed: Int,
    val individualResults: List<SyncResult>
)


data class UserProfilePrefs(
    val firstName: String,
    val lastName: String,
    val fullNames: String,
    val email: String,
    val phone: String,
    val idNumber: String,
    val role: String,
    val county: String,
    val countyName: String,
    val subCounty: String,
    val subCountyName: String,
    val ward: String,
    val wardName: String,
    val facility: String,
    val facilityName: String
)

data class LocalBundleResponse(
    val resourceType: String,
    val id: String,
    val type: String,
    val link: List<LocalLink>,
    val entry: List<LocalEntry>
) {
    data class LocalLink(
        val relation: String,
        val url: String
    )

    data class LocalEntry(
        val response: LocalResponse
    )

    data class LocalResponse(
        val status: String,
        val location: String? = null,
        val etag: String? = null,
        val outcome: LocalOperationOutcome?
    )

    data class LocalOperationOutcome(
        val resourceType: String,
        val issue: List<LocalIssue>
    ) {
        data class LocalIssue(
            val severity: String,
            val code: String,
            val details: LocalDetails? = null,
            val diagnostics: String? = null
        ) {
            data class LocalDetails(
                val coding: List<LocalCoding>
            ) {
                data class LocalCoding(
                    val system: String,
                    val code: String,
                    val display: String
                )
            }
        }
    }
}