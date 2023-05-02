package com.nightlynexus.chromecastbuttonremapper

import android.app.Application

class App : Application() {
  internal lateinit var data: Data
  internal lateinit var buttonClickNotifier: ButtonClickNotifier

  override fun onCreate() {
    super.onCreate()
    data = Data(getSharedPreferences("buttons", MODE_PRIVATE))
    buttonClickNotifier = ButtonClickNotifier()
  }
}
