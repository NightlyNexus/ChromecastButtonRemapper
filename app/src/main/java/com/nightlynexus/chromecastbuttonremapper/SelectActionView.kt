package com.nightlynexus.chromecastbuttonremapper

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.ExecutorService

@SuppressLint("ViewConstructor")
internal class SelectActionView(
  context: Context,
  private val onActionSelectedListener: OnActionSelectedListener,
  private val buttonClickNotifier: ButtonClickNotifier,
  private val threads: ExecutorService
) : LinearLayout(context) {
  interface OnActionSelectedListener {
    fun onComponentNameSelected(key: Int, componentName: ComponentName)
    fun onOpenNotificationsSelection(key: Int)
    fun onSearchSelected(key: Int, search: String)
    fun onCustomSearchSelected(key: Int)
  }

  private val keySelection: TextView
  private val actionList: RecyclerView
  private val actionListLoading: View

  init {
    orientation = HORIZONTAL
    gravity = Gravity.CENTER
    val inflater = LayoutInflater.from(context)
    inflater.inflate(R.layout.select_action_children, this, true)
    keySelection = findViewById(R.id.key_selection)
    actionListLoading = findViewById(R.id.action_list_loading)
    actionList = findViewById(R.id.action_list)
    actionList.layoutManager = LinearLayoutManager(context)
    actionList.addItemDecoration(
      SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.action_list_item_spacing))
    )
  }

  private lateinit var adapter: ActionListAdapter
  private var keySelected = false
  private var key = -1
  private var keyListenerComplete = false

  private val listener = object : ButtonClickNotifier.Listener {
    override fun onButtonClicked(keyEvent: KeyEvent): Boolean {
      if (keyListenerComplete) {
        return false
      }
      when (keyEvent.action) {
        KeyEvent.ACTION_DOWN -> {
          val keyCode = keyEvent.keyCode
          val resources = resources
          val keyName = keyName(resources, keyCode)
          if (!isKeyAllowed(keyCode)) {
            Toast.makeText(
              context,
              resources.getString(R.string.disallowed_key, keyName),
              Toast.LENGTH_LONG
            ).show()
            return true
          }

          key = keyCode
          keySelected = true
          keySelection.text = resources.getString(R.string.key_selection_continue, keyName)
          if (actionList.adapter == null) {
            // Still loading the list.
            actionListLoading.visibility = View.VISIBLE
          } else {
            adapter.key = keyCode
            actionList.visibility = View.VISIBLE
          }
        }

        KeyEvent.ACTION_UP -> {
          if (keySelected && keyEvent.keyCode == key) {
            keyListenerComplete = true
          }
        }
      }
      return true
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    buttonClickNotifier.addListener(listener)

    val packageManager = context.packageManager
    threads.execute {
      val launcherActivities = packageManager.getLauncherActivities().run {
        sortedWith(ResolveInfo.DisplayNameComparator(packageManager))
      }
      handler.post {
        adapter = ActionListAdapter(
          launcherActivities,
          packageManager,
          threads,
          handler,
          onActionSelectedListener
        )
        actionList.adapter = adapter
        if (keySelected) {
          adapter.key = key
          actionListLoading.visibility = View.GONE
          actionList.visibility = View.VISIBLE
        }
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    buttonClickNotifier.removeListener(listener)
  }

  internal class ActionListAdapter(
    private val activities: List<ResolveInfo>,
    private val packageManager: PackageManager,
    private val threads: ExecutorService,
    private val uiHandler: Handler,
    private val onActionSelectedListener: OnActionSelectedListener
  ) :
    RecyclerView.Adapter<ActionListAdapter.ViewHolder>() {
    var key = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val inflater = LayoutInflater.from(parent.context)
      val item = inflater.inflate(R.layout.action_list_item, parent, false)
      return ViewHolder(item, key, packageManager, uiHandler, onActionSelectedListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      when (position) {
        0 -> {
          holder.icon.setImageResource(R.drawable.open_menu)
          holder.icon.contentDescription =
            holder.icon.resources.getText(R.string.open_notifications_icon_content_description)
          holder.label.setText(R.string.open_menu)
          holder.activityInfoAlternative = 0
          holder.activityInfo = null
        }

        1 -> {
          holder.icon.setImageResource(R.drawable.assistant)
          holder.icon.contentDescription =
            holder.icon.resources.getText(R.string.assistant_icon_content_description)
          holder.label.setText(R.string.weather)
          holder.activityInfoAlternative = 1
          holder.activityInfo = null
        }

        2 -> {
          holder.icon.setImageResource(R.drawable.assistant)
          holder.icon.contentDescription =
            holder.icon.resources.getText(R.string.assistant_icon_content_description)
          holder.label.setText(R.string.custom_search)
          holder.activityInfoAlternative = 2
          holder.activityInfo = null
        }

        else -> {
          val activityInfo = activities[position - 3].activityInfo
          holder.icon.setImageDrawable(null)
          holder.icon.contentDescription = null
          holder.label.text = null
          holder.activityInfoAlternative = -1
          holder.activityInfo = activityInfo
          threads.execute(holder)
        }
      }
    }

    override fun getItemCount(): Int {
      return activities.size + 3
    }

    class ViewHolder(
      itemView: View,
      private val key: Int,
      private val packageManager: PackageManager,
      private val uiHandler: Handler,
      private val onActionSelectedListener: OnActionSelectedListener
    ) :
      RecyclerView.ViewHolder(itemView), Runnable, OnClickListener {
      val icon = itemView.findViewById<ImageView>(R.id.action_icon)!!
      val label = itemView.findViewById<TextView>(R.id.action_label)!!
      var activityInfoAlternative = -1
      @Volatile var activityInfo: ActivityInfo? = null

      init {
        itemView.setOnClickListener(this)
      }

      override fun run() {
        val activityInfo = activityInfo
        if (activityInfo == null) {
          // This is no longer a componentName action type.
          return
        }
        val label = activityInfo.loadLabel(packageManager)
        uiThreadRunnable.bind(activityInfo, label)
        uiHandler.post(uiThreadRunnable)
        val icon = activityInfo.loadIcon(packageManager)
        uiThreadRunnable.bind(activityInfo, icon)
        uiHandler.post(uiThreadRunnable)
      }

      private val uiThreadRunnable = object : Runnable {
        private lateinit var activityInfo: ActivityInfo
        private var icon: Drawable? = null
        private var label: CharSequence? = null

        fun bind(activityInfo: ActivityInfo, label: CharSequence) {
          synchronized(this) {
            this.activityInfo = activityInfo
            this.label = label
          }
        }

        fun bind(activityInfo: ActivityInfo, icon: Drawable) {
          synchronized(this) {
            this.activityInfo = activityInfo
            this.icon = icon
          }
        }

        override fun run() {
          val icon: Drawable?
          val label: CharSequence?
          synchronized(this) {
            if (activityInfo !== this@ViewHolder.activityInfo) {
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
        when (activityInfoAlternative) {
          0 -> {
            onActionSelectedListener.onOpenNotificationsSelection(key)
          }

          1 -> {
            onActionSelectedListener.onSearchSelected(
              key,
              label.resources.getString(R.string.weather_query)
            )
          }

          2 -> {
            onActionSelectedListener.onCustomSearchSelected(key)
          }

          else -> {
            val activityInfo = activityInfo!!
            onActionSelectedListener.onComponentNameSelected(
              key,
              ComponentName(
                activityInfo.packageName,
                activityInfo.name
              )
            )
          }
        }
      }
    }
  }

  private fun PackageManager.getLauncherActivities(): List<ResolveInfo> {
    return queryIntentActivities(
      Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
      })
  }
}
