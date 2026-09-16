package com.naminfo.ui.main.contacts.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import androidx.annotation.UiThread
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.naminfo.DiyaOneApplication.Companion.coreContext
import java.io.File
import com.naminfo.R
import org.linphone.core.FriendList
import org.linphone.core.tools.Log
import com.naminfo.databinding.ContactsListFilterPopupMenuBinding
import com.naminfo.databinding.ContactsListFragmentBinding
import com.naminfo.ui.fileviewer.FileViewerActivity
import com.naminfo.ui.fileviewer.MediaViewerActivity
import com.naminfo.ui.main.contacts.adapter.ContactsListAdapter
import com.naminfo.ui.main.contacts.model.ContactAvatarModel
import com.naminfo.ui.main.contacts.viewmodel.ContactsListViewModel
import com.naminfo.ui.main.fragment.AbstractMainFragment
import com.naminfo.utils.ConfirmationDialogModel
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.Event

@UiThread
class ContactsListFragment : AbstractMainFragment() {
    companion object {
        private const val TAG = "[Contacts List Fragment]"
    }

    private lateinit var binding: ContactsListFragmentBinding

    private lateinit var listViewModel: ContactsListViewModel

    private lateinit var adapter: ContactsListAdapter
    private lateinit var conferenceAdapter: ContactsListAdapter
    private lateinit var favouritesAdapter: ContactsListAdapter

    private var bottomSheetDialog: BottomSheetDialogFragment? = null

