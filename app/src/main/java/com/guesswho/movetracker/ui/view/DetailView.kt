package com.guesswho.movetracker.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.getStringOrThrow
import com.guesswho.movetracker.R
import kotlinx.android.synthetic.main.card_ride_detail.view.*

/**
 *
 * @author Maxim on 2020-02-12
 */
class DetailView (context: Context, attributeSet: AttributeSet?) : CardView(context, attributeSet) {

    lateinit var tvTitle: TextView

    init {
        inflate(context, R.layout.card_ride_detail, this)
        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.CardDetail,
            0, 0).apply {
            try {
                val title = this.getStringOrThrow(R.styleable.CardDetail_title)
                val detail = this.getStringOrThrow(R.styleable.CardDetail_detail)
                tvTitle = tv_title
                tvTitle.text = title
                tv_details.text = detail
            } catch (e: Exception) {
                e.printStackTrace()
                rootView.visibility = View.GONE
            } finally {
                this.recycle()
            }
        }
    }

    fun setTitle(text: String) {
        invalidate()
    }
}