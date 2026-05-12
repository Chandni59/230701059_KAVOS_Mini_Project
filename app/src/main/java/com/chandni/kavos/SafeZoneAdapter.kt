package com.chandni.kavos

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class SafeZoneAdapter(
    private var zones: List<Pair<SafeZone, Float?>>,
    private val onNavigate: (SafeZone) -> Unit,
    private val onDelete: (SafeZone) -> Unit
) : RecyclerView.Adapter<SafeZoneAdapter.ZoneViewHolder>() {

    class ZoneViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardZone)
        val iconBg: View = view.findViewById(R.id.zoneIconBg)
        val icon: ImageView = view.findViewById(R.id.ivZoneIcon)
        val name: TextView = view.findViewById(R.id.tvZoneName)
        val address: TextView = view.findViewById(R.id.tvZoneAddress)
        val distance: TextView = view.findViewById(R.id.tvZoneDistance)
        val typeChip: TextView = view.findViewById(R.id.tvZoneType)
        val btnDelete: ImageButton = view.findViewById(R.id.btnZoneDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_safe_zone, parent, false)
        return ZoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        val (zone, distMeters) = zones[position]
        val ctx = holder.itemView.context
        val (iconRes, color) = SafeZoneTypes.iconAndColor(zone.type)

        holder.icon.setImageResource(iconRes)
        val iconBgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ctx.getColor(color))
        }
        holder.iconBg.background = iconBgDrawable

        holder.name.text = zone.name
        holder.address.text = zone.address
        holder.typeChip.text = zone.type.uppercase()
        holder.typeChip.setTextColor(ctx.getColor(color))
        holder.typeChip.backgroundTintList =
            ColorStateList.valueOf(ctx.getColor(color) and 0x22FFFFFF or 0x22000000)

        holder.distance.text = formatDistance(distMeters)

        holder.card.setOnClickListener { onNavigate(zone) }
        holder.btnDelete.setOnClickListener { onDelete(zone) }
    }

    override fun getItemCount(): Int = zones.size

    fun submit(newList: List<Pair<SafeZone, Float?>>) {
        zones = newList
        notifyDataSetChanged()
    }

    private fun formatDistance(meters: Float?): String {
        if (meters == null) return "Tap to navigate"
        return when {
            meters < 1000 -> "${meters.toInt()} m away"
            else -> String.format("%.1f km away", meters / 1000f)
        }
    }
}

object SafeZoneTypes {
    val ALL = listOf("Home", "College", "Friend", "Police", "Hospital", "Other")

    fun iconAndColor(type: String): Pair<Int, Int> = when (type) {
        "Home" -> R.drawable.ic_zone_home to R.color.tip_purple
        "College" -> R.drawable.ic_zone_school to R.color.tip_blue
        "Friend" -> R.drawable.ic_zone_friend to R.color.tip_orange
        "Police" -> R.drawable.ic_zone_police to R.color.purple_primary
        "Hospital" -> R.drawable.ic_zone_hospital to R.color.tip_red
        else -> R.drawable.ic_zone_other to R.color.tip_green
    }
}
