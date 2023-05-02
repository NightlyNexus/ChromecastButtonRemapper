package com.nightlynexus.chromecastbuttonremapper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager


class ButtonRemappingAccessibilityService : AccessibilityService() {
  internal companion object {
    fun isEnabled(context: Context): Boolean {
      val manager = context.getSystemService(AccessibilityManager::class.java)
      val services = manager.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
      )
      val packageName = context.packageName
      for (i in services.indices) {
        val service = services[i]
        val serviceInfo = service.resolveInfo.serviceInfo
        if (serviceInfo.packageName.equals(packageName) && serviceInfo.name.equals(
            ButtonRemappingAccessibilityService::class.java.name
          )
        ) {
          return true
        }
      }
      return false
    }

    fun settingsIntent(): Intent {
      return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
  }

  private lateinit var data: Data
  private lateinit var buttonClickNotifier: ButtonClickNotifier

  override fun onCreate() {
    val app = application as App
    data = app.data
    buttonClickNotifier = app.buttonClickNotifier
    super.onCreate()
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent) {
    // No-op.
  }

  override fun onInterrupt() {
    // No-op.
  }

  override fun onServiceConnected() {
    startActivity(
      Intent(
        this,
        MainActivity::class.java
      ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    )
  }

  override fun onKeyEvent(event: KeyEvent): Boolean {
    val key = event.keyCode
    if (buttonClickNotifier.buttonClicked(event)) {
      return true
    }
    val action = data.getAction(key) ?: return false
    if (action.componentName != null) {
      startActivity(
        Intent()
          .setComponent(action.componentName)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    } else if (action.openNotifications) {
      performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    } else if (action.search != null) {
      // TODO: Check if this device can handle this Intent (and remove the option if so).
      startActivity(
        Intent(Intent.ACTION_ASSIST)
          .putExtra(SearchManager.QUERY, action.search)
          .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    } else {
      throw IllegalStateException("Unimplemented action type: $action")
    }
    return true
  }
}
