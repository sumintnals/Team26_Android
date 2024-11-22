package org.ktc2.cokaen.wouldyouin.feat_booking.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONException
import org.json.JSONObject
import org.ktc2.cokaen.wouldyouin.core.DateTimeUtils
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.data.model.ReservationCreateRequestWrapper
import org.ktc2.cokaen.wouldyouin.data.model.ReservationRequest
import org.ktc2.cokaen.wouldyouin.feat_booking.databinding.ActivityBookingBinding
import org.ktc2.cokaen.wouldyouin.feat_booking.viewModel.BookingEventViewModel
import org.ktc2.cokaen.wouldyouin.feat_booking.viewModel.ReservationViewModel

@AndroidEntryPoint
class BookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingBinding
    private var totalPrice: Int = 0
    private val bookingEventViewModel: BookingEventViewModel by viewModels()
    private val reservationViewModel: ReservationViewModel by viewModels()

    // 데이터 받는 변수 부분입니다. 나중에 수정해주세요!
    private var bookingId: String? = null
    private var userId: String? = null

    private var reservationIdFromApi: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 데이터 받아오는 부분
        val eventId = intent.getStringExtra("eventId")?.toLongOrNull()
        Log.d("BookingActivity", "Received eventId from intent: $eventId")
        if (eventId != null) {
            Log.d("BookingActivity", "Calling fetchEventDetails with eventId: $eventId")
            bookingEventViewModel.fetchEventDetails(eventId, this)
        } else {
            Log.e("BookingActivity", "EventId is null or invalid")
        }

        // ViewModel의 eventDetails 관찰하여 UI 업데이트
        bookingEventViewModel.eventDetails.observe(this) { eventResponse ->
            if (eventResponse == null) {
                Log.e("BookingActivity", "ㅇㅇEvent response is null")
            } else {
                Log.d("BookingActivity", "ㅇㅇEvent fetched successfully: ${eventResponse.data}")
            }
            eventResponse?.data?.let { event ->
                Log.d("BookingActivity", "Event fetched successfully: $event")
                binding.imageUrl = event.thumbnailUrl
                binding.eventName.text = event.title
                binding.eventOrganizerName.text = event.host.nickname
                binding.eventLocation.text = event.location.detailAddress
                binding.eventDate.text = DateTimeUtils.formatDateTimeString(event.startTime)
                //binding.price.text = "₩${event.price}"

                // 첫 가격 설정
                updateTotalPrice(1, event.price)
            } ?: run {
                Log.e("BookingActivity", "Event response data is null")
            }
        }

        binding.numberPicker.minValue = 1
        binding.numberPicker.maxValue = 10
        binding.numberPicker.wrapSelectorWheel = false

        binding.numberPicker.setOnValueChangedListener { _, _, newVal ->
            Log.d("BookingActivity", "NumberPicker value changed: $newVal")
            bookingEventViewModel.eventDetails.value?.data?.price?.let { pricePerTicket ->
                updateTotalPrice(newVal, pricePerTicket)
            }
        }

