package org.ktc2.cokaen.wouldyouin.network.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.data.model.ApiResponseBodyKakaoPayReservationResponse
import org.ktc2.cokaen.wouldyouin.data.model.ApiResponseBodyReservationResponse
import org.ktc2.cokaen.wouldyouin.data.model.CurationResponse
import org.ktc2.cokaen.wouldyouin.data.model.CurationSliceResponse
import org.ktc2.cokaen.wouldyouin.data.model.KakaoPayReservationResponse
import org.ktc2.cokaen.wouldyouin.data.model.ReservationCreateRequestWrapper
import org.ktc2.cokaen.wouldyouin.network.service.ReservationAPIRetrofitService
import org.ktc2.cokaen.wouldyouin.data.model.ReservationRequest
import org.ktc2.cokaen.wouldyouin.data.model.ReservationResponse
import org.ktc2.cokaen.wouldyouin.data.model.ReservationSliceResponse
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ReservationAPIRetrofitRepository @Inject constructor(
    private val retrofitService: ReservationAPIRetrofitService
) {
    // 예매 목록 조회
    suspend fun getReservationList(
        page: Int = 0,
        size: Int = 10,
        lastId: Long = Long.MAX_VALUE
    ): ReservationSliceResponse {
        try {
            val response = retrofitService.getReservationList(page, size, lastId)
            return when {
                response.isSuccessful -> {
                    response.body()?.let { body ->
                        if (body.success) {
                            Log.d("Reservation  List Body", body.data.toString())
                            body.data
                        } else {
                            throw ServerCommonAPIRetrofitRepository.CustomException(
                                body.message ?: "예매 목록 조회에 실패했습니다"
                            )
                        }
                    }
                        ?: throw ServerCommonAPIRetrofitRepository.CustomException("서버로부터 유효한 응답을 받지 못했습니다")
                }

                else -> {
                    val errorBody = response.errorBody()?.string()
                    throw ServerCommonAPIRetrofitRepository.CustomException("서버 응답 오류: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Log.e("GetReservationList", "Create failed", e)
            throw when (e) {
                is IOException -> ServerCommonAPIRetrofitRepository.CustomException("네트워크 연결을 확인해주세요")
                is HttpException -> ServerCommonAPIRetrofitRepository.CustomException("서버 통신 오류: ${e.code()}")
                else -> e
            }
        }
    }

    // 예매 생성
    suspend fun createReservation(request: ReservationRequest): ApiResponseBodyReservationResponse? {
        return try {
            val response = retrofitService.createReservation(request)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("ReservationRepository", "Error creating reservation: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ReservationRepository", "Exception creating reservation", e)
            null
        }
    }

    suspend fun createKakaoPay(request: ReservationRequest): KakaoPayReservationResponse? {
        return try {
            val response = retrofitService.createKakaoPay(request)
            if (response.isSuccessful) {
                val bodyString = response.body()?.string()
                Log.d("ReservationRepository", "Raw response: $bodyString")

                // JSON 파싱
                val jsonObject = JSONObject(bodyString ?: "")
                val dataObject = jsonObject.getJSONObject("data") // "data" 객체 추출

                val kakaoPayResponse = Gson().fromJson(
                    dataObject.toString(),
                    KakaoPayReservationResponse::class.java
                )
                Log.d("ReservationRepository", "Parsed KakaoPayResponse: $kakaoPayResponse")
                kakaoPayResponse
            } else {
                Log.e("ReservationRepository", "Error: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: JSONException) {
            Log.e("ReservationRepository", "JSON Parsing Exception", e)
            null
        } catch (e: Exception) {
            Log.e("ReservationRepository", "Exception while creating KakaoPay", e)
            null
        }
    }

    suspend fun getReservationDetails(reservationId: Long): ReservationResponse {
        try {
            Log.d("DetailBooking", "Repository - Starting API call")

            // retrofitService가 null이 아닌지 확인
            Log.d("DetailBooking", "RetrofitService instance: $retrofitService")

            val response = retrofitService.getReservation(reservationId)
            Log.d("DetailBooking", "Repository - API call completed with response: $response")

            return when {
                response.isSuccessful -> {
                    val body = response.body()
                    Log.d("DetailBooking", "Response body: $body")

                    body?.let {
                        if (it.success) {
                            it.data
                        } else {
                            throw ServerCommonAPIRetrofitRepository.CustomException(
                                it.message ?: "예매 상세 정보 조회에 실패했습니다"
                            )
                        }
                    } ?: throw ServerCommonAPIRetrofitRepository.CustomException("서버로부터 유효한 응답을 받지 못했습니다")
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e("DetailBooking", "Error response: ${response.code()}, Error body: $errorBody")
                    throw ServerCommonAPIRetrofitRepository.CustomException("서버 응답 오류: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Log.e("DetailBooking", "Repository exception", e)
            throw when (e) {
                is IOException -> {
                    Log.e("DetailBooking", "Network error", e)
                    ServerCommonAPIRetrofitRepository.CustomException("네트워크 연결을 확인해주세요")
                }
                is HttpException -> {
                    Log.e("DetailBooking", "HTTP error: ${e.code()}", e)
                    ServerCommonAPIRetrofitRepository.CustomException("서버 통신 오류: ${e.code()}")
                }
                else -> {
                    Log.e("DetailBooking", "Unknown error", e)
                    e
                }
            }
        }
    }

    suspend fun deleteReservation(reservationId: Long): Boolean {
        return try {
            val response = retrofitService.deleteReservation(reservationId)
            if (response.isSuccessful) {
                true
            } else {
                throw Exception("예매 취소에 실패했습니다. 오류 코드: ${response.code()}")
            }
        } catch (e: Exception) {
            when (e) {
                is IOException -> throw Exception("네트워크 연결을 확인해주세요")
                is HttpException -> throw Exception("서버 통신 중 오류가 발생했습니다")
                else -> throw e
            }
        }
    }
}
