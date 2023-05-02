package com.nightlynexus.chromecastbuttonremapper

import android.app.AlertDialog
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class MainActivity : FragmentActivity() {
  private lateinit var data: Data
  private lateinit var buttonClickNotifier: ButtonClickNotifier
  private lateinit var enableAccessibilityPrompt: View
  private lateinit var enableAccessibilityPromptButton: View
  private lateinit var addButton: View
  private lateinit var list: RecyclerView

  override fun onCreate(savedInstanceState: Bundle?) {
    val app = application as App
    data = app.data
    buttonClickNotifier = app.buttonClickNotifier
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    enableAccessibilityPrompt = findViewById(R.id.enable_accessibility_prompt)
    enableAccessibilityPromptButton = findViewById(R.id.enable_accessibility_prompt_button)
    addButton = findViewById(R.id.add)
    list = findViewById(R.id.list)

    enableAccessibilityPromptButton.setOnClickListener {
      val intent = ButtonRemappingAccessibilityService.settingsIntent()
      startActivity(intent)
    }

    val threads = Executors.newCachedThreadPool()
    val uiHandler = Handler(Looper.getMainLooper())
    val onButtonSelectedListener = object : ButtonListAdapter.OnButtonSelectedListener {
      override fun onButtonSelected(position: Int) {
        data.removeAction(position)
        adapter.notifyItemRemoved(position)
      }

      override fun onActionInvalid(key: Int) {
        val position = data.removeActionByKey(key)
        adapter.notifyItemRemoved(position)
      }
    }
    adapter = ButtonListAdapter(data, packageManager, threads, uiHandler, onButtonSelectedListener)
    val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    list.adapter = adapter
    list.layoutManager = layoutManager
    list.addItemDecoration(DividerItemDecoration(getDrawable(R.drawable.divider)!!))

    val onButtonConfiguredListener = object : SelectActionView.OnActionSelectedListener {
      override fun onComponentNameSelected(key: Int, componentName: ComponentName) {
        val action = Action(componentName = componentName)
        addAction(key, action)
      }

      override fun onOpenNotificationsSelection(key: Int) {
        val action = Action(openNotifications = true)
        addAction(key, action)
      }

      override fun onSearchSelected(key: Int, search: String) {
        val action = Action(search = search)
        addAction(key, action)
      }

      override fun onCustomSearchSelected(key: Int) {
        dialog.dismiss()
        dialog = AlertDialog.Builder(this@MainActivity)
          .setView(
            InputCustomSearchView(
              this@MainActivity,
              key,
              onInputCompleteListener
            )
          )
          .create()
        dialog.show()
      }

      private val onInputCompleteListener = object : InputCustomSearchView.OnInputCompleteListener {
        override fun onInputComplete(key: Int, search: String) {
          val action = Action(search = search)
          addAction(key, action)
        }
      }

      private fun addAction(key: Int, action: Action) {
        dialog.dismiss()
        data.addAction(key, action) {
          adapter.notifyItemRemoved(it)
        }
        adapter.notifyItemInserted(0)
        list.scrollToPosition(0)
      }
    }

    addButton.setOnClickListener {
      dialog = AlertDialog.Builder(this)
        .setView(
          SelectActionView(
            this,
            onButtonConfiguredListener,
            buttonClickNotifier,
            threads
          )
        )
        .create()
      dialog.show()
    }
  }

  override fun onResume() {
    super.onResume()
    if (ButtonRemappingAccessibilityService.isEnabled(this)) {
      enableAccessibilityPrompt.visibility = View.GONE
      addButton.visibility = View.VISIBLE
      list.visibility = View.VISIBLE
      if (currentFocus == null) {
        addButton.requestFocus()
      }
    } else {
      enableAccessibilityPrompt.visibility = View.VISIBLE
      addButton.visibility = View.GONE
      list.visibility = View.GONE
      enableAccessibilityPromptButton.requestFocus()
    }
  }

  private lateinit var adapter: ButtonListAdapter
  private lateinit var dialog: AlertDialog
}
