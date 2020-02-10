package com.guesswho.movetracker.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.guesswho.movetracker.R
import com.guesswho.movetracker.database.LocationHistoryDatabase
import com.guesswho.movetracker.util.Coroutines
import kotlinx.android.synthetic.main.activity_history_viewer_activty.*

class HistoryViewerFragment : Fragment() {

    private val db by lazy { LocationHistoryDatabase(mContext) }

    private lateinit var mContext: Context
    private lateinit var navController: NavController
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_history_viewer_activty, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        Coroutines.ioThenMain({
            db.locationHistoryDoa().getAll()
        }, {
            it?.let {
                val locationSource = it.distinctBy { it.session }
                val adapter = HistoryListAdapter(locationSource) {
                    val action =
                        HistoryViewerFragmentDirections.actionHistoryViewerActivtyToHistoryFragment3()
                    action.sessionId = it.session.toString()
                    navController.navigate(action)
                }
                recyclerView.adapter = adapter
            }
        })

    }
}
