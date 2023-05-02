package com.nightlynexus.chromecastbuttonremapper

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.ExecutorService

internal class ButtonListAdapter(
  private val data: Data,
  private val packageManager: PackageManager,
  private val threads: ExecutorService,
  private val uiHandler: Handler,
  private val onButtonSelectedListener: OnButtonSelectedListener
) :
  RecyclerView.Adapter<ButtonListAdapter.ViewHolder>() {
  interface OnButtonSelectedListener {
    fun onButtonSelected(position: Int)

    fun onActionInvalid(key: Int)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val item = inflater.inflate(R.layout.list_item, parent, false)
    return ViewHolder(item, packageManager, uiHandler, onButtonSelectedListener)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val mapping = data.getKeyAndActionAt(position)
    holder.key.text = keyName(holder.key.resources, mapping.key)
    synchronized(holder) {
      holder.componentName = mapping.action.componentName
      holder.keyCode = mapping.key
    }
    val action = mapping.action
    if (action.componentName != null) {
      holder.icon.setImageDrawable(null)
      holder.icon.contentDescription = null
      holder.label.text = null
      threads.execute(holder)
    } else if (action.openNotifications) {
      holder.icon.setImageResource(R.drawable.open_menu)
      holder.icon.contentDescription =
        holder.icon.resources.getText(R.string.open_notifications_icon_content_description)
      holder.label.setText(R.string.open_menu)
    } else if (action.search != null) {
      holder.icon.setImageResource(R.drawable.assistant)
      holder.icon.contentDescription =
        holder.icon.resources.getText(R.string.assistant_icon_content_description)
      holder.label.text = holder.label.resources.getString(R.string.search, action.search)
    } else {
      throw IllegalStateException("Unimplemented action type: $action")
    }
  }

  override fun getItemCount(): Int {
    return data.size
  }

  class ViewHolder(
    itemView: View,
    private val packageManager: PackageManager,
    private val uiHandler: Handler,
    private val onButtonSelectedListener: OnButtonSelectedListener
  ) :
    RecyclerView.ViewHolder(itemView), Runnable, View.OnClickListener {
    val key = itemView.findViewById<TextView>(R.id.key)!!
    val icon = itemView.findViewById<ImageView>(R.id.action_icon)!!
    val label = itemView.findViewById<TextView>(R.id.action_label)!!
    var componentName: ComponentName? = null
    var keyCode: Int = -1

    init {
      itemView.setOnClickListener(this)
    }

    override fun run() {
      val componentName: ComponentName?
      val keyCode: Int
      synchronized(this) {
        componentName = this@ViewHolder.componentName
        keyCode = this@ViewHolder.keyCode
      }
      if (componentName == null) {
        // This is no longer a componentName action type.
        return
      }
      val activityInfo = packageManager.queryIntentActivities(
        Intent().setComponent(componentName)
      ).run {
        if (isEmpty()) {
          // The action is no longer valid. Remove the mapping.
          // We can't remove by position because we would need to wait for all the invalid positions
          // to begin removing them from the backing data.
          uiHandler.post { onButtonSelectedListener.onActionInvalid(keyCode) }
          return
        }
        check(size == 1)
        first().activityInfo
      }
      val label = activityInfo.loadLabel(packageManager)
      uiThreadRunnable.bind(componentName, label)
      uiHandler.post(uiThreadRunnable)
      val icon = activityInfo.loadIcon(packageManager)
      uiThreadRunnable.bind(componentName, icon)
      uiHandler.post(uiThreadRunnable)
    }

    private val uiThreadRunnable = object : Runnable {
      private lateinit var componentName: ComponentName
      private var icon: Drawable? = null
      private var label: CharSequence? = null

      fun bind(componentName: ComponentName, label: CharSequence) {
        synchronized(this) {
          this.componentName = componentName
          this.label = label
        }
      }

      fun bind(componentName: ComponentName, icon: Drawable) {
        synchronized(this) {
          this.componentName = componentName
          this.icon = icon
        }
      }

      override fun run() {
        val icon: Drawable?
        val label: CharSequence?
        synchronized(this) {
          // this@ViewHolder.componentName doesn't need to be synchronized here for this read on the
          // main thread because it is only ever written on the main thread.
          if (componentName !== this@ViewHolder.componentName) {
            return
          }
          icon = this.icon
          label = this.label
        }
        this@ViewHolder.icon.setImageDrawable(icon)
        if (label != null) {
          this@ViewHolder.icon.contentDescription =
            this@ViewHolder.icon.resources.getString(R.string.app_icon_content_description, label)
        }
        this@ViewHolder.label.text = label
      }
    }

    override fun onClick(v: View) {
      onButtonSelectedListener.onButtonSelected(adapterPosition)
    }
  }
}