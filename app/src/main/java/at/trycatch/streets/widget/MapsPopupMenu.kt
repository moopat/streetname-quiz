package at.trycatch.streets.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.PopupMenu
import at.trycatch.streets.R
import at.trycatch.streets.activity.LegalActivity


/**
 * @author Markus Deutsch <markus@moop.at>
 */
class MapsPopupMenu(private val context: Context, val view: View) {

    private val popupMenu = PopupMenu(context, view)

    init {
        popupMenu.inflate(R.menu.menu_main)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_legal -> context.startActivity(Intent(context, LegalActivity::class.java))
                R.id.menu_feedback -> {
                    AlertDialog.Builder(context)
                            .setTitle(R.string.feedback_dialog_title)
                            .setMessage(R.string.feedback_dialog_contents)
                            .setNegativeButton(R.string.all_cancel, null)
                            .setPositiveButton(R.string.all_ok) { _, _ ->
                                startFeedbackIntent()
                            }
                            .show()
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    fun show() = popupMenu.show()

    private fun startFeedbackIntent() {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "support@moop.at", null))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Streets Feedback")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@moop.at"))
        context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.feedback_dialog_title)))
    }

}
