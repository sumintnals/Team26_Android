package org.ktc2.cokaen.wouldyouin.feat_booking.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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

        //결제 결과
        binding.payButton.setOnClickListener {
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
                                /*
                            reservationId?.let {
                                Log.d("BookingActivity", "Navigating to BookingDetailsActivity with reservationId: $reservationId")
                                val intent = Intent(this, BookingDetailsActivity::class.java).apply {
                                    putExtra("reservationId", reservationId.toString())
                                }
                                startActivity(intent)
                            }*/
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
                            }
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
    }
    /*
    private fun launchExternalBrowser(oauthUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl))
        startActivity(intent)
    }*/

    /*
    override fun onResume() {
        super.onResume()
        val reservationId = intent.getStringExtra("reservationId")
        if (!reservationId.isNullOrEmpty()) {
            val intent = Intent(this, BookingDetailsActivity::class.java).apply {
                putExtra("reservationId", reservationId)
            }
            startActivity(intent)
            Log.d("BookingActivity", "Navigating to BookingDetailsActivity with reservationId: $reservationId")
        }
    }*/

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
    //첫
    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data
        if (uri != null && uri.scheme == "wouldyouin" && uri.host == "booking") {
            val path = uri.path
            if (path == "/payment/check") {
                val action = uri.lastPathSegment // 마지막 경로를 가져옴
                when (action) {
                    "payment_approve" -> {
                        Log.i("DeepLinkHandler", "Payment approved")
                        val reservationId = uri.getQueryParameter("reservationId")
                        val intent = Intent(this, BookingDetailsActivity::class.java).apply {
                            putExtra("reservationId", bookingId)
                        }
                        startActivity(intent)
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