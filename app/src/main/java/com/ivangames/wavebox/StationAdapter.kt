package com.ivangames.wavebox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Station(val name: String, val desc: String, val url: String)

class StationAdapter(
    private val stations: List<Station>,
    private val onClick: (Station) -> Unit
) : RecyclerView.Adapter<StationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.stationName)
        val desc: TextView = view.findViewById(R.id.stationDesc)
        val icon: TextView = view.findViewById(R.id.stationIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = stations[position]
        holder.name.text = station.name
        holder.desc.text = station.desc
        holder.itemView.setOnClickListener { onClick(station) }
    }

    override fun getItemCount() = stations.size
}
