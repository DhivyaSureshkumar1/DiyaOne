package com.naminfo.ui.main.contacts.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.naminfo.R
import com.naminfo.databinding.ContactsListFragmentBinding
import com.naminfo.ui.fileviewer.FileViewerActivity
import com.naminfo.ui.fileviewer.MediaViewerActivity
import com.naminfo.ui.main.contacts.adapter.ContactsListAdapter
import com.naminfo.ui.main.contacts.viewmodel.ContactsListViewModel
import com.naminfo.ui.main.contacts.viewmodel.MatchedContactsViewModel
import com.naminfo.ui.main.fragment.AbstractMainFragment
import com.naminfo.utils.Event
import org.linphone.core.tools.Log

@UiThread
class ContactsListFragment : AbstractMainFragment() {

    companion object {
        private const val TAG = "[Contacts List Fragment]"
        private const val PERMISSION_REQUESTED = "contacts_permission_requested"
    }

    private var fragmentBinding: ContactsListFragmentBinding? = null
    private val binding: ContactsListFragmentBinding
        get() = checkNotNull(fragmentBinding)

    private lateinit var listViewModel: ContactsListViewModel
    private lateinit var matchedViewModel: MatchedContactsViewModel

    private lateinit var matchedAdapter: ContactsListAdapter
    private lateinit var conferenceAdapter: ContactsListAdapter

    private var contactsPermissionRequested = false
    private var permissionRequestInProgress = false
    private var lastStatusMessage = ""

    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionRequestInProgress = false

        if (::matchedViewModel.isInitialized) {
            // The ViewModel checks permission and reports denial itself.
            matchedViewModel.reload()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactsPermissionRequested = savedInstanceState
            ?.getBoolean(PERMISSION_REQUESTED)
            ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = ContactsListFragmentBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listViewModel = ViewModelProvider(requireActivity())[
            ContactsListViewModel::class.java
        ]

        matchedViewModel = ViewModelProvider(requireActivity())[
            MatchedContactsViewModel::class.java
        ]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = listViewModel

        observeToastEvents(listViewModel)

        matchedAdapter = ContactsListAdapter(disableLongClick = true)
        conferenceAdapter = ContactsListAdapter(disableLongClick = true)

        setupLists()
        observeContacts()
        observeNavigation()

        listViewModel.title.value =
            getString(R.string.bottom_navigation_contacts_label)

        setViewModel(listViewModel)

        initViews(
            binding.slidingPaneLayout,
            binding.topBar,
            binding.bottomNavBar,
            R.id.contactsListFragment
        )
    }

    private fun setupLists() {
        // Existing lists remain hidden.
        binding.contactsList.visibility = View.GONE
        binding.contactsList.adapter = null

        binding.favouritesContactsList.visibility = View.GONE
        binding.favouritesContactsList.adapter = null

        // Loading happens automatically; no refresh button is needed.
        binding.contactsListSwipeRefresh.isEnabled = false
        binding.contactsListSwipeRefresh.isRefreshing = false

        binding.matchedContactsList?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
            adapter = matchedAdapter
        }

