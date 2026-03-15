package com.example.galleryoverlan.core.logging

import android.util.Log

object AppLogger {
    private const val TAG = "GalleryOverLan"

    fun d(message: String, tag: String = TAG) {
        Log.d(tag, message)
    }

    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }

    fun w(message: String, throwable: Throwable? = null, tag: String = TAG) {
        Log.w(tag, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        Log.e(tag, message, throwable)
    }
}
