package com.github.borispristupa.onlynewwarningsplugin.analysis

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.fragments.DiffFragment
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.util.text.CharSequenceSubSequence
import java.util.Comparator

class RangeFixer(oldDocument: Document, currentDocument: Document) {
  private val diffs: List<DiffFragment> = ComparisonManager.getInstance().compareWords(
    CharSequenceSubSequence(oldDocument.charsSequence),
    CharSequenceSubSequence(currentDocument.charsSequence),
    ComparisonPolicy.DEFAULT,
    DumbProgressIndicator.INSTANCE
  ).sortedWith(Comparator.comparing(DiffFragment::getStartOffset1).thenComparing(DiffFragment::getStartOffset2))

  /**
   * If for some range `startOffset1 == endOffset1 == index`,
   * [stickLeft] defines whether the resulting offset should be `startOffset2` or `endOffset2`
   */
  private fun fixOffset(index: Int, stickLeft: Boolean): Int {
    val pos = diffs.binarySearch { diff ->
      when {
        stickLeft && diff.startOffset1 == index -> 0
        diff.endOffset1 <= index -> -1
        diff.startOffset1 > index -> 1
        else -> 0
      }
    }

    return when {
      pos >= 0 -> minOf(
        diffs[pos].endOffset2,
        diffs[pos].startOffset2 + (index - diffs[pos].startOffset1)
      )

      pos == -1 -> index

      else -> {
        val realPos = -(pos + 1)
        diffs[realPos - 1].endOffset2 + (index - diffs[realPos - 1].endOffset1)
      }
    }
  }

  fun fixRange(textRange: TextRange) = TextRange.create(
    fixOffset(textRange.startOffset, false),
    fixOffset(textRange.endOffset, true)
  )
}