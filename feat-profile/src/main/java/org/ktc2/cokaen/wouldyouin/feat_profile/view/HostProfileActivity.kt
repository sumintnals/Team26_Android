package org.ktc2.cokaen.wouldyouin.feat_profile.view

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ktc2.cokaen.wouldyouin.core_navigation.ActivityNavigationOptions
import org.ktc2.cokaen.wouldyouin.core_navigation.DeepLinkDestinations
import org.ktc2.cokaen.wouldyouin.core_navigation.NavigationCommand
import org.ktc2.cokaen.wouldyouin.core_navigation.NavigationDestination
import org.ktc2.cokaen.wouldyouin.core_navigation.NavigationUtil
import org.ktc2.cokaen.wouldyouin.data.model.EventResponse
import org.ktc2.cokaen.wouldyouin.data.model.MemberType
import org.ktc2.cokaen.wouldyouin.feat_event.view.viewmodel.EventViewModel
import org.ktc2.cokaen.wouldyouin.feat_profile.databinding.ActivityProfileBinding
import org.ktc2.cokaen.wouldyouin.feat_profile.adapter.HashtagAdapter
import org.ktc2.cokaen.wouldyouin.feat_profile.adapter.PostAdapter
import org.ktc2.cokaen.wouldyouin.feat_profile.viewModel.LikesViewModel
import org.ktc2.cokaen.wouldyouin.feat_profile.viewModel.ProfileViewModel
import javax.inject.Inject

@AndroidEntryPoint
class HostProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val profileViewModel: ProfileViewModel by viewModels()
    private val eventViewModel: EventViewModel by viewModels()
    private val likesViewModel: LikesViewModel by viewModels()

    @Inject lateinit var navigationUtil: NavigationUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        //이벤트 상세 페이지에(EventDetail)서 프로필 보기를 누른 후 userId 전달 받을 경우
        val hostId = intent.getStringExtra("hostId")?.toLongOrNull()
        Log.d("HostProfileActivity", "Received hostId from intent: $hostId")
        if (hostId != null) {
            Log.d("HostProfileActivity", "Calling with hostId: $hostId")
            profileViewModel.fetchMemberProfile(hostId, this)
            eventViewModel.fetchEventsByHost(hostId, context = this)
            setupLikeButton(hostId)
        } else {
            Log.e("HostProfileActivity", "HostId is null or invalid")
        }

        // ViewModel의 데이터를 관찰하여 UI 업데이트
        profileViewModel.memberProfile.observe(this) { memberResponse ->
            Log.d("HostProfileActivity", "1. Raw memberResponse: $memberResponse")

            memberResponse?.data?.let { member ->
                Log.d("HostProfileActivity", "2. Member object: $member")
                Log.d("HostProfileActivity", "3. Member hashtags: ${member.hashtags}")
                Log.d("HostProfileActivity", "4. Hashtags type: ${member.hashtags::class.java}")

                binding.nickname.text = member.nickname
                binding.role.text = member.memberType
                binding.likes.text = member.likes.toString()
                binding.intro.text = member.intro
                binding.email.text = member.email

                // 해시태그 처리를 더 상세하게 로깅
                when {
                    member.hashtags == null -> {
                        Log.e("HostProfileActivity", "5. Hashtags is null")
                    }
                    member.hashtags.isEmpty() -> {
                        Log.d("HostProfileActivity", "6. Hashtags is empty list")
                    }
                    else -> {
                        Log.d("HostProfileActivity", "7. Hashtags found: ${member.hashtags.size} items")
                        setupHashtagRecyclerView(member.hashtags)
                    }
                }

                binding.imageUrl = member.profileUrl
            } ?: run {
                Log.e("HostProfileActivity", "Member data is null")
            }
        }

        //진행한 행사 리사이클러뷰
        eventViewModel.eventsByHost.observe(this) { response ->
            response?.data?.events?.let { events ->
                Log.d("HostProfileActivity", "Events by host fetched: ${events.size}")
                setupPostRecyclerView(events)
            }
        }
    }

    // 해시태그 리사이클러뷰 설정 메소드
    private fun setupHashtagRecyclerView(hashtags: List<String>) {
        val hashtagAdapter = HashtagAdapter(hashtags)
        binding.hashtag.apply {
            layoutManager = LinearLayoutManager(this@HostProfileActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = hashtagAdapter
        }
    }

    //진행한 행사 리사이클러뷰 설정 메소드
    private fun setupPostRecyclerView(events: List<EventResponse>) {
        val postAdapter = PostAdapter(events) { eventId ->
            startActivityTo(eventId)
        }
        binding.posts.apply {
            layoutManager = LinearLayoutManager(this@HostProfileActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = postAdapter
        }
    }

    private fun setupLikeButton(hostId: Long) {
        likesViewModel.checkIfLiked(hostId, MemberType.host)

        lifecycleScope.launch {
            likesViewModel.isLiked.collect { isLiked ->
                updateLikeButtonColor(isLiked)
            }
        }

        binding.likeButton.setOnClickListener {
            lifecycleScope.launch {
                likesViewModel.toggleLike(hostId, MemberType.host)
                delay(300)
                profileViewModel.fetchMemberProfile(hostId, this@HostProfileActivity)
            }
        }
    }

    private fun startActivityTo(eventId: Long) {
        val command = NavigationCommand(
            destination = NavigationDestination.Activity(DeepLinkDestinations.DETAIL_EVENT_ACTIVITY),
            data = mapOf("event_id" to eventId),
            activityOptions = ActivityNavigationOptions(
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            )
        )
        navigationUtil.navigate(command)
    }

    private fun updateLikeButtonColor(isLiked: Boolean) {
        val color = if (isLiked) {
            Color.parseColor("#FF0000")
        } else {
            Color.parseColor("#808080")
        }
        binding.likeButton.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}