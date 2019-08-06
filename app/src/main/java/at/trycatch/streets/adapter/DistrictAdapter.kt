package at.trycatch.streets.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import at.trycatch.streets.R
import at.trycatch.streets.adapter.vh.DistrictViewHolder
import at.trycatch.streets.model.CityWithProgress
import at.trycatch.streets.model.DistrictWithProgress

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class DistrictAdapter(private val callback: (districtId: String?) -> Unit) : RecyclerView.Adapter<DistrictViewHolder>() {

    private val data = mutableListOf<Any>()

    private val districts = mutableListOf<DistrictWithProgress>()
    private var city: CityWithProgress? = null

    fun setDistricts(newDistricts: List<DistrictWithProgress>) {
        districts.clear()
        districts.addAll(newDistricts)
        applyChanges()
    }

    fun setCity(newCity: CityWithProgress?) {
        city = newCity
        applyChanges()
    }

    private fun applyChanges() {
        data.clear()
        city?.let { data.add(it) }
        data.addAll(districts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DistrictViewHolder {
        return DistrictViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_district, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: DistrictViewHolder, position: Int) {
        val district = data[position]
        if (district is CityWithProgress) {
            bindCity(holder, district)
        } else if (district is DistrictWithProgress) {
            bindDistrict(holder, district)
        }
    }

    private fun bindCity(holder: DistrictViewHolder, city: CityWithProgress) {
        holder.tvTitle.text = holder.itemView.context.getString(R.string.districts_city_title, city.city.displayName)
        holder.tvTitle.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_inline_city), null, null, null)

        holder.progress.progress = city.solvedStreets
        holder.progress.max = city.totalStreets

        holder.itemView.setOnClickListener { callback.invoke(null) }
    }

    private fun bindDistrict(holder: DistrictViewHolder, district: DistrictWithProgress) {
        holder.tvTitle.text = district.district.displayName
        holder.tvTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

        holder.progress.progress = district.solvedStreets
        holder.progress.max = district.totalStreets

        holder.itemView.setOnClickListener { callback.invoke(district.district.id) }
    }

}
