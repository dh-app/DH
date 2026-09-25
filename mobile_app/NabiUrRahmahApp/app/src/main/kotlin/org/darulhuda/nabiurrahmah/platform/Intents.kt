package org.darulhuda.nabiurrahmah.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import org.darulhuda.nabiurrahmah.R

/** Play Store listing of the released app (debug builds use a suffixed id). */
const val PLAY_STORE_APP_ID = "org.darulhuda.udupi"
const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$PLAY_STORE_APP_ID"

private fun Context.tryStart(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

/** Starts [intent], telling the person if no installed app can handle it. */
fun Context.startOrToast(intent: Intent): Boolean =
    tryStart(intent).also { started ->
        if (!started) Toast.makeText(this, R.string.error_no_app, Toast.LENGTH_SHORT).show()
    }

fun Context.openUrl(url: String) = startOrToast(Intent(Intent.ACTION_VIEW, url.toUri()))

fun Context.dial(phone: String) =
    startOrToast(Intent(Intent.ACTION_DIAL, "tel:${phone.filter { it.isDigit() || it == '+' }}".toUri()))

fun Context.openWhatsApp(phone: String) = openUrl("https://wa.me/${phone.filter(Char::isDigit)}")

fun Context.sendEmail(address: String, subject: String) =
    startOrToast(
        Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            .putExtra(Intent.EXTRA_SUBJECT, subject),
    )

fun Context.openMap(address: String, mapUrl: String?) =
    openUrl(mapUrl ?: "https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}")

fun Context.openPlayStore() {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=$PLAY_STORE_APP_ID".toUri())
    if (!tryStart(market)) openUrl(PLAY_STORE_URL)
}

fun Context.shareText(text: String, chooserTitle: String) =
    startOrToast(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
            chooserTitle,
        ),
    )

fun Context.shareFile(file: LocalFile, text: String, chooserTitle: String) {
    // The clip data lets the share sheet show a preview of the file.
    startOrToast(Intent.createChooser(sendIntent(file, text), chooserTitle))
}

private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

private fun sendIntent(file: LocalFile, text: String): Intent =
    Intent(Intent.ACTION_SEND)
        .setType(file.mimeType)
        .putExtra(Intent.EXTRA_STREAM, file.uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .also { it.clipData = ClipData.newRawUri(null, file.uri) }

/** Straight into WhatsApp (or WhatsApp Business); the share sheet if neither is installed. */
fun Context.shareFileToWhatsApp(file: LocalFile, text: String, chooserTitle: String) {
    if (WHATSAPP_PACKAGES.none { tryStart(sendIntent(file, text).setPackage(it)) }) shareFile(file, text, chooserTitle)
}

fun Context.shareTextToWhatsApp(text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    if (WHATSAPP_PACKAGES.none { tryStart(Intent(send).setPackage(it)) }) shareText(text, chooserTitle)
}

fun Context.viewFile(file: LocalFile) =
    startOrToast(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(file.uri, file.mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )
