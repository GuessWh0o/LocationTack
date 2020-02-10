package com.guesswho.movetracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.guesswho.movetracker.R
import com.guesswho.movetracker.database.LocationHistory
import kotlinx.android.synthetic.main.item_history.view.*
import java.util.*

/**
 *
 * @author Maxim on 2020-02-10
 */
class HistoryListAdapter(
    private val items: List<LocationHistory>,
    val onSelected: (LocationHistory) -> Unit
) :
    RecyclerView.Adapter<HistoryListAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int) = HistoryViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
    )

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.itemView.apply {
            setOnClickListener {
                onSelected(items[position])
            }
        }.let { rootView ->
            rootView.tv_dummy_title.text = items[position].session
            rootView.tv_dummy_subtitle.text = Date(items[position].time ?: 0L).toString()
        }
    }


    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view)
}