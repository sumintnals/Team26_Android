package org.ktc2.cokaen.wouldyouin.data.model
import com.google.gson.annotations.SerializedName

//리뷰
data class ApiResponseBodyReviewResponse(
    val success: Boolean,
    val data: ReviewResponse?,
    val code: String,
    val message: String
)

//리뷰 리스트
data class ApiResponseBodReviewSliceResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: ReviewSliceResponse,

    @SerializedName("code")
    val code: String,

    @SerializedName("message")
    val message: String
)

data class ReviewSliceResponse(
    val reviews: List<ReviewResponse>,

    val sliceInfo: SliceInfo
)

data class ReviewResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("member")
    val member: ReviewMemberResponse,

    @SerializedName("event")
    val event: ReviewEventResponse,

    @SerializedName("score")
    val score: Int,

    @SerializedName("content")
    val content: String
)

data class ReviewMemberResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nickname")
    val nickname: String
)

data class ReviewEventResponse(
    val eventId: Long,
    val title: String,
    val startTime: List<String>,
    val thumbnailUrl: String
)
