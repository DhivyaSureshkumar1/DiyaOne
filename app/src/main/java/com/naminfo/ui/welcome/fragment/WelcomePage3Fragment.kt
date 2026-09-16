package com.naminfo.ui.welcome.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import com.naminfo.R
import com.naminfo.ui.GenericFragment

@UiThread
class WelcomePage3Fragment : GenericFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.welcome_page_3, container, false)
}
