package com.example.prizoscope.ui.chat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R

class AdminAdapter(
    private val admins: List<String>,
    private val onAdminClick: (String) -> Unit
) : RecyclerView.Adapter<AdminAdapter.AdminViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val adminName = admins[position]
        holder.adminName.text = adminName
        holder.rootLayout.setOnClickListener { onAdminClick(adminName) }
    }

    override fun getItemCount(): Int = admins.size

    class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val adminName: TextView = itemView.findViewById(R.id.admin_name)
        val rootLayout: View = itemView.findViewById(R.id.root_layout)
    }
}

