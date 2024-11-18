package org.ktc2.cokaen.wouldyouin.core

import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("processedTitle")
fun TextView.setProcessedTitle(title: String?) {
    title?.let {
        text = it.replace("\n", " ")
    }
}