package org.ktc2.cokaen.wouldyouin.feat_booking.view

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import org.ktc2.cokaen.wouldyouin.core.DateTimeUtils
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.feat_booking.R
import org.ktc2.cokaen.wouldyouin.feat_booking.databinding.ActivityBookingDetailsBinding
import org.ktc2.cokaen.wouldyouin.feat_booking.viewModel.BookingDetailsViewModel

@AndroidEntryPoint
class BookingDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookingDetailsBinding
    private val viewModel: BookingDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        // Observer 설정을 먼저 합니다
        setupObservers()

        // 그 다음 데이터 로드
        loadReservationData()


    }

    /*
    private fun setupObservers() {
        viewModel.reservation.observe(this) { currentReservation ->
            currentReservation?.let { reservation ->
                viewModel.reservation.observe(this) { currentReservation ->
                    currentReservation?.let { reservation ->
                        binding.apply {
                            imageUrl = reservation.event.thumbnailUrl
                            eventName.text = reservation.event.title
                            eventLocation.text = reservation.event.location.detailAddress
                            eventDate.text = DateTimeUtils.formatDetailTimeString(reservation.event.startTime)
                            paymentDate.text = "${DateTimeUtils.formatDetailTimeString(reservation.reservationDate)} 예매"
                            paymentAmount.text = "₩${reservation.price}"
                            reservationNumber.text = reservation.id.toString()
                            bookerName.text = reservation.member.nickname
                            ticketQuantity.text = reservation.quantity.toString()

                            btnBack.setOnClickListener {
                                finish()
                            }
                        }

                        // binding.apply 밖으로 이동
                        binding.cancelButton.setOnClickListener {
                            intent.getStringExtra("reservationId")?.toLongOrNull()?.let { id ->
                                viewModel?.deleteReservation(id, reservation.event.startTime)  // null-safe 호출로 변경
                            }
                        }
                    }
                }
            }
        }
        viewModel.deletionSuccess.observe(this) { success ->
            if (success) {
                finish()  // 성공했을 때만 화면을 닫음
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }*/
    private fun setupObservers() {
        // reservation LiveData 관찰
        viewModel.reservation.observe(this) { currentReservation ->
            currentReservation?.let { reservation ->
                // UI 업데이트
                binding.apply {
                    imageUrl = reservation.event.thumbnailUrl
                    eventName.text = reservation.event.title
                    eventLocation.text = reservation.event.location.detailAddress
                    eventDate.text = DateTimeUtils.formatDetailTimeString(reservation.event.startTime)
                    paymentDate.text = "${DateTimeUtils.formatDetailTimeString(reservation.reservationDate)} 예매"
                    paymentAmount.text = "₩${reservation.price}"
                    reservationNumber.text = reservation.id.toString()
                    bookerName.text = reservation.member.nickname
                    ticketQuantity.text = reservation.quantity.toString()

                    btnBack.setOnClickListener {
                        finish()
                    }

                    cancelButton.setOnClickListener {
                        intent.getStringExtra("reservationId")?.toLongOrNull()?.let { id ->
                            viewModel?.deleteReservation(id, reservation.event.startTime)
                        }
                    }
                }
            }
        }

        // 삭제 성공 여부 관찰
        viewModel.deletionSuccess.observe(this) { success ->
            if (success) {
                finish()  // 성공 시 화면 종료
            }
        }

        // 로딩 상태 관찰
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }


    private fun loadReservationData() {
        val reservationId = intent.getStringExtra("reservationId")?.toLongOrNull()
        Log.d("DetailBooking", "ReservationId: $reservationId")

        if (reservationId != null && reservationId != -1L) {
            viewModel.loadReservationDetail(reservationId)
        } else {
            Toast.makeText(this, "예매 내역을 찾을 수 없습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}