package io.github.abeleiras.aura.ui.recordings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.abeleiras.aura.R
import io.github.abeleiras.aura.data.Recording

/** Opens the system share sheet with the recording's audio (FR-004-05, audio only for now). */
fun shareRecording(context: Context, recording: Recording) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", recording.file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("audio/ogg")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser_title)))
}
