package org.ktc2.cokaen.wouldyouin.feat_profile.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.data.model.ReviewCreateRequest
import org.ktc2.cokaen.wouldyouin.data.model.ReviewEventResponse
import org.ktc2.cokaen.wouldyouin.data.model.ReviewResponse
import org.ktc2.cokaen.wouldyouin.network.repository.ReservationAPIRetrofitRepository
import org.ktc2.cokaen.wouldyouin.network.repository.ReviewRepositoryAPIRetrofitService
import javax.inject.Inject

@HiltViewModel
class EventReviewViewModel @Inject constructor(
    private val repository: ReviewRepositoryAPIRetrofitService,
    application: Application
) : ViewModel() {
    private val context = application.applicationContext

    private val _pendingReviews = MutableStateFlow<List<ReviewEventResponse>>(emptyList())
    val pendingReviews = _pendingReviews.asStateFlow()

    private val _completedReviews = MutableStateFlow<List<ReviewResponse>>(emptyList())
    val completedReviews = _completedReviews.asStateFlow()

    private val _pendingloading = MutableStateFlow(false)
    val pendingloading = _pendingloading.asStateFlow()

    private val _reviewLoading = MutableStateFlow(false)
    val reviewLoading = _reviewLoading.asStateFlow()

    private val _reviewSubmitResult = MutableSharedFlow<Boolean>()
    val reviewSubmitResult = _reviewSubmitResult.asSharedFlow()

    private var lastEventId: Long = Long.MAX_VALUE
    private var isLastEventPage: Boolean = false
    private var currentEventPage: Int = 0

    private var lastReviewId: Long = Long.MAX_VALUE
    private var isLastReviewPage: Boolean = false
    private var currentReviewPage: Int = 0

    init {
        loadPendingReviewList()
        loadCompleteReviewList()
    }

    fun loadPendingReviewList(page: Int = currentEventPage, size: Int = 10) {
        if (_pendingloading.value || isLastEventPage) return

        _pendingloading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getPendingReviewList(page, size, lastEventId)
                val reviewEvents = response.reviewEvents ?: emptyList()  // null이 아닌 빈 리스트로 처리
                if (reviewEvents.isNotEmpty()) {
                    val newList = _pendingReviews.value.orEmpty() + reviewEvents.distinctBy { it.eventId }
                    _pendingReviews.value = newList
                    lastEventId = reviewEvents.last().eventId
                    currentEventPage++
                    Log.d("EventReviewViewModel", "Loaded reviews: ${response.reviewEvents}")
                } else {
                    isLastEventPage = true
                }

            } catch (e: Exception) {
                ToastUtils.showShortToast(context, "후기 작성 대기중인 행사 목록을 불러오는 데 실패했습니다. 다시 시도해 주세요.")
                Log.e("EventReviewViewModel", "Error loading review list", e)
            } finally {
                _pendingloading.value = false
            }
        }
    }

    fun loadCompleteReviewList(page: Int = currentReviewPage, size: Int = 10) {
        if (_reviewLoading.value || isLastReviewPage) return

        _reviewLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getReviewList(page, size, lastReviewId)
                val reviewEvents = response.reviews ?: emptyList()  // null이 아닌 빈 리스트로 처리
                if (reviewEvents.isNotEmpty()) {
                    val newList = _completedReviews.value.orEmpty() + reviewEvents.distinctBy { it.id }
                    _completedReviews.value = newList
                    lastReviewId = reviewEvents.last().id
                    currentReviewPage++
                    Log.d("EventReviewViewModel", "Loaded reviews: ${response.reviews}")
                } else {
                    isLastReviewPage = true
                }

            } catch (e: Exception) {
                ToastUtils.showShortToast(context, "후기 작성 대기중인 행사 목록을 불러오는 데 실패했습니다. 다시 시도해 주세요.")
                Log.e("EventReviewViewModel", "Error loading review list", e)
            } finally {
                _reviewLoading.value = false
            }
        }
    }

    fun submitReview(review: ReviewCreateRequest) {
        viewModelScope.launch {
            _pendingloading.value = true
            try {
                repository.createReview(review)
                removePendingReview(review.eventId)
                _reviewSubmitResult.emit(true)
            } catch (e: Exception) {
                Log.e("EventReviewViewModel", "Error submitting review", e)
                _reviewSubmitResult.emit(false)
            } finally {
                _pendingloading.value = false
            }
        }
    }

    private fun removePendingReview(eventId: Long) {
        val currentList = _pendingReviews.value.toMutableList()
        currentList.removeAll { it.eventId == eventId }
        _pendingReviews.value = currentList
    }
}