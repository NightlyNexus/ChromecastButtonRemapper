package com.nightlynexus.chromecastbuttonremapper

import android.content.ComponentName

internal class Action(
  val componentName: ComponentName? = null,
  val openNotifications: Boolean = false,
  val search: String? = null
)
