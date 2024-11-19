package org.ktc2.cokaen.wouldyouin.feat_booking.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.ktc2.cokaen.wouldyouin.core.DateTimeUtils
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

        //이게 맞음
        binding.payButton.setOnClickListener {
            eventId?.let { id ->
                val quantity = binding.numberPicker.value
                val reservationRequest = ReservationRequest(eventId = id, quantity = quantity)
                Log.d("BookingActivity", "결제 요청: $reservationRequest")
                reservationViewModel.createKakaoPay(reservationRequest)

                reservationViewModel.kakaoPayResponse.observe(this) { response ->
                    response?.let {
                        val redirectUrl = it.kakaoPayResponse.nextRedirectPcUrl
                        Log.d("BookingActivity", "결제 URL: $redirectUrl")
                        launchExternalBrowser(redirectUrl) // 웹 브라우저 열기
                        reservationIdFromApi = it.reservationResponse.id
                        Log.d("BookingActivity", "ReservationId: $reservationIdFromApi")
                    } ?: run {
                        Log.e("BookingActivity", "KakaoPay Response is null")
                        Toast.makeText(this, "결제 URL을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                Log.e("BookingActivity", "Event ID is null")
                Toast.makeText(this, "이벤트 ID를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
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

    /*
    //최근 사용
    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data
        if (uri != null && uri.scheme == "wouldyouin" && uri.host == "booking") {
            val path = uri.path
            if (path == "/payment/check") {
                val action = uri.getQueryParameter("action") // 액션 추출
                //val reservationId = uri.getQueryParameter("reservationId")?.toLongOrNull()
                val reservationId = reservationIdFromApi
                when (action) {
                    "payment_approve" -> {
                        Log.i("DeepLinkHandler", "Payment approved")
                        /*
                        if (reservationId != null) {
                            navigateToBookingDetails(reservationId)
                        } else {
                            Log.e("DeepLinkHandler", "Reservation ID is null")
                        }*/
                        //val reservationId = uri.getQueryParameter("reservationId")?.toLongOrNull()
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
    }*/

    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data
        if (uri != null && uri.scheme == "wouldyouin" && uri.host == "booking") {
            val path = uri.path
            if (path?.startsWith("/payment/check") == true) {
                val action = path.substringAfterLast("/")
                val reservationId = uri.getQueryParameter("reservationId")?.toLongOrNull()
                //val reservationId = reservationIdFromApi
                Log.d("DeepLinkHandler", "Extracted reservationId: $reservationId")
                when (action) {
                    "payment_approve" -> {
                        Log.i("DeepLinkHandler", "Payment approved")
                        /*
                        if (reservationId != null) {
                            navigateToBookingDetails(reservationId)
                        } else {
                            Log.e("DeepLinkHandler", "Reservation ID is null")
                        }*/
                        //val reservationId = uri.getQueryParameter("reservationId")?.toLongOrNull()
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