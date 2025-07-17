package com.example.promptpilot.helpers

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

class UrlLauncher {

    fun openUrl(context: Context, url: String) {
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            url.toUri()
        )
        context.startActivity(urlIntent)
    }
}