package org.ktc2.cokaen.wouldyouin.feat_booking.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.data.model.ApiResponseBodyKakaoPayReservationResponse
import org.ktc2.cokaen.wouldyouin.data.model.ApiResponseBodyReservationResponse
import org.ktc2.cokaen.wouldyouin.data.model.KakaoPayReservationResponse
import org.ktc2.cokaen.wouldyouin.data.model.ReservationCreateRequestWrapper
import org.ktc2.cokaen.wouldyouin.data.model.ReservationRequest
import org.ktc2.cokaen.wouldyouin.data.model.ReservationResponse
import org.ktc2.cokaen.wouldyouin.network.repository.ReservationAPIRetrofitRepository
import javax.inject.Inject

@HiltViewModel
class ReservationViewModel @Inject constructor(
    private val repository: ReservationAPIRetrofitRepository,
    application: Application,
) : ViewModel() {
    private val context = application.applicationContext

    private var lastId: Long = Long.MAX_VALUE
    private var isLastPage: Boolean = false
    private var currentPage: Int = 0

    // 예약 목록을 저장하기 위한 LiveData
    private val _reservationList = MutableLiveData<List<ReservationResponse>>()
    val reservationList: LiveData<List<ReservationResponse>> get() = _reservationList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // 특정 예약의 상세 정보를 저장하기 위한 LiveData
    private val _reservationDetails = MutableLiveData<ReservationResponse>()
    val reservationDetails: LiveData<ReservationResponse> get() = _reservationDetails

    // 작업 성공 여부를 저장하기 위한 LiveData
    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> get() = _operationSuccess

    // 예매 생성 결과를 저장할 LiveData
    private val _reservationResponse = MutableLiveData<ApiResponseBodyReservationResponse?>()
    val reservationResponse: LiveData<ApiResponseBodyReservationResponse?> get() = _reservationResponse

    // 카카오 결제
    /*
    private val _payResponse = MutableLiveData<String?>()
    val payResponse: LiveData<String?> get() = _payResponse*/
    private val _kakaoPayResponse = MutableLiveData<KakaoPayReservationResponse?>()
    val kakaoPayResponse: LiveData<KakaoPayReservationResponse?> get() = _kakaoPayResponse

    private val _kakaoPayLiveData = MutableLiveData<KakaoPayReservationResponse?>()
    val kakaoPayLiveData: LiveData<KakaoPayReservationResponse?> = _kakaoPayLiveData

    // 전체 예약 목록을 가져오는 메서드
    fun fetchReservationList(page: Int = currentPage, size: Int = 10) {
        if (isLoading.value == true || isLastPage) return

        _isLoading.value = true
        Log.d("CurationList", "Called for page: $page with lastId: $lastId")

        viewModelScope.launch {
            try {
                val response = repository.getReservationList(page, size, lastId)
                if (response.reservations.isNotEmpty()) {
                    _reservationList.value = (_reservationList.value.orEmpty() + response.reservations).distinctBy { it.id }
                    lastId = response.reservations.last().id
                    currentPage++
                } else {
                    isLastPage = true
                }
            } catch (e: Exception) {
                ToastUtils.showShortToast(context, e.message ?: "큐레이션 목록 조회에 실패했습니다. 다시 시도해주세요.")
                Log.e("CurationViewModel", "Error loading curation list", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 예매 생성
    fun createReservation(request: ReservationRequest) {
        viewModelScope.launch {
            val response = repository.createReservation(request)
            _reservationResponse.value = response
            Log.d("ReservationViewModel", "Request Body JSON: $request")
            if (response == null) {
                Log.e("ReservationViewModel", "Response is null")
            }

        }
    }

    // 카카오 결제
    fun createKakaoPay(request: ReservationRequest) {
        viewModelScope.launch {
            try {
                val response = repository.createKakaoPay(request)
                _kakaoPayResponse.value = response
                if (response != null) {
                    Log.d("ReservationViewModel", "KakaoPay Response: $response")
                } else {
                    Log.e("ReservationViewModel", "KakaoPay Response is null")
                }
            } catch (e: Exception) {
                Log.e("ReservationViewModel", "Error in createKakaoPay", e)
            }
        }
    }
    /*
    fun createKakaoPay(request: ReservationRequest) {

        //첫번째 방법
        viewModelScope.launch {
            try {
                val redirectUrl = repository.createKakaoPay(request)
                _payResponse.value = redirectUrl
                if (redirectUrl != null) {
                    Log.d("ReservationViewModel", "Redirect URL: $redirectUrl")
                } else {
                    Log.e("ReservationViewModel", "Redirect URL is null")
                }
            } catch (e: Exception) {
                Log.e("ReservationViewModel", "Error in createKakaoPay", e)
            }
        }
    }*/

    /*
    fun getKakaoPayData() {
        viewModelScope.launch {
            val response = repository.fetchKakaoPayData()
            if (response != null) {
                Log.d("KakaoPayViewModel", "KakaoPay data received: $response")
                _kakaoPayLiveData.postValue(response)
            } else {
                Log.e("KakaoPayViewModel", "Failed to fetch KakaoPay data")
            }
        }
    }*/

}