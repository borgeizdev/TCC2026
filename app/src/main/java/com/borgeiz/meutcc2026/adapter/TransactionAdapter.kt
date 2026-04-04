package com.borgeiz.meutcc2026.adapter

import android.content.Intent
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
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvInfo: TextView = itemView.findViewById(R.id.tvInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = list[position]
        holder.tvTitle.text = item.title
        holder.tvInfo.text = "${item.type} | ${item.category} | R$ %.2f".format(item.amount)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditTransactionActivity::class.java)
            intent.putExtra("id", item.id)
            intent.putExtra("type", item.type)
            intent.putExtra("title", item.title)
            intent.putExtra("amount", item.amount)
            intent.putExtra("category", item.category)
            intent.putExtra("date", item.date)
            intent.putExtra("description", item.description)
            holder.itemView.context.startActivity(intent)
        }
    }
}
