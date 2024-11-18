package org.ktc2.cokaen.wouldyouin.feat_curation.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.data.model.EventResponse
import org.ktc2.cokaen.wouldyouin.network.repository.EventAPIRetrofitRepository
import javax.inject.Inject

@HiltViewModel
class CurationSearchViewModel @Inject constructor(
    private val repository: EventAPIRetrofitRepository,
    application: Application
) : ViewModel() {
    private val context = application.applicationContext

    private val _eventList = MutableLiveData<List<EventResponse>>()
    val eventList: LiveData<List<EventResponse>> = _eventList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private var lastId: Long = Long.MAX_VALUE
    private var isLastPage: Boolean = false
    private var currentPage: Int = 0

    fun loadEventList(page: Int = currentPage, size: Int = 10) {
        if (isLoading.value == true || isLastPage) return

        _isLoading.value = true
        Log.d("EventList", "Called for page: $page with lastId: $lastId")

        viewModelScope.launch {
            try {
                val response = repository.getAllEvents(page, size, lastId)
                val newList = (_eventList.value.orEmpty() + response.events).distinctBy { it.id }
                _eventList.value = newList  // postValue 대신 value 사용

                if (response.events.isNotEmpty()) {
                    lastId = response.events.last().id
                    currentPage++
                    _isEmpty.value = newList.isEmpty()  // 전체 리스트 기준으로 판단
                } else {
                    isLastPage = true
                    _isEmpty.value = newList.isEmpty()
                }
            } catch (e: Exception) {
                ToastUtils.showShortToast(context, e.message ?: "이벤트 목록 조회에 실패했습니다. 다시 시도해주세요.")
                Log.e("CurationSearchViewModel", "Error loading event list", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}