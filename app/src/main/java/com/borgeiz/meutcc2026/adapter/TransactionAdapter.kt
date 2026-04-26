package com.borgeiz.meutcc2026.adapter

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.borgeiz.meutcc2026.EditTransactionActivity
import com.borgeiz.meutcc2026.R
import com.borgeiz.meutcc2026.model.Transaction

class TransactionAdapter(
    private val list: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle:  TextView = itemView.findViewById(R.id.tvTitle)
        val tvInfo:   TextView = itemView.findViewById(R.id.tvInfo)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvIcon:   TextView = itemView.findViewById(R.id.tvIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = list[position]

        holder.tvTitle.text  = item.title
        holder.tvInfo.text   = "${item.category}  ·  ${item.date}"
        holder.tvAmount.text = "R$ %.2f".format(item.amount)

        // Cor e ícone por tipo
        if (item.type == "receita") {
            holder.tvAmount.setTextColor(Color.parseColor("#10B981"))
            holder.tvIcon.text = "💰"
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#EF4444"))
            holder.tvIcon.text = "💸"
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditTransactionActivity::class.java).apply {
                putExtra("id",          item.id)
                putExtra("type",        item.type)
                putExtra("title",       item.title)
                putExtra("amount",      item.amount)
                putExtra("category",    item.category)
                putExtra("date",        item.date)
                putExtra("description", item.description)
            }
            holder.itemView.context.startActivity(intent)
        }
    }
}