package com.construction.diary.cloud.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.construction.diary.cloud.R

object DialogUtils {
    fun showRounded(builder: AlertDialog.Builder): AlertDialog {
        val dialog = builder.create()
        applyRoundedDialogStyle(dialog)
        dialog.show()
        return dialog
    }

    fun showRoundedCompat(builder: AlertDialog.Builder): AlertDialog {
        val dialog = builder.create()
        applyRoundedDialogStyle(dialog)
        dialog.show()
        return dialog
    }

    fun applyRoundedDialogStyle(dialog: Dialog) {
        dialog.window?.let { window ->
            val ctx = window.context
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setOnShowListener { applyDialogContentViewStyle(window.decorView, ctx) }
            if (dialog.isShowing) applyDialogContentViewStyle(window.decorView, ctx)
        }
    }

    private fun applyDialogContentViewStyle(decorView: View, ctx: Context) {
        val density = ctx.resources.displayMetrics.density
        val bgColor = ContextCompat.getColor(ctx, R.color.mi_card)
        val dialogBg = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 16f * density
        }
        val contentView = (decorView as? ViewGroup)?.getChildAt(0)
        contentView?.let {
            it.background = dialogBg
            it.clipToOutline = true
            it.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 16f * density)
                }
            }
        }
    }
}
