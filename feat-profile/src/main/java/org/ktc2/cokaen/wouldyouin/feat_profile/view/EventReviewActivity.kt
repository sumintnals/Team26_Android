package org.ktc2.cokaen.wouldyouin.feat_profile.view

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.feat_profile.R
import org.ktc2.cokaen.wouldyouin.feat_profile.adapter.CompletedReviewAdapter
import org.ktc2.cokaen.wouldyouin.feat_profile.adapter.PendingReviewAdapter
import org.ktc2.cokaen.wouldyouin.feat_profile.databinding.ActivityEventReviewBinding
import org.ktc2.cokaen.wouldyouin.feat_profile.viewModel.EventReviewViewModel

@AndroidEntryPoint
class EventReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEventReviewBinding
    private val viewModel: EventReviewViewModel by viewModels()
    private val pendingReviewAdapter = PendingReviewAdapter { eventId ->
        showReviewDialog(eventId)
    }
    private val completedReviewAdapter = CompletedReviewAdapter { reviewId ->
        deleteCompletedReview(reviewId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Intent에서 데이터 추출 및 null 처리
        val testData = intent.getStringExtra("key")

        if (testData != null) {
            // 데이터가 존재하는 경우
            Log.d("TargetActivity", "Received data: $testData")
            // 여기서 testData를 사용하는 추가 로직 구현
        } else {
            // 데이터가 null인 경우
            Log.w("TargetActivity", "No data received or data is null")
            // 데이터가 없는 경우에 대한 처리 로직 구현
        }

        setupRecyclerView()
        setupObservers()
    }

    private fun setupObservers() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pendingReviews.collect { reviews ->
                        pendingReviewAdapter.submitList(reviews)
                        if (reviews.isEmpty()) {
                            binding.pendingReviewRecyclerView.visibility = View.GONE
                            binding.emptyPendingView.visibility = View.VISIBLE
                        } else {
                            binding.pendingReviewRecyclerView.visibility = View.VISIBLE
                            binding.emptyPendingView.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.pendingloading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading && viewModel.pendingReviews.value.isEmpty()
                    }
                }

                launch {
                    viewModel.reviewSubmitResult.collect { success ->
                        if (success) {
                           ToastUtils.showShortToast(this@EventReviewActivity, "등록이 완료되었습니다.")
                        }
                    }
                }

                launch {
                    viewModel.completedReviews.collect { reviews ->
                        completedReviewAdapter.submitList(reviews)
                        if (reviews.isEmpty()) {
                            binding.completedReviewRecyclerView.visibility = View.GONE
                            binding.emptyCompletedView.visibility = View.VISIBLE
                        } else {
                            binding.completedReviewRecyclerView.visibility = View.VISIBLE
                            binding.emptyCompletedView.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.pendingReviewRecyclerView.apply {
            adapter = pendingReviewAdapter
            layoutManager = LinearLayoutManager(this@EventReviewActivity)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (!viewModel.pendingloading.value && totalItemCount <= lastVisibleItem + 5) {
                        viewModel.loadPendingReviewList()
                    }
                }
            })
        }

        binding.completedReviewRecyclerView.apply {
            adapter = completedReviewAdapter
            layoutManager = LinearLayoutManager(this@EventReviewActivity)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (!viewModel.reviewLoading.value && totalItemCount <= lastVisibleItem + 5) {
                        viewModel.loadCompleteReviewList()
                    }
                }
            })
        }
    }

    private fun deleteCompletedReview(reviewId: Long) {
//        viewModel.deleteReview(reviewId)
//        ToastUtils.showShortToast(this, "후기가 삭제되었습니다.")
    }
    private fun showReviewDialog(eventId: Long) {
        ReviewDialog.newInstance(eventId)
            .show(supportFragmentManager, "review_dialog")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}