package org.ktc2.cokaen.wouldyouin.feat_curation.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.data.model.CurationCardResponse
import org.ktc2.cokaen.wouldyouin.data.model.CurationEventResponse
import org.ktc2.cokaen.wouldyouin.data.model.CurationResponse
import org.ktc2.cokaen.wouldyouin.network.repository.CurationAPIRetrofitRepository
import javax.inject.Inject

@HiltViewModel
class CurationDetailViewModel @Inject constructor(
    private val curationRepository: CurationAPIRetrofitRepository,
    application: Application
) : ViewModel() {
    private val context = application.applicationContext

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _curation = MutableLiveData<CurationResponse?>()
    val curation: LiveData<CurationResponse?> = _curation

    private val _curationBlocks = MutableLiveData<List<CurationCardResponse>>()
    val curationBlocks: LiveData<List<CurationCardResponse>> = _curationBlocks

    private val _curationEvents = MutableLiveData<List<CurationEventResponse>>()
    val curationEvents: LiveData<List<CurationEventResponse>> = _curationEvents

    private val _hashtags = MutableLiveData<List<String>>()
    val hashtags: LiveData<List<String>> = _hashtags

    private val _isDeleted = MutableLiveData<Boolean>()
    val isDeleted: LiveData<Boolean> = _isDeleted

    fun loadCurationDetail(curationId: Long) {
        if (isLoading.value == true) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val curationDetail = curationRepository.getCurationDetail(curationId)

                // 첫 번째 큐레이션 카드의 이미지 리스트 수정
                val modifiedCurationDetail = if (curationDetail.curationCards.isNotEmpty()) {
                    val modifiedCards = curationDetail.curationCards.mapIndexed { index, card ->
                        if (index == 0) {
                            card.copy(imageUrls = card.imageUrls.drop(1))
                        } else {
                            card
                        }
                    }
                    curationDetail.copy(curationCards = modifiedCards)
                } else {
                    curationDetail
                }

                _curation.value = modifiedCurationDetail
                _curationBlocks.value = modifiedCurationDetail.curationCards
                _hashtags.value = modifiedCurationDetail.hashtags
                _curationEvents.value = modifiedCurationDetail.eventsInfo

            } catch (e: Exception) {
                ToastUtils.showShortToast(context, e.message ?: "큐레이션 상세 정보 조회에 실패했습니다. 다시 시도해주세요.")
                Log.e("CurationDetailViewModel", "Error loading curation detail", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCuration(curationId: Long) {
        if (isLoading.value == true) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val isDeleted = curationRepository.deleteCuration(curationId)
                if (isDeleted) {
                    ToastUtils.showShortToast(context, "큐레이션이 삭제되었습니다.")
                    // 삭제 성공 시 Activity 종료를 위한 LiveData
                    _isDeleted.value = true
                }
            } catch (e: Exception) {
                ToastUtils.showShortToast(context, e.message ?: "큐레이션 삭제에 실패했습니다. 다시 시도해주세요.")
                Log.e("CurationDetailViewModel", "Error deleting curation", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}