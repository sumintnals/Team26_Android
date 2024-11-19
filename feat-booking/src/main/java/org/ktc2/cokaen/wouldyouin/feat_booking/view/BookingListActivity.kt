package org.ktc2.cokaen.wouldyouin.feat_booking.view

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.core_navigation.ActivityNavigationOptions
import org.ktc2.cokaen.wouldyouin.core_navigation.DeepLinkDestinations
import org.ktc2.cokaen.wouldyouin.core_navigation.NavigationCommand
import org.ktc2.cokaen.wouldyouin.core_navigation.NavigationDestination
import org.ktc2.cokaen.wouldyouin.core_navigation.NavigationUtil
import org.ktc2.cokaen.wouldyouin.data.model.ReservationResponse
import org.ktc2.cokaen.wouldyouin.feat_booking.adapter.BookingAdapter
import org.ktc2.cokaen.wouldyouin.feat_booking.databinding.ActivityBookingListBinding
import org.ktc2.cokaen.wouldyouin.feat_booking.viewModel.BookingListViewModel
import javax.inject.Inject

@AndroidEntryPoint
class BookingListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookingListBinding
    private val viewModel: BookingListViewModel by viewModels()
    @Inject
    lateinit var navigationUtil: NavigationUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        viewModel.loadReservations()
        binding.btnBack.setOnClickListener {
            Log.d("BUTTON", "Clicked")
            finish()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // 예매 내역 관찰
                    viewModel.reservations.collect { reservations ->
                        setupCurationRecyclerView(reservations)

                        // RecyclerView 및 빈 상태 뷰 처리
                        if (reservations.isEmpty()) {
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyView.visibility = View.VISIBLE
                        } else {
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.emptyView.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }


    private fun setupCurationRecyclerView(reservations: List<ReservationResponse>) {
        val bookingAdapter = BookingAdapter(reservations)
        binding.recyclerView.apply {
            adapter = bookingAdapter
            layoutManager = LinearLayoutManager(
                this@BookingListActivity,
                LinearLayoutManager.VERTICAL,
                false
            )

            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val spacing = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8f, // 8dp
                        resources.displayMetrics
                    ).toInt()

                    val position = parent.getChildAdapterPosition(view)
                    val itemCount = parent.adapter?.itemCount ?: 0

                    outRect.top = spacing

                    // 마지막 아이템은 bottom 간격을 0으로 설정
                    if (position == itemCount - 1) {
                        outRect.bottom = 0
                    } else {
                        outRect.bottom = spacing
                    }
                }
            })


            bookingAdapter.setOnItemClickListener { reservation ->
                startActivityTo(reservation.id)
            }

            // 세로 스크롤
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (!viewModel.isLoading.value &&
                        totalItemCount <= lastVisibleItem + 5 &&
                        dy > 0) {  // 아래로 스크롤할 때만
                        viewModel.loadReservations()
                    }
                }
            })
        }
    }

    private fun startActivityTo(reservationId: Long) {
        val command = NavigationCommand(
            destination = NavigationDestination.Activity(DeepLinkDestinations.DETAIL_BOOKING_ACTIVITY),
            data = mapOf("reservationId" to reservationId.toString()),
            activityOptions = ActivityNavigationOptions(
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            )
        )
        navigationUtil.navigate(command)
    }
}