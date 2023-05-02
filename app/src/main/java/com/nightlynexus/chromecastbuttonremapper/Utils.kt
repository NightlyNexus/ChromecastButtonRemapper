package com.nightlynexus.chromecastbuttonremapper

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.WorkerThread

@WorkerThread
@Suppress("Deprecation")
internal fun PackageManager.queryIntentActivities(intent: Intent): List<ResolveInfo> {
  return if (SDK_INT >= 33) {
    queryIntentActivities(
      intent,
      PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
    )
  } else {
    queryIntentActivities(
      intent,
      PackageManager.MATCH_ALL
    )
  }
}
