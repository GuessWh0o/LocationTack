package com.guesswho.movetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.guesswho.movetracker.database.LocationHistoryDatabase
import com.guesswho.movetracker.ui.HistoryListAdapter
import com.guesswho.movetracker.util.Coroutines
import kotlinx.android.synthetic.main.activity_history_viewer_activty.*

class HistoryViewerActivty : AppCompatActivity() {

    private val db by lazy { LocationHistoryDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_viewer_activty)
        Coroutines.ioThenMain({
            db.locationHistoryDoa().getAll()
        }, {
            it?.let {
                val adapter = HistoryListAdapter(it)
                recyclerView.adapter = adapter
            }
        })

    }

    companion object {
       fun start(context: Context) {
           context.startActivity(Intent(context, HistoryViewerActivty::class.java))
        }
    }
}
