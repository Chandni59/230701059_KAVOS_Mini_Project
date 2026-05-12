package com.chandni.kavos

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class LegalRightAdapter(
    private var rights: List<LegalRight>,
    private val onClick: (LegalRight) -> Unit
) : RecyclerView.Adapter<LegalRightAdapter.RightViewHolder>() {

    class RightViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardRight)
        val gradientLayer: View = view.findViewById(R.id.gradientLayer)
        val icon: ImageView = view.findViewById(R.id.ivRightIcon)
        val title: TextView = view.findViewById(R.id.tvRightTitle)
        val category: TextView = view.findViewById(R.id.tvRightCategory)
        val shortLine: TextView = view.findViewById(R.id.tvRightShort)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_legal_right, parent, false)
        return RightViewHolder(view)
    }

    override fun onBindViewHolder(holder: RightViewHolder, position: Int) {
        val right = rights[position]
        val ctx = holder.itemView.context

        val start = ctx.getColor(right.gradientStart)
        val end = ctx.getColor(right.gradientEnd)

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(start, end)
        )
        gradient.cornerRadius = dp(ctx, 18f)
        holder.gradientLayer.background = gradient

        holder.icon.setImageResource(right.iconRes)
        holder.title.text = right.title
        holder.category.text = right.category.uppercase()
        holder.shortLine.text = right.shortLine

        holder.card.setOnClickListener { onClick(right) }
    }

    override fun getItemCount(): Int = rights.size

    fun submit(newList: List<LegalRight>) {
        rights = newList
        notifyDataSetChanged()
    }

    private fun dp(ctx: android.content.Context, value: Float): Float =
        value * ctx.resources.displayMetrics.density
}
