package org.ktc2.cokaen.wouldyouin.feat_booking.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.ktc2.cokaen.wouldyouin.core.DateTimeUtils
import org.ktc2.cokaen.wouldyouin.data.model.CurationResponse
import org.ktc2.cokaen.wouldyouin.data.model.ReservationResponse
import org.ktc2.cokaen.wouldyouin.feat_booking.databinding.ItemReservationBinding

class BookingAdapter(
    private val reservations: List<ReservationResponse>
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private var onItemClickListener: ((ReservationResponse) -> Unit)? = null

    fun setOnItemClickListener(listener: (ReservationResponse) -> Unit) {
        onItemClickListener = listener
    }

    inner class BookingViewHolder(private val binding: ItemReservationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reservation: ReservationResponse) {
            binding.eventTitle.text = reservation.event.title

            val reservationDate = DateTimeUtils.formatDetailTimeString(reservation.reservationDate)
            val eventDate = DateTimeUtils.formatDetailTimeString(reservation.event.startTime)
            binding.reservationDate.text = "${reservationDate} 예매"
            binding.eventStartTime.text = eventDate
            Log.d("Binding", reservation.event.location.detailAddress)
            binding.eventLocation.text = reservation.event.location.detailAddress
            binding.imageUrl = reservation.event.thumbnailUrl

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(reservation)
            }

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(reservations[position])
    }

    override fun getItemCount(): Int = reservations.size
}
