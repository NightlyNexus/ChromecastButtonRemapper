package com.nightlynexus.chromecastbuttonremapper

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener


@SuppressLint("ViewConstructor")
internal class InputCustomSearchView(
  context: Context,
  key: Int,
  onInputCompleteListener: OnInputCompleteListener
) : LinearLayout(context) {
  interface OnInputCompleteListener {
    fun onInputComplete(key: Int, search: String)
  }

  init {
    orientation = VERTICAL
    val inflater = LayoutInflater.from(context)
    inflater.inflate(R.layout.input_custom_search_children, this, true)
    val prompt = findViewById<TextView>(R.id.custom_search_prompt)
    val input = findViewById<EditText>(R.id.custom_search_input)
    val resources = resources
    prompt.text = resources.getString(R.string.custom_search_prompt, keyName(resources, key))
    input.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        onInputCompleteListener.onInputComplete(key, input.text.toString())
        return@OnEditorActionListener true
      }
      false
    })
  }
}
