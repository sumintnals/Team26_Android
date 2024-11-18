package org.ktc2.cokaen.wouldyouin.feat_curation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.ktc2.cokaen.wouldyouin.data.model.SearchEventData
import org.ktc2.cokaen.wouldyouin.feat_curation.databinding.SelectedEventItemBinding
import org.ktc2.cokaen.wouldyouin.feat_curation.viewModel.CreateCurationViewModel

class SelectedEventsAdapter(
    private val viewModel: CreateCurationViewModel
) : ListAdapter<SearchEventData, SelectedEventsAdapter.EventViewHolder>(EventDiffCallback) {

    // DiffUtil.ItemCallback 구현
    companion object {
        private object EventDiffCallback : DiffUtil.ItemCallback<SearchEventData>() {
            override fun areItemsTheSame(oldItem: SearchEventData, newItem: SearchEventData): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: SearchEventData, newItem: SearchEventData): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        return EventViewHolder(
            SelectedEventItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)

        holder.binding.apply {
            searchEventData = event  // 데이터 클래스 직접 바인딩
            imageUrl = event.imageUrl  // 이미지 URL 바인딩

            // 삭제 버튼 클릭 리스너
            deleteButton.setOnClickListener {
                viewModel.deleteEvent(position)
            }

            // 바인딩 즉시 실행
            executePendingBindings()
        }
    }

    inner class EventViewHolder(val binding: SelectedEventItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }
}
