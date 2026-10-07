package io.github.abeleiras.aura.crash

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.abeleiras.aura.R

/**
 * Error screen in its own process (see the manifest). Plain Views on purpose: it must work
 * even when the main process failed while starting (FR-007-02).
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = intent.getStringExtra(EXTRA_REPORT).orEmpty()
        val pad = (16 * resources.displayMetrics.density).toInt()

        fun button(label: Int, onClick: () -> Unit) = Button(this).apply {
            setText(label)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(this).apply {
            setText(R.string.crash_title)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        }
        val body = TextView(this).apply {
            setText(R.string.crash_body)
            setPadding(0, pad / 2, 0, pad / 2)
        }
        val trace = TextView(this).apply {
            text = report
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            addView(trace)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(button(R.string.crash_copy) { copy(report) })
            addView(button(R.string.crash_share) { share(report) })
            addView(button(R.string.crash_close) { finishAndRemoveTask() })
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(title)
            addView(body)
            addView(scroll)
            addView(buttons)
        }
        // Edge-to-edge is enforced from targetSdk 35: keep the content clear of the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom)
            insets
        }
        setContentView(root)
    }

    private fun copy(report: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Aura error report", report))
        Toast.makeText(this, R.string.crash_copied, Toast.LENGTH_SHORT).show()
    }

    private fun share(report: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report)
        startActivity(Intent.createChooser(send, getString(R.string.crash_share)))
    }

    companion object {
        const val EXTRA_REPORT = "report"
    }
}
