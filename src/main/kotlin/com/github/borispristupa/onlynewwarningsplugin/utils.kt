package com.github.borispristupa.onlynewwarningsplugin

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import kotlin.reflect.KProperty

fun <K, V> Map<K?, V>.filterKeysNotNull(): Map<K, V> =
  entries.fold(mutableMapOf()) { map, (key, value) ->
    if (key != null) {
      map[key] = value
    }
    map
  }

val VirtualFile.document: Document?
  get() = FileDocumentManager.getInstance().getDocument(this)


class BooleanProperty(id: String, private val defaultValue: Boolean) {
  private val id = "com.github.borispristupa.onlynewwarningsplugin.settings.$id"

  operator fun getValue(thisRef: Any, property: KProperty<*>): Boolean =
    PropertiesComponent.getInstance().getBoolean(id, defaultValue)

  operator fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
    PropertiesComponent.getInstance().setValue(id, value, defaultValue)
  }
}