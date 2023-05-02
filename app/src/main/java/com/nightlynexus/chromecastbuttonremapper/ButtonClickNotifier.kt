package com.nightlynexus.chromecastbuttonremapper

import android.view.KeyEvent

internal class ButtonClickNotifier {
  interface Listener {
    fun onButtonClicked(keyEvent: KeyEvent): Boolean
  }

  private val listeners = mutableListOf<Listener>()

  fun buttonClicked(keyEvent: KeyEvent): Boolean {
    var consumed = false
    for (i in listeners.indices) {
      val listener = listeners[i]
      if (listener.onButtonClicked(keyEvent)) {
        consumed = true
      }
    }
    return consumed
  }

  fun addListener(listener: Listener) {
    listeners += listener
  }

  fun removeListener(listener: Listener) {
    listeners -= listener
  }
}
