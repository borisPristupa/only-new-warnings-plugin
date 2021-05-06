package com.github.borispristupa.onlynewwarningsplugin

import com.intellij.ide.util.PropertiesComponent
import kotlin.reflect.KProperty

class BooleanProperty(id: String, private val defaultValue: Boolean) {
  private val id = "com.github.borispristupa.onlynewwarningsplugin.settings.$id"

  operator fun getValue(thisRef: Any, property: KProperty<*>): Boolean =
    PropertiesComponent.getInstance().getBoolean(id, defaultValue)

  operator fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
    PropertiesComponent.getInstance().setValue(id, value, defaultValue)
  }
}