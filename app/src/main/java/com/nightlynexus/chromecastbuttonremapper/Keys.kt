package com.nightlynexus.chromecastbuttonremapper

import android.content.res.Resources
import android.view.KeyEvent

internal fun isKeyAllowed(keyCode: Int): Boolean {
  return when (keyCode) {
    KeyEvent.KEYCODE_HOME -> false
    KeyEvent.KEYCODE_BACK -> false
    KeyEvent.KEYCODE_DPAD_DOWN -> false
    KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> false
    KeyEvent.KEYCODE_DPAD_DOWN_RIGHT -> false
    KeyEvent.KEYCODE_DPAD_UP -> false
    KeyEvent.KEYCODE_DPAD_UP_LEFT -> false
    KeyEvent.KEYCODE_DPAD_UP_RIGHT -> false
    KeyEvent.KEYCODE_DPAD_LEFT -> false
    KeyEvent.KEYCODE_DPAD_RIGHT -> false
    KeyEvent.KEYCODE_DPAD_CENTER -> false
    else -> true
  }
}

internal fun keyName(resources: Resources, keyCode: Int): CharSequence {
  return resources.getText(
    when (keyCode) {
      KeyEvent.KEYCODE_HOME -> R.string.key_home
      KeyEvent.KEYCODE_BACK -> R.string.key_back
      KeyEvent.KEYCODE_DPAD_DOWN,
      KeyEvent.KEYCODE_DPAD_DOWN_LEFT,
      KeyEvent.KEYCODE_DPAD_DOWN_RIGHT,
      KeyEvent.KEYCODE_DPAD_UP,
      KeyEvent.KEYCODE_DPAD_UP_LEFT,
      KeyEvent.KEYCODE_DPAD_UP_RIGHT,
      KeyEvent.KEYCODE_DPAD_LEFT,
      KeyEvent.KEYCODE_DPAD_RIGHT,
      KeyEvent.KEYCODE_DPAD_CENTER -> R.string.key_d_pad

      KeyEvent.KEYCODE_BUTTON_1 -> R.string.key_button_1
      KeyEvent.KEYCODE_BUTTON_2 -> R.string.key_button_2
      KeyEvent.KEYCODE_BUTTON_3 -> R.string.key_button_3
      KeyEvent.KEYCODE_BUTTON_4 -> R.string.key_button_4
      KeyEvent.KEYCODE_BUTTON_5 -> R.string.key_button_5
      KeyEvent.KEYCODE_BUTTON_6 -> R.string.key_button_6
      KeyEvent.KEYCODE_BUTTON_7 -> R.string.key_button_7
      KeyEvent.KEYCODE_BUTTON_8 -> R.string.key_button_8
      KeyEvent.KEYCODE_BUTTON_9 -> R.string.key_button_9
      KeyEvent.KEYCODE_BUTTON_10 -> R.string.key_button_10
      KeyEvent.KEYCODE_BUTTON_11 -> R.string.key_button_11
      KeyEvent.KEYCODE_BUTTON_12 -> R.string.key_button_12
      KeyEvent.KEYCODE_BUTTON_13 -> R.string.key_button_13
      KeyEvent.KEYCODE_BUTTON_14 -> R.string.key_button_14
      KeyEvent.KEYCODE_BUTTON_15 -> R.string.key_button_15
      KeyEvent.KEYCODE_BUTTON_16 -> R.string.key_button_16
      else -> return resources.getString(R.string.unknown_key, keyCode)
    }
  )
}
