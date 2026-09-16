package com.naminfo.ui.fileviewer.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.linphone.core.tools.Log
import com.naminfo.ui.fileviewer.fragment.MediaViewerFragment
import com.naminfo.ui.fileviewer.viewmodel.MediaListViewModel

class MediaListAdapter(
    fragmentActivity: FragmentActivity,
    private val viewModel: MediaListViewModel
) :
    FragmentStateAdapter(fragmentActivity) {
    companion object {
        private const val TAG = "[Media List Adapter]"
    }

    override fun getItemCount(): Int {
        return viewModel.mediaList.value.orEmpty().size
    }

    override fun getItemId(position: Int): Long {
        return viewModel.mediaList.value.orEmpty().getOrNull(position)?.originalPath.hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return viewModel.mediaList.value.orEmpty().any { it.originalPath.hashCode().toLong() == itemId }
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = MediaViewerFragment()
        fragment.arguments = Bundle().apply {
            val path = viewModel.mediaList.value.orEmpty().getOrNull(position)?.path
            Log.d("$TAG Path is [$path] for position [$position]")
            putString("path", path)
        }
        return fragment
    }
}
