package com.chandni.kavos

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class SafetyTipAdapter(
    private val tips: List<SafetyTip>,
    private val onClick: (SafetyTip) -> Unit
) : RecyclerView.Adapter<SafetyTipAdapter.TipViewHolder>() {

    class TipViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardTip)
        val iconBg: View = view.findViewById(R.id.iconBg)
        val icon: ImageView = view.findViewById(R.id.ivTipIcon)
        val tagline: TextView = view.findViewById(R.id.tvTipTagline)
        val title: TextView = view.findViewById(R.id.tvTipTitle)
        val summary: TextView = view.findViewById(R.id.tvTipSummary)
        val accentBar: View = view.findViewById(R.id.accentBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_safety_tip, parent, false)
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        val tip = tips[position]
        val ctx = holder.itemView.context
        val accent = ctx.getColor(tip.accentColor)

        holder.tagline.text = tip.tagline.uppercase()
        holder.tagline.setTextColor(accent)
        holder.title.text = tip.title
        holder.summary.text = tip.summary
        holder.icon.setImageResource(tip.iconRes)
        holder.icon.imageTintList = ColorStateList.valueOf(accent)
        val tintedBg = (accent and 0x00FFFFFF) or 0x22000000
        holder.iconBg.backgroundTintList = ColorStateList.valueOf(tintedBg)
        holder.accentBar.setBackgroundColor(accent)

        holder.card.setOnClickListener { onClick(tip) }
    }

    override fun getItemCount(): Int = tips.size
}