/*
        binding.payButton.setOnClickListener {
            eventId?.let { id ->
                val quantity = binding.numberPicker.value
                val reservationRequest = ReservationRequest(eventId = id, quantity = quantity)
                Log.d("BookingActivity", "결제 요청: $reservationRequest")

                // KakaoPay 요청 호출
                reservationViewModel.createKakaoPay(reservationRequest)

                // KakaoPay URL 관찰
                reservationViewModel.kakaoPayResponse.observe(this) { redirectUrl ->
                    redirectUrl?.let {
                        Log.d("BookingActivity", "Received KakaoPay redirect URL: $redirectUrl")
                        launchExternalBrowser(redirectUrl) // URL을 브라우저에서 열기
                    } ?: run {
                        Log.e("BookingActivity", "KakaoPay Response is null")
                        Toast.makeText(this, "결제 URL을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                Log.e("BookingActivity", "Event ID is null")
                Toast.makeText(this, "이벤트 ID를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }*/
        binding.payButton.setOnClickListener {
            eventId?.let { id ->

                //원본
                val quantity = binding.numberPicker.value
                val reservationRequest = ReservationRequest(eventId = id, quantity = quantity)
                //val requestWrapper = ReservationCreateRequestWrapper(reservationRequest = reservationRequest)
                //val memberId = 18L

                Log.d("BookingActivity", "RequestWrapper: $reservationRequest")

                /*
            // 카카오 결제 API 호출
            reservationViewModel.createKakaoPay(memberId, reservationRequest)
            Log.d("BookingActivity", "Request Body: $reservationRequest")
            */

                reservationViewModel.createReservation(reservationRequest)
                Log.d("BookingActivity", "Request Body: $reservationRequest")

                // KakaoPay 결제 URL 요청
                reservationViewModel.createKakaoPay(reservationRequest)

                // KakaoPay URL 관찰
                reservationViewModel.kakaoPayResponse.observe(this) { redirectUrl ->
                    redirectUrl?.let {
                        launchExternalBrowser(it) // 브라우저로 URL 이동
                    } ?: run {
                        ToastUtils.showShortToast(this, "결제 URL을 불러오지 못했습니다.")
                    }
                }

                reservationViewModel.reservationResponse.observe(this) { response ->
                    //reservationViewModel.payResponse.observe(this) { response ->
                    if (response?.success == true) {
                        Log.d(
                            "BookingActivity",
                            "Reservation created successfully: ${response.data}"
                        )
                        val reservationId = response.data?.id
                        //val reservationId = response.data?.reservationResponse?.id
                        Log.d("BookingActivity", "ReservationId to pass: $reservationId")

                        if (reservationId != null) {

                            binding.root.postDelayed({
                                val intent = Intent(this, BookingDetailsActivity::class.java).apply {
                                    putExtra("reservationId", reservationId.toString())
                                }
                                startActivity(intent)
                                Log.d("BookingActivity", "Navigated to BookingDetailsActivity with ID: $reservationId")
                            }, 7000) // 7초 딜레이
                        }
                            /*
                        reservationId?.let {
                            Log.d("BookingActivity", "Navigating to BookingDetailsActivity with reservationId: $reservationId")
                            val intent = Intent(this, BookingDetailsActivity::class.java).apply {
                                putExtra("reservationId", reservationId.toString())
                            }
                            startActivity(intent)
                        }*/

                            /*
                            val intent =
                                Intent(this, BookingDetailsActivity::class.java).apply {
                                    putExtra("reservationId", reservationId.toString())
                                }

                            Log.d(
                                "BookingActivity",
                                "Passing ReservationId to Intent: $reservationId"
                            )
                            startActivity(intent)
                        } else {
                            Log.e("BookingActivity", "ReservationId is null")
                        }*/
                    } else {
                        Log.e("BookingActivity", "예매 생성 실패: ${response?.message}")
                        Log.e("BookingActivity", "Response Code: ${response?.code}")
                        Log.e("BookingActivity", "Response Body: ${response?.message}")
                        ToastUtils.showShortToast(this@BookingActivity, "남은 좌석이 부족합니다.")
                    }
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        Log.d("BookingActivity", "돌아옴")
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    //추가
    //첫번째 방법&두번째 방법
    private fun launchExternalBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun updateTotalPrice(quantity: Int, pricePerTicket: Int) {
        totalPrice = quantity * pricePerTicket
        Log.d("BookingActivity", "Updating total price: $totalPrice")
        binding.price.text = "₩ $totalPrice"
    }

    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data
        if (uri != null && uri.scheme == "wouldyouin" && uri.host == "booking") {
            val path = uri.path
            if (path?.startsWith("/kakao/check") == true) {
                val action = path.substringAfterLast("/")
                val reservationId = uri.getQueryParameter("reservationId")?.toLongOrNull()
                //val reservationId = reservationIdFromApi
                Log.d("DeepLinkHandler", "Extracted reservationId: $reservationId")
                when (action) {
                    "payment_approve" -> {
                        Log.i("DeepLinkHandler", "Payment approved")
                        if (reservationId != null) {
                            val intent = Intent(this, BookingDetailsActivity::class.java).apply {
                                putExtra("reservationId", reservationId.toString())
                            }
                            startActivity(intent)
                            Toast.makeText(this, "결제 성공.", Toast.LENGTH_SHORT).show()
                        } else {
                            //Log.e("DeepLinkHandler", "Reservation ID is null or invalid")
                            Toast.makeText(this, "결제 정보가 올바르지 않습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "payment_failed" -> {
                        Log.i("DeepLinkHandler", "Payment failed")
                        Toast.makeText(this, "결제가 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    }
                    "payment_cancel" -> {
                        Log.i("DeepLinkHandler", "Payment canceled")
                        Toast.makeText(this, "결제가 취소되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    else -> Log.w("DeepLinkHandler", "Unknown payment action: $action")
                }
            } else {
                Log.w("DeepLinkHandler", "Unknown path: $path")
            }
        } else {
            Log.w("DeepLinkHandler", "Invalid deep link: ${uri?.toString()}")
        }
    }


    private fun handlePaymentResult(redirectUrl: String) {
        if (redirectUrl.isNotEmpty()) {
            Log.d("BookingActivity", "Redirecting to URL: $redirectUrl")
            launchExternalBrowser(redirectUrl)
        } else {
            Toast.makeText(this, "링크 이동 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToBookingDetails(reservationId: Long) {
        val intent = Intent(this, BookingDetailsActivity::class.java).apply {
            putExtra("reservationId", reservationId)
        }
        startActivity(intent)
    }

}