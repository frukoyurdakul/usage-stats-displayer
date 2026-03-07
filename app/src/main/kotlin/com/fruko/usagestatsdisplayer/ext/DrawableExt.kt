package com.fruko.usagestatsdisplayer.ext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

fun Drawable.asBitmap(): Bitmap {
    if (this is BitmapDrawable) {
        return bitmap
    }
    val bitmap = createBitmap(intrinsicWidth, intrinsicHeight)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}