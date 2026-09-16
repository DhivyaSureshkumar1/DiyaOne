package com.naminfo.ui.assistant.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.AssistantQrCodeScannerFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.GenericFragment
import com.naminfo.ui.assistant.viewmodel.QrCodeViewModel
import com.naminfo.ui.sso.SingleSignOnActivity

@UiThread
class QrCodeScannerFragment : GenericFragment() {
    companion object {
        private const val TAG = "[Qr Code Scanner Fragment]"
    }

    private lateinit var binding: AssistantQrCodeScannerFragmentBinding

    private val viewModel: QrCodeViewModel by navGraphViewModels(
        R.id.assistant_nav_graph
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("$TAG CAMERA permission has been granted")
            enableQrCodeVideoScanner()
        } else {
            Log.e("$TAG CAMERA permission has been denied, leaving this fragment")
            goBack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AssistantQrCodeScannerFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            goBack()
        }

        viewModel.remoteProvisioningSuccessfulEvent.observe(viewLifecycleOwner) {
            it.consume { atLeastOneAccountFound ->
                if (atLeastOneAccountFound) {
                    requireActivity().finish()
                } else {
                    goBack()
                }
            }
        }

        viewModel.onErrorEvent.observe(viewLifecycleOwner) {
            it.consume {
                // Core has restarted but something went wrong, restart video capture
                enableQrCodeVideoScanner()
            }
        }

        coreContext.bearerAuthenticationRequestedEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val serverUrl = pair.first
                val username = pair.second
                Log.i(
                    "$TAG Bearer auth requested, navigating to Single Sign On Fragment with server URL [$serverUrl] and username [$username]"
                )
                val intent = Intent(requireContext(), SingleSignOnActivity::class.java)
                intent.putExtra(SingleSignOnActivity.INTENT_EXTRA_USERNAME, username)
                intent.putExtra(SingleSignOnActivity.INTENT_EXTRA_SERVER_URL, serverUrl)
                startActivity(intent)
            }
        }

        if (!isCameraPermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
                Log.w("$TAG CAMERA permission wasn't granted yet, asking for it now")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                Log.i("$TAG Permission request for CAMERA will be automatically denied, go to android app settings instead")
                (requireActivity() as GenericActivity).goToAndroidPermissionSettings()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (isCameraPermissionGranted()) {
            Log.i(
                "$TAG Record video permission is granted, starting video preview with back cam if possible"
            )
            viewModel.setBackCamera()
            enableQrCodeVideoScanner()
        }
    }

    override fun onPause() {
        coreContext.postOnCoreThread { core ->
            core.nativePreviewWindowId = null
            core.isVideoPreviewEnabled = false
            core.isQrcodeVideoPreviewEnabled = false

            coreContext.setFrontCamera()
        }

        super.onPause()
    }

    private fun goBack() {
        findNavController().popBackStack()
    }

    private fun isCameraPermissionGranted(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        Log.i("$TAG CAMERA permission is ${if (granted) "granted" else "denied"}")
        return granted
    }

    private fun enableQrCodeVideoScanner() {
        coreContext.postOnCoreThread { core ->
            core.nativePreviewWindowId = binding.qrCodePreview
            core.isQrcodeVideoPreviewEnabled = true
            core.isVideoPreviewEnabled = true
            Log.i("$TAG Video preview with QR scanner enabled")
        }
    }
}
