package com.naminfo.ui.fileviewer.fragment

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import org.linphone.core.tools.Log
import com.naminfo.databinding.FileMediaViewerChildFragmentBinding
import com.naminfo.ui.fileviewer.viewmodel.MediaViewModel
import com.naminfo.ui.main.fragment.GenericMainFragment
import com.naminfo.ui.main.viewmodel.SharedMainViewModel
import com.naminfo.utils.FileUtils

@UiThread
class MediaViewerFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[Media Viewer Fragment]"
    }

    private lateinit var binding: FileMediaViewerChildFragmentBinding

    private lateinit var viewModel: MediaViewModel

    private var wasVideoPlayingBeforeTrackingTouch = false

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            wasVideoPlayingBeforeTrackingTouch = viewModel.isMediaPlaying.value == true
            viewModel.pause()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            val newPosition = seekBar.progress
            viewModel.seekTo(newPosition, wasVideoPlayingBeforeTrackingTouch)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FileMediaViewerChildFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this)[SharedMainViewModel::class.java]
        }

        viewModel = ViewModelProvider(this)[MediaViewModel::class.java]
        viewModel.fullScreenMode.value = sharedViewModel.mediaViewerFullScreenMode.value == true

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        val path = if (arguments?.containsKey("path") == true) {
            requireArguments().getString("path")
        } else {
            ""
        }
        if (path.isNullOrEmpty()) {
            Log.e("$TAG Path argument not found!")
            return
        }

        val exists = FileUtils.doesFileExist(path)
        Log.i("$TAG Path argument is [$path], it ${if (exists) "exists" else "doesn't exist"}")
        viewModel.loadFile(path)

        binding.setToggleFullScreenModeClickListener {
            val fullScreenMode = viewModel.toggleFullScreen()
            sharedViewModel.mediaViewerFullScreenMode.value = fullScreenMode
        }

        binding.setSeekBarListener(seekBarListener)

        viewModel.videoSizeChangedEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val width = pair.first
                val height = pair.second
                Log.i("$TAG Updating video texture ration to ${width}x$height")
                binding.videoPlayer.setAspectRatio(width, height)
            }
        }

        viewModel.changeFullScreenModeEvent.observe(viewLifecycleOwner) {
            it.consume { fullScreenMode ->
                sharedViewModel.mediaViewerFullScreenMode.value = fullScreenMode
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val textureView = binding.videoPlayer
        if (textureView.isAvailable) {
            Log.i("$TAG Surface created, setting display in mediaPlayer")
            viewModel.setMediaPlayerSurface((Surface(textureView.surfaceTexture)))
        } else {
            Log.i("$TAG Surface not available yet, setting listener")
            textureView.surfaceTextureListener = object : SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surfaceTexture: SurfaceTexture,
                    p1: Int,
                    p2: Int
                ) {
                    Log.i("$TAG Surface available, setting display in mediaPlayer")
                    viewModel.setMediaPlayerSurface(Surface(surfaceTexture))
                }

                override fun onSurfaceTextureSizeChanged(
                    surfaceTexture: SurfaceTexture,
                    p1: Int,
                    p2: Int
                ) {
                }

                override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                    return true
                }

                override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                }
            }
        }

        viewModel.play()
    }

    override fun onPause() {
        if (viewModel.isMediaPlaying.value == true) {
            Log.i("$TAG Paused, stopping media player")
            viewModel.pause()
        }

        super.onPause()
    }
}
