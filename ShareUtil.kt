package org.darulhuda.udupi.util

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun shareText(text: String) {
    val ctx = LocalContext.current
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, "Share via"))
}
