package com.naminfo.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.WelcomeActivityBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.assistant.AssistantActivity
import com.naminfo.ui.welcome.fragment.WelcomePage1Fragment
import com.naminfo.ui.welcome.fragment.WelcomePage2Fragment
import com.naminfo.ui.welcome.fragment.WelcomePage3Fragment
import com.naminfo.utils.AppUtils

class WelcomeActivity : GenericActivity() {
    companion object {
        private const val TAG = "[Welcome Activity]"
        private const val PAGES = 3
    }

    private lateinit var binding: WelcomeActivityBinding

    private lateinit var viewPager: ViewPager2

    private val pageChangedCallback = PageChangedCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Disable back gesture / button
        onBackPressedDispatcher.addCallback { }

        binding = DataBindingUtil.setContentView(this, R.layout.welcome_activity)
        binding.lifecycleOwner = this

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        viewPager = binding.pager
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter

        binding.dotsIndicator.attachTo(viewPager)

        binding.setSkipClickListener {
            Log.i("$TAG User clicked on 'skip' button, going to Assistant")
            goToAssistant()
        }

        binding.setNextClickListener {
            if (viewPager.currentItem == PAGES - 1) {
                Log.i(
                    "$TAG User clicked on 'start' button, leaving activity and going into Assistant"
                )
                goToAssistant()
            } else {
                viewPager.currentItem += 1
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewPager.registerOnPageChangeCallback(pageChangedCallback)
    }

    override fun onPause() {
        viewPager.unregisterOnPageChangeCallback(pageChangedCallback)
        super.onPause()
    }

    private fun goToAssistant() {
        finish()
        val intent = Intent(this, AssistantActivity::class.java)
        intent.putExtra(AssistantActivity.SKIP_LANDING_EXTRA, true)
        startActivity(intent)
    }

    private class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = PAGES

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WelcomePage1Fragment()
                1 -> WelcomePage2Fragment()
                else -> WelcomePage3Fragment()
            }
        }
    }

    private inner class PageChangedCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            Log.i("$TAG Current page is [$position]")
            if (position == PAGES - 1) {
                binding.next.text = AppUtils.getString(R.string.start)
                binding.skip.visibility = View.INVISIBLE
            } else {
                binding.next.text = AppUtils.getString(R.string.next)
                binding.skip.visibility = View.VISIBLE
            }
        }
    }
}