    private val swipeToRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        Log.i("$TAG Swipe to refresh triggered, updating CardDAV friend lists")
        listViewModel.refreshCardDavContacts()
    }

    override fun onDefaultAccountChanged() {
        Log.i(
            "$TAG Default account changed, updating avatar in top bar & refreshing contacts list"
        )
        listViewModel.applyCurrentDefaultAccountFilter()
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (findNavController().currentDestination?.id == R.id.newContactFragment
        ) {
            // Holds fragment in place while new fragment slides over it
            return AnimationUtils.loadAnimation(activity, R.anim.hold)
        }
        return super.onCreateAnimation(transit, enter, nextAnim)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ContactsListAdapter()
        conferenceAdapter = ContactsListAdapter()
        favouritesAdapter = ContactsListAdapter(favourites = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ContactsListFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listViewModel = ViewModelProvider(requireActivity())[ContactsListViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = listViewModel
        observeToastEvents(listViewModel)

        // Disabled by default, may be enabled in onResume()
        binding.contactsListSwipeRefresh.isEnabled = false
        binding.contactsListSwipeRefresh.setOnRefreshListener(swipeToRefreshListener)

        binding.contactsList.setHasFixedSize(true)
        binding.contactsList.layoutManager = LinearLayoutManager(requireContext())
        binding.contactsList.outlineProvider = outlineProvider

        binding.conferenceContactsList?.setHasFixedSize(true)
        binding.conferenceContactsList?.layoutManager = LinearLayoutManager(requireContext())
        binding.conferenceContactsList?.isNestedScrollingEnabled = false

        binding.favouritesContactsList.setHasFixedSize(true)
        val favouritesLayoutManager = LinearLayoutManager(requireContext())
        favouritesLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.favouritesContactsList.layoutManager = favouritesLayoutManager

        configureAdapter(adapter)
        configureAdapter(conferenceAdapter)
        configureAdapter(favouritesAdapter)

        listViewModel.isListFiltered.observe(viewLifecycleOwner) { filtered ->
            binding.contactsList.clipToOutline = filtered
        }

        listViewModel.contactsList.observe(
            viewLifecycleOwner
        ) {
            adapter.submitList(it)

            // Wait for adapter to have items before setting it in the RecyclerView,
            // otherwise scroll position isn't retained
            if (binding.contactsList.adapter != adapter) {
                binding.contactsList.adapter = adapter
            }

            Log.i("$TAG Contacts list updated with [${it.size}] items")
            listViewModel.fetchInProgress.value = false
        }

        listViewModel.conferenceList.observe(viewLifecycleOwner) {
            conferenceAdapter.submitList(it)
            if (binding.conferenceContactsList?.adapter != conferenceAdapter) {
                binding.conferenceContactsList?.adapter = conferenceAdapter
            }
            Log.i("$TAG Conference contacts list updated with [${it.size}] items")
        }

        listViewModel.favouritesList.observe(
            viewLifecycleOwner
        ) {
            favouritesAdapter.submitList(it)

            // Wait for adapter to have items before setting it in the RecyclerView,
            // otherwise scroll position isn't retained
            if (binding.favouritesContactsList.adapter != favouritesAdapter) {
                binding.favouritesContactsList.adapter = favouritesAdapter
            }

            Log.i("$TAG Favourites contacts list updated with [${it.size}] items")
        }

        listViewModel.vCardTerminatedEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val contactName = pair.first
                val file = pair.second
                Log.i(
                    "$TAG Friend [$contactName] was exported as vCard file [${file.absolutePath}], sharing it"
                )
                shareContact(contactName, file)
            }
        }

        listViewModel.cardDavSynchronizationCompletedEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG CardDAV synchronization has completed")
                binding.contactsListSwipeRefresh.isRefreshing = false
            }
        }

        binding.setOnNewContactClicked {
            sharedViewModel.showNewContactEvent.value = Event(true)
        }

        binding.setFilterClickListener {
            showFilterPopupMenu(binding.topBar.extraAction)
        }

        sharedViewModel.showContactEvent.observe(viewLifecycleOwner) {
            it.consume { refKey ->
                Log.i("$TAG Displaying contact with ref key [$refKey]")
                val navController = binding.contactsNavContainer.findNavController()
                val action = ContactFragmentDirections.actionGlobalContactFragment(
                    refKey
                )
                navController.navigate(action)
            }
        }

        sharedViewModel.showNewContactEvent.observe(viewLifecycleOwner) {
            it.consume {
                if (findNavController().currentDestination?.id == R.id.contactsListFragment) {
                    Log.i("$TAG Opening contact editor for creating new contact")
                    val action =
                        ContactsListFragmentDirections.actionContactsListFragmentToNewContactFragment()
                    findNavController().navigate(action)
                }
            }
        }

        sharedViewModel.forceRefreshContactsList.observe(viewLifecycleOwner) {
            it.consume {
                listViewModel.filter()
            }
        }

        sharedViewModel.displayFileEvent.observe(viewLifecycleOwner) {
            it.consume { bundle ->
                if (findNavController().currentDestination?.id == R.id.contactsListFragment) {
                    val path = bundle.getString("path", "")
                    val isMedia = bundle.getBoolean("isMedia", false)
                    if (path.isEmpty()) {
                        Log.e("$TAG Can't navigate to file viewer for empty path!")
                        return@consume
                    }

                    Log.i(
                        "$TAG Navigating to [${if (isMedia) "media" else "file"}] viewer fragment with path [$path]"
                    )
                    if (isMedia) {
                        val intent = Intent(requireActivity(), MediaViewerActivity::class.java)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    } else {
                        val intent = Intent(requireActivity(), FileViewerActivity::class.java)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    }
                }
            }
        }

        // AbstractMainFragment related

        listViewModel.title.value = getString(R.string.bottom_navigation_contacts_label)
        setViewModel(listViewModel)
        initViews(
            binding.slidingPaneLayout,
            binding.topBar,
            binding.bottomNavBar,
            R.id.contactsListFragment
        )

    }

    override fun onPause() {
        super.onPause()

        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }

    private var contactsPermissionRequested = false

    private val contactsPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            (requireActivity() as com.naminfo.ui.main.MainActivity).loadContacts()
        }
        listViewModel.filter()
    }

    override fun onResume() {
        super.onResume()

        if (requireContext().checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            (requireActivity() as com.naminfo.ui.main.MainActivity).loadContacts()
        } else if (!contactsPermissionRequested) {
            contactsPermissionRequested = true
            contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }
        listViewModel.filter()

        coreContext.postOnCoreThread { core ->
            val cardDavFriendList = core.friendsLists.find {
                it.type == FriendList.Type.CardDAV
            }
            val cardDavFriendListFound = cardDavFriendList != null
            if (cardDavFriendListFound) {
                Log.i("$TAG CardDAV friend list [${cardDavFriendList.displayName}] found, enabling swipe to refresh")
            } else {
                Log.i("$TAG No CardDAV friend list was found, disabling swipe to refresh")
            }
            coreContext.postOnMainThread {
                binding.contactsListSwipeRefresh.isEnabled = cardDavFriendListFound
            }
        }
    }

    private fun configureAdapter(adapter: ContactsListAdapter) {
        adapter.contactLongClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                val modalBottomSheet = ContactsListMenuDialogFragment(
                    model.isFavourite.value == true,
                    model.isStored,
                    isReadOnly = model.isReadOnly || model.friend.refKey.orEmpty().startsWith("conference:permanent:"),
                    isNative = model.isNative,
                    { // onDismiss
                        adapter.resetSelection()
                    },
                    { // onFavourite
                        listViewModel.toggleContactFavoriteFlag(model)
                    },
                    { // onShare
                        Log.i(
                            "$TAG Sharing friend [${model.name.value}], exporting it as vCard file first"
                        )
                        listViewModel.exportContactAsVCard(model.friend)
                    },
                    { // onDelete
                        showDeleteConfirmationDialog(model)
                    }
                )
                modalBottomSheet.show(parentFragmentManager, ContactsListMenuDialogFragment.TAG)
                bottomSheetDialog = modalBottomSheet
            }
        }

        adapter.contactClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                sharedViewModel.displayedFriend = model.friend
                sharedViewModel.showContactEvent.value = Event(model.id)
            }
        }
    }

    private fun shareContact(name: String, file: File) {
        val publicUri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().getString(R.string.file_provider),
            file
        )
        Log.i("$TAG Public URI for vCard file is [$publicUri], starting intent chooser")

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, publicUri)
            putExtra(Intent.EXTRA_SUBJECT, name)
            type = ContactsContract.Contacts.CONTENT_VCARD_TYPE
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        try {
            startActivity(shareIntent)
        } catch (anfe: ActivityNotFoundException) {
            Log.e("$TAG Failed to start intent chooser: $anfe")
        }
    }

    private fun showFilterPopupMenu(view: View) {
        val popupView: ContactsListFilterPopupMenuBinding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.contacts_list_filter_popup_menu,
            null,
            false
        )
        popupView.seeAllSelected = listViewModel.areAllContactsDisplayed.value == true
        popupView.showLinphoneFilter = listViewModel.isDefaultAccountLinphone.value == true

        val popupWindow = PopupWindow(
            popupView.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.setNoFilterClickListener {
            if (listViewModel.areAllContactsDisplayed.value != true) {
                listViewModel.changeContactsFilter(
                    onlyLinphoneContacts = false,
                    onlySipContacts = false
                )
            }
            popupWindow.dismiss()
        }

        popupView.setLinphoneOnlyClickListener {
            if (listViewModel.areAllContactsDisplayed.value == true) {
                listViewModel.changeContactsFilter(
                    onlyLinphoneContacts = true,
                    onlySipContacts = false
                )
            }
            popupWindow.dismiss()
        }

        popupView.setSipOnlyClickListener {
            if (listViewModel.areAllContactsDisplayed.value == true) {
                listViewModel.changeContactsFilter(
                    onlyLinphoneContacts = false,
                    onlySipContacts = true
                )
            }
            popupWindow.dismiss()
        }

        // Elevation is for showing a shadow around the popup
        popupWindow.elevation = 20f
        popupWindow.showAsDropDown(view, 0, 0, Gravity.BOTTOM)
    }

    private fun showDeleteConfirmationDialog(contactModel: ContactAvatarModel) {
        val model = ConfirmationDialogModel()
        val dialog = DialogUtils.getDeleteContactConfirmationDialog(
            requireActivity(),
            model,
            contactModel.contactName.orEmpty()
        )

        model.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialog.dismiss()
            }
        }

        model.confirmEvent.observe(viewLifecycleOwner) {
            it.consume {
                listViewModel.deleteContact(contactModel)
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
