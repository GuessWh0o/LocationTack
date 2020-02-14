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
import com.guesswho.movetracker.util.DataManager
import kotlinx.android.synthetic.main.fragment_history_viewer.*

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
    ): View? = inflater.inflate(R.layout.fragment_history_viewer, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        Coroutines.ioThenMain({
            DataManager.getLocationHistory(db)
        }, {
            it?.let {
                val adapter = HistoryListAdapter(it) {
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
