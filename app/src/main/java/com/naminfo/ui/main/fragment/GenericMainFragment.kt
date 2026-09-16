package com.naminfo.ui.main.fragment

import android.os.Bundle
import android.view.View
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import org.linphone.core.tools.Log
import com.naminfo.ui.GenericFragment
import com.naminfo.ui.main.viewmodel.SharedMainViewModel

@UiThread
abstract class GenericMainFragment : GenericFragment() {
    companion object {
        private const val TAG = "[Generic Main Fragment]"
    }

    protected lateinit var sharedViewModel: SharedMainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this)[SharedMainViewModel::class.java]
        }
    }

    protected fun getFragmentRealClassName(): String {
        return "[${this.javaClass.name}]"
    }

    protected open fun goBack(): Boolean {
        Log.d("$TAG ${getFragmentRealClassName()} Going back")
        try {
            Log.d("$TAG ${getFragmentRealClassName()} Calling onBackPressed on activity dispatcher")
            requireActivity().onBackPressedDispatcher.onBackPressed()
        } catch (ise: IllegalStateException) {
            Log.w("$TAG ${getFragmentRealClassName()} Can't go back: $ise")
            return false
        }
        return true
    }
}
