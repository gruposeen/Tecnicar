package com.example.tecnicar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TecnicoAdapter(
    private val tecnicos: List<Tecnico>,
    private val onItemClick: (Tecnico) -> Unit
) : RecyclerView.Adapter<TecnicoAdapter.TecnicoViewHolder>() {

    class TecnicoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcono: ImageView = itemView.findViewById(R.id.ivTecnicoTipo)
        val tvNombre: TextView = itemView.findViewById(R.id.tvTecnicoNombre)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTecnicoTipo)
        val tvDistancia: TextView = itemView.findViewById(R.id.tvTecnicoDistancia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TecnicoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tecnico, parent, false)
        return TecnicoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TecnicoViewHolder, position: Int) {
        val tecnico = tecnicos[position]

        // Configurar icono según el tipo de técnico
        val iconoResourceId = holder.itemView.context.resources.getIdentifier(
            tecnico.tipo.lowercase(), "drawable", holder.itemView.context.packageName
        )

        if (iconoResourceId != 0) {
            holder.ivIcono.setImageResource(iconoResourceId)
        } else {
            // Icono por defecto si no se encuentra uno específico

        }

        // Configurar información del técnico
        holder.tvNombre.text = tecnico.nombre
        holder.tvTipo.text = tecnico.tipo
        holder.tvDistancia.text = "A 2.5 km"

        // Configurar clic en el elemento
        holder.itemView.setOnClickListener {
            onItemClick(tecnico)
        }
    }

    override fun getItemCount(): Int = tecnicos.size
}