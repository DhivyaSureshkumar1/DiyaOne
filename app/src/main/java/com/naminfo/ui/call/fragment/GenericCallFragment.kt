package com.naminfo.ui.call.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Outline
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.ui.GenericFragment
import com.naminfo.ui.call.view.RoundCornersTextureView
import com.naminfo.ui.call.viewmodel.SharedCallViewModel

@UiThread
abstract class GenericCallFragment : GenericFragment() {
    companion object {
        private const val TAG = "[Generic Call Fragment]"
    }

    protected lateinit var sharedViewModel: SharedCallViewModel

    protected val outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            val radius = resources.getDimension(R.dimen.top_list_item_rounded_corner_radius)
            view ?: return
            outline?.setRoundRect(0, 0, view.width, (view.height + radius).toInt(), radius)
        }
    }

    // For moving video preview purposes
    private val videoPreviewTouchListener = View.OnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                sharedViewModel.videoPreviewX = view.x - event.rawX
                sharedViewModel.videoPreviewY = view.y - event.rawY
                true
            }
            MotionEvent.ACTION_UP -> {
                sharedViewModel.videoPreviewX = view.x
                sharedViewModel.videoPreviewY = view.y
                sharedViewModel.videoPreviewOrientation = resources.configuration.orientation
                true
            }
            MotionEvent.ACTION_MOVE -> {
                view.animate()
                    .x(event.rawX + sharedViewModel.videoPreviewX)
                    .y(event.rawY + sharedViewModel.videoPreviewY)
                    .setDuration(0)
                    .start()
                true
            }
            else -> {
                view.performClick()
                false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this)[SharedCallViewModel::class.java]
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    protected fun setupVideoPreview(localPreviewVideoSurface: RoundCornersTextureView) {
        if (requireActivity().isInPictureInPictureMode) {
            Log.i("$TAG Activity is in PiP mode, do not move video preview")
            return
        }

        // To restore video preview position if possible
        val currentOrientation = resources.configuration.orientation
        if (sharedViewModel.videoPreviewOrientation == currentOrientation || sharedViewModel.videoPreviewOrientation == Configuration.ORIENTATION_UNDEFINED) {
            if (sharedViewModel.videoPreviewX != 0f && sharedViewModel.videoPreviewY != 0f) {
                Log.i("$TAG Restoring video preview position with position X [${sharedViewModel.videoPreviewX}] and Y [${sharedViewModel.videoPreviewY}]")
                localPreviewVideoSurface.x = sharedViewModel.videoPreviewX
                localPreviewVideoSurface.y = sharedViewModel.videoPreviewY
            }
        }

        localPreviewVideoSurface.setOnTouchListener(videoPreviewTouchListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    protected fun cleanVideoPreview(localPreviewVideoSurface: RoundCornersTextureView) {
        localPreviewVideoSurface.setOnTouchListener(null)
    }
}
