package com.nightlynexus.chromecastbuttonremapper

import android.content.ComponentName
import android.content.SharedPreferences

internal class Data(private val sharedPreferences: SharedPreferences) {
  class Mapping(val sharedPreferencesKey: Int, val key: Int, val action: Action)

  private val map: MutableMap<Int, Action> = mutableMapOf()
  private val list: MutableList<Mapping> = mutableListOf()

  init {
    val all = sharedPreferences.all
    for (entry in all) {
      val sharedPreferencesKey = entry.key
      val value = entry.value as String
      val delimiter1 = value.indexOf('\n')
      val key = value.substring(0, delimiter1).toInt()
      val delimiter2 = value.indexOf('\n', delimiter1 + 1)
      val type = if (delimiter2 == -1) {
        1
      } else {
        value.substring(delimiter1 + 1, delimiter2).toInt()
      }
      val action = when (type) {
        0 -> {
          val delimiter3 = value.indexOf('\n', delimiter2 + 1)
          val packageName = value.substring(delimiter2 + 1, delimiter3)
          val className = value.substring(delimiter3 + 1)
          Action(componentName = ComponentName(packageName, className))
        }

        1 -> {
          Action(openNotifications = true)
        }

        2 -> {
          val search = value.substring(delimiter2 + 1)
          Action(search = search)
        }

        else -> {
          throw IllegalStateException("Unknown action type: $type")
        }
      }

      map[key] = action
      list += Mapping(sharedPreferencesKey.toInt(), key, action)
    }
    list.sortBy {
      it.sharedPreferencesKey
    }
  }

  val size: Int
    get() = list.size

  fun getAction(key: Int): Action? {
    return map[key]
  }

  fun getKeyAndActionAt(index: Int): Mapping {
    return list[reverseIndex(index)]
  }

  private fun reverseIndex(index: Int): Int {
    return list.size - 1 - index
  }

  inline fun addAction(key: Int, action: Action, removedIndex: (Int) -> Unit) {
    if (map.put(key, action) != null) {
      for (i in list.indices) {
        val mapping = list[i]
        if (mapping.key == key) {
          sharedPreferences
            .edit()
            .remove(mapping.sharedPreferencesKey.toString())
            .apply()
          list.removeAt(i)
          removedIndex(reverseIndex(i))
          break
        }
      }
    }
    val sharedPreferencesKey = if (list.isEmpty()) {
      0
    } else {
      list.last().sharedPreferencesKey + 1
    }
    sharedPreferences
      .edit()
      .putString(
        sharedPreferencesKey.toString(),
        encodeKeyCodeAndActionValue(key, action)
      )
      .apply()
    list += Mapping(sharedPreferencesKey, key, action)
  }

  fun removeAction(index: Int) {
    val mapping = list.removeAt(reverseIndex(index))
    sharedPreferences
      .edit()
      .remove(mapping.sharedPreferencesKey.toString())
      .apply()
    map.remove(mapping.key)
  }

  fun removeActionByKey(key: Int): Int {
    // Remove the mapping from the list.
    var mapping: Mapping? = null
    var index = -1
    for (i in list.indices) {
      val m = list[i]
      if (m.key == key) {
        list.removeAt(i)
        mapping = m
        index = i
        break
      }
    }
    sharedPreferences
      .edit()
      .remove(mapping!!.sharedPreferencesKey.toString())
      .apply()
    map.remove(key)
    return reverseIndex(index)
  }

  private fun encodeKeyCodeAndActionValue(keyCode: Int, action: Action): String {
    val builder = StringBuilder()
      .append(keyCode)
      .append('\n')
    if (action.componentName != null) {
      builder
        .append(0)
        .append('\n')
        .append(action.componentName.packageName)
        .append('\n')
        .append(action.componentName.className)
    } else if (action.openNotifications) {
      builder
        .append(1)
    } else if (action.search != null) {
      builder
        .append(2)
        .append('\n')
        .append(action.search)
    } else {
      throw IllegalStateException("Unimplemented action type: $action")
    }
    return builder.toString()
  }
}
