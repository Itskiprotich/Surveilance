package com.imeja.surveilance.models

import com.imeja.surveilance.R
import kotlinx.serialization.Serializable

data class QuestionnaireAnswer(
    val linkId: String,
    val text: String,
    val answer: String
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

data class EnableWhen(
    val question: String,
    val operator: String,
    val answerCoding: AnswerCoding? = null,
    val answerString: String? = null,
    val answerBoolean: Boolean? = null,
    val answerDate: String? = null,
    val answerInteger: Int?
)

data class AnswerCoding(
    val code: String,
    val display: String?
)

enum class UrlData(var message: Int) {
    BASE_URL(R.string.base_url),
}

data class DbSignIn(
    val idNumber: String,
    val password: String,
    val location: String,
)

data class DbResetPasswordData(val idNumber: String, val email: String)


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

data class DbUserInfoResponse(
    val user: DbUser?,
)

data class DbUser(
    val firstName: String,
    val lastName: String,
    val role: String,
    val id: String,
    val idNumber: String,
    val fullNames: String,
    val phone: String?, // nullable
    val email: String
)

data class CaseOption(
    val title: String,
    val showCount: Boolean = false,
    val count: Int = 0
)
data class SpecimenConfig(
    val type: String,
    val entryLinkId: String,
    val dateLinkId: String
)
