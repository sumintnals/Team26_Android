package org.ktc2.cokaen.wouldyouin.feat_profile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.ktc2.cokaen.wouldyouin.core.ToastUtils
import org.ktc2.cokaen.wouldyouin.data.model.ReviewResponse
import org.ktc2.cokaen.wouldyouin.feat_profile.databinding.ItemCompletedReviewBinding

class CompletedReviewAdapter(
    private val onDeleteClick: (Long) -> Unit
) : ListAdapter<ReviewResponse, CompletedReviewAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCompletedReviewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onDeleteClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCompletedReviewBinding,
        private val onDeleteClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: ReviewResponse) {
            binding.apply {
                this.review = review

                deleteButton.setOnClickListener {
                    onDeleteClick(review.id)
                    ToastUtils.showShortToast(deleteButton.context, "후기가 삭제되었습니다.")
                }

                executePendingBindings()
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ReviewResponse>() {
            override fun areItemsTheSame(oldItem: ReviewResponse, newItem: ReviewResponse): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ReviewResponse, newItem: ReviewResponse): Boolean {
                return oldItem == newItem
            }
        }
    }
}