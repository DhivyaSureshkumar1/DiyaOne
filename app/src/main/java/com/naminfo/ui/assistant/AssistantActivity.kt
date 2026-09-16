package com.naminfo.ui.assistant

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.UiThread
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import kotlin.math.max
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.AssistantActivityBinding
import com.naminfo.ui.GenericActivity

@UiThread
class AssistantActivity : GenericActivity() {
    companion object {
        private const val TAG = "[Assistant Activity]"

        const val SKIP_LANDING_EXTRA = "SkipLandingIfAtLeastAnAccount"
    }

    private lateinit var binding: AssistantActivityBinding

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val navController = binding.assistantNavContainer.findNavController()
            if (navController.currentDestination?.id != R.id.thirdPartySipAccountLoginFragment) {
                navController.popBackStack()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.assistant_activity)
        binding.lifecycleOwner = this
        setUpToastsArea(binding.toastsArea)

        ViewCompat.setOnApplyWindowInsetsListener(binding.assistantNavContainer) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(
                insets.left,
                insets.top,
                insets.right,
                max(insets.bottom, keyboard.bottom)
            )
            WindowInsetsCompat.CONSUMED
        }

        coreContext.postOnCoreThread { core ->
            if (core.accountList.isEmpty()) {
                Log.i("$TAG No account configured, disabling back gesture")
                coreContext.postOnMainThread {
                    // Only allow to navigate back within the assistant nav controller,
                    // not to leave the AssistantActivity
                    onBackPressedDispatcher.addCallback(backPressedCallback)
                }
            }
        }

        coreContext.mdmConfigAppliedEvent.observe(this) {
            it.consume {
                Log.i("$TAG Managed configuration applied, checking for accounts")
                leaveAssistantIfAnAccountIsConfigured()
            }
        }

        coreContext.provisioningAppliedEvent.observe(this) {
            it.consume {
                Log.i("$TAG Provisioning applied, checking for accounts")
                leaveAssistantIfAnAccountIsConfigured()
            }
        }

        (binding.root as? ViewGroup)?.doOnPreDraw {
            if (intent.getBooleanExtra(SKIP_LANDING_EXTRA, false)) {
                Log.w(
                    "$TAG We were asked to leave assistant if at least an account is already configured"
                )
                leaveAssistantIfAnAccountIsConfigured()
            }
        }
    }

    private fun leaveAssistantIfAnAccountIsConfigured() {
        coreContext.postOnCoreThread { core ->
            if (core.accountList.isNotEmpty()) {
                coreContext.postOnMainThread {
                    try {
                        Log.w("$TAG At least one account was found, leaving assistant")
                        setResult(RESULT_OK)
                        finish()
                    } catch (ise: IllegalStateException) {
                        Log.e("$TAG Can't finish activity: $ise")
                    }
                }
            }
        }
    }
}