        binding.conferenceContactsList?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
            adapter = conferenceAdapter
        }

        observeContactClicks(matchedAdapter)
        observeContactClicks(conferenceAdapter)

        binding.setOnNewContactClicked {
            sharedViewModel.showNewContactEvent.value = Event(true)
        }
    }

    private fun observeContactClicks(adapter: ContactsListAdapter) {
        adapter.contactClickedEvent.observe(viewLifecycleOwner) { event ->
            event.consume { model ->
                sharedViewModel.displayedFriend = model.friend
                sharedViewModel.showContactEvent.value = Event(model.id)
            }
        }
    }

    private fun observeContacts() {
        matchedViewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            // Conference selection uses this complete matched list.
            // Do not replace it with the search-filtered list.
            listViewModel.contactsList.value = ArrayList(contacts)

            updateMatchedContactsDisplay()

            Log.i("$TAG Matched contacts loaded: ${contacts.size}")
        }

        listViewModel.searchFilter.observe(viewLifecycleOwner) {
            updateMatchedContactsDisplay()
        }

        matchedViewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.matchedContactsProgress?.visibility =
                if (loading) View.VISIBLE else View.GONE

            binding.contactsListSwipeRefresh.isRefreshing = false

            if (loading) {
                lastStatusMessage = ""
            }
        }

        matchedViewModel.status.observe(viewLifecycleOwner) { message ->
            if (
                message.isNotBlank() &&
                message != "Loading contacts…" &&
                message != lastStatusMessage
            ) {
                lastStatusMessage = message

                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        listViewModel.conferenceList.observe(viewLifecycleOwner) { contacts ->
            conferenceAdapter.submitList(contacts.toList())
        }

        sharedViewModel.forceRefreshContactsList.observe(
            viewLifecycleOwner
        ) { event ->
            event.consume {
                listViewModel.filter()
                matchedViewModel.reload()
            }
        }
    }

    private fun updateMatchedContactsDisplay() {
        if (fragmentBinding == null ||
            !::matchedAdapter.isInitialized ||
            !::matchedViewModel.isInitialized
        ) {
            return
        }

        val contacts = matchedViewModel.contacts.value.orEmpty()
        val query = listViewModel.searchFilter.value.orEmpty().trim()

        val filteredContacts = if (query.isBlank()) {
            contacts
        } else {
            contacts.filter { model ->
                model.contactName.orEmpty().contains(
                    query,
                    ignoreCase = true
                ) || model.id.removePrefix("mobion:").contains(query)
            }
        }

        matchedAdapter.submitList(filteredContacts.toList())
    }

    private fun observeNavigation() {
        sharedViewModel.showContactEvent.observe(
            viewLifecycleOwner
        ) { event ->
            event.consume { refKey ->
                val navController =
                    binding.contactsNavContainer.findNavController()

                val action =
                    ContactFragmentDirections.actionGlobalContactFragment(
                        refKey
                    )

                navController.navigate(action)
            }
        }

        sharedViewModel.showNewContactEvent.observe(
            viewLifecycleOwner
        ) { event ->
            event.consume {
                if (
                    findNavController().currentDestination?.id ==
                    R.id.contactsListFragment
                ) {
                    val action =
                        ContactsListFragmentDirections
                            .actionContactsListFragmentToNewContactFragment()

                    findNavController().navigate(action)
                }
            }
        }

        sharedViewModel.displayFileEvent.observe(
            viewLifecycleOwner
        ) { event ->
            event.consume { bundle ->
                if (
                    findNavController().currentDestination?.id !=
                    R.id.contactsListFragment
                ) {
                    return@consume
                }

                val path = bundle.getString("path", "").orEmpty()
                if (path.isBlank()) {
                    Log.e("$TAG Cannot open a file with an empty path")
                    return@consume
                }

                val isMedia = bundle.getBoolean("isMedia", false)

                val intent = if (isMedia) {
                    Intent(requireActivity(), MediaViewerActivity::class.java)
                } else {
                    Intent(requireActivity(), FileViewerActivity::class.java)
                }

                intent.putExtras(bundle)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!::matchedViewModel.isInitialized ||
            permissionRequestInProgress
        ) {
            return
        }

        val permissionGranted = requireContext().checkSelfPermission(
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        when {
            permissionGranted -> {
                matchedViewModel.reload()
            }

            !contactsPermissionRequested -> {
                contactsPermissionRequested = true
                permissionRequestInProgress = true

                contactsPermissionLauncher.launch(
                    Manifest.permission.READ_CONTACTS
                )
            }

            else -> {
                // Show the permission message without repeatedly prompting.
                matchedViewModel.reload()
            }
        }
    }

    override fun onDefaultAccountChanged() {
        if (::listViewModel.isInitialized) {
            listViewModel.applyCurrentDefaultAccountFilter()
        }

        if (::matchedViewModel.isInitialized) {
            matchedViewModel.reload(accountChanged = true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            PERMISSION_REQUESTED,
            contactsPermissionRequested
        )
        super.onSaveInstanceState(outState)
    }

    override fun onCreateAnimation(
        transit: Int,
        enter: Boolean,
        nextAnim: Int
    ): Animation? {
        if (
            findNavController().currentDestination?.id ==
            R.id.newContactFragment
        ) {
            return AnimationUtils.loadAnimation(activity, R.anim.hold)
        }

        return super.onCreateAnimation(transit, enter, nextAnim)
    }

    override fun onDestroyView() {
        fragmentBinding?.matchedContactsList?.adapter = null
        fragmentBinding?.conferenceContactsList?.adapter = null

        super.onDestroyView()
        fragmentBinding = null
    }
}
