package at.trycatch.streets.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.PopupMenu
import at.trycatch.streets.R
import at.trycatch.streets.activity.LegalActivity

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class MapsPopupMenu(val context: Context, val view: View) {

    private val popupMenu = PopupMenu(context, view)

    init {
        popupMenu.inflate(R.menu.menu_main)
        popupMenu.setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_legal) context.startActivity(Intent(context, LegalActivity::class.java))
            return@setOnMenuItemClickListener true
        }
    }

    fun show() = popupMenu.show()

    fun dismiss() = popupMenu.dismiss()

}