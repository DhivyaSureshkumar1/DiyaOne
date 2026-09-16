package com.naminfo.ui.main.settings.model

import androidx.annotation.AnyThread
import androidx.annotation.UiThread

@AnyThread
class CardDavLdapModel(val name: String, private val onClicked: (name: String) -> (Unit)) {
    @UiThread
    fun clicked() {
        onClicked.invoke(name)
    }
}
