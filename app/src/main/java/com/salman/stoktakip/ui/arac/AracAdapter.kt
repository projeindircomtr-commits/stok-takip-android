package com.salman.stoktakip.ui.arac

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.salman.stoktakip.BuildConfig
import com.salman.stoktakip.data.local.entity.AracCacheEntity
import com.salman.stoktakip.databinding.ItemAracBinding

class AracAdapter(
    private val onDuzenle: (AracCacheEntity) -> Unit,
    private val onSil: (AracCacheEntity) -> Unit
) : ListAdapter<AracCacheEntity, AracAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAracBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemAracBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AracCacheEntity) {
            binding.txtAd.text = "${item.aracIsmi} ${if (!item.plaka.isNullOrBlank()) "(${item.plaka})" else ""}"
            val ozellikler = mutableListOf<String>()
            if (item.kamera == "Var") ozellikler.add("Kamera")
            if (item.gps == "Var") ozellikler.add("GPS")
            val ozellikMetni = if (ozellikler.isEmpty()) "" else " • " + ozellikler.joinToString(", ")
            binding.txtDetay.text = "${item.kategoriAdi ?: "-"} • ${item.lokasyonAdi ?: "-"}$ozellikMetni"

            binding.txtDurum.visibility = if (item.syncStatus == "offline_bekliyor") View.VISIBLE else View.GONE

            if (!item.resim.isNullOrBlank()) {
                val url = BuildConfig.BASE_URL.removeSuffix("mobile-api/") + item.resim
                Glide.with(binding.imgResim).load(url).centerCrop().into(binding.imgResim)
            } else {
                Glide.with(binding.imgResim).clear(binding.imgResim)
                binding.imgResim.setImageDrawable(null)
            }

            binding.btnDuzenle.setOnClickListener { onDuzenle(item) }
            binding.btnSil.setOnClickListener { onSil(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AracCacheEntity>() {
            override fun areItemsTheSame(a: AracCacheEntity, b: AracCacheEntity) = a.id == b.id
            override fun areContentsTheSame(a: AracCacheEntity, b: AracCacheEntity) = a == b
        }
    }
}
