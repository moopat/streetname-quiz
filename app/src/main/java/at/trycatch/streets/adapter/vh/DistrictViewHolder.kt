package at.trycatch.streets.adapter.vh

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import at.trycatch.streets.R

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class DistrictViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val cardView: CardView = itemView as CardView
    val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    val progress: ProgressBar = itemView.findViewById(R.id.progress)

}
