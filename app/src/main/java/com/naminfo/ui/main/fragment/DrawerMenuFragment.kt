package com.naminfo.ui.main.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.BuildConfig
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.DrawerMenuBinding
import com.naminfo.ui.main.MainActivity
import com.naminfo.ui.main.settings.fragment.AccountProfileFragmentDirections
import com.naminfo.ui.main.viewmodel.DrawerMenuViewModel
import androidx.core.net.toUri

@UiThread
class DrawerMenuFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[Drawer Menu Fragment]"
    }

    private lateinit var binding: DrawerMenuBinding

    private lateinit var viewModel: DrawerMenuViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DrawerMenuBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = requireActivity().run {
            ViewModelProvider(this)[DrawerMenuViewModel::class.java]
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setSettingsClickedListener {
            val navController = (requireActivity() as MainActivity).findNavController()
            navController.navigate(R.id.action_global_settingsFragment)
            (requireActivity() as MainActivity).closeDrawerMenu()
        }

        binding.version.text = getString(R.string.drawer_menu_version_title, BuildConfig.VERSION_NAME)

        binding.setInviteClickedListener {
            val inviteIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.drawer_menu_invite_text))
            }
            startActivity(Intent.createChooser(inviteIntent, getString(R.string.drawer_menu_invite_chooser)))
            (requireActivity() as MainActivity).closeDrawerMenu()
        }

        binding.setVersionClickedListener {
            (requireActivity() as MainActivity).closeDrawerMenu()
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.drawer_menu_version_title, BuildConfig.VERSION_NAME))
                .setMessage(R.string.drawer_menu_version_changes)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        binding.setQuitClickedListener {
            coreContext.stopKeepAliveService()

            coreContext.postOnCoreThread {
                Log.i("$TAG Stopping Core Context")
                coreContext.quitSafely()
            }

            Log.i("$TAG Quitting app")
            requireActivity().finishAndRemoveTask()
        }

        viewModel.startAssistantEvent.observe(viewLifecycleOwner) {
            it.consume {
                (requireActivity() as MainActivity).showAssistant()
                (requireActivity() as MainActivity).closeDrawerMenu()
            }
        }

        viewModel.closeDrawerEvent.observe(viewLifecycleOwner) {
            it.consume {
                (requireActivity() as MainActivity).closeDrawerMenu()
            }
        }

        viewModel.openAccountProfileEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                val navController = (requireActivity() as MainActivity).findNavController()
                val action = AccountProfileFragmentDirections.actionGlobalAccountProfileFragment(
                    model.identity
                )
                Log.i("$TAG Going to account [${model.identity}] profile")
                navController.navigate(action)
                (requireActivity() as MainActivity).closeDrawerMenu()
            }
        }

        viewModel.defaultAccountChangedEvent.observe(viewLifecycleOwner) {
            it.consume { identity ->
                Log.w(
                    "$TAG Default account has changed, now is [$identity], closing side menu in 500ms"
                )

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        delay(500)
                        withContext(Dispatchers.Main) {
                            (requireActivity() as MainActivity).closeDrawerMenu()
                        }
                    }
                }
            }
        }

        viewModel.openLinkInBrowserEvent.observe(viewLifecycleOwner) {
            it.consume { link ->
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, link.toUri())
                    startActivity(browserIntent)
                } catch (ise: IllegalStateException) {
                    Log.e(
                        "$TAG Can't start ACTION_VIEW intent for URL [$link], IllegalStateException: $ise"
                    )
                } catch (anfe: ActivityNotFoundException) {
                    Log.e(
                        "$TAG Can't start ACTION_VIEW intent for URL [$link], ActivityNotFoundException: $anfe"
                    )
                } catch (e: Exception) {
                    Log.e(
                        "$TAG Can't start ACTION_VIEW intent for URL [$link]: $e"
                    )
                }
            }
        }

        sharedViewModel.refreshDrawerMenuAccountsListEvent.observe(viewLifecycleOwner) {
            it.consume { recreate ->
                if (recreate) {
                    viewModel.updateAccountsList()
                } else {
                    viewModel.refreshAccountsNotificationsCount()
                }
            }
        }

        sharedViewModel.refreshDrawerMenuQuitButtonEvent.observe(viewLifecycleOwner) {
            it.consume {
                coreContext.postOnCoreThread {
                    viewModel.checkIfKeepAliveServiceIsEnabled()
                }
            }
        }
    }
}
