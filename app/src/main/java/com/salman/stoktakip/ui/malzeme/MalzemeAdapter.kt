package com.salman.stoktakip.ui.malzeme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.salman.stoktakip.BuildConfig
import com.salman.stoktakip.data.local.entity.MalzemeCacheEntity
import com.salman.stoktakip.databinding.ItemMalzemeBinding

class MalzemeAdapter(
    private val onDuzenle: (MalzemeCacheEntity) -> Unit,
    private val onSil: (MalzemeCacheEntity) -> Unit
) : ListAdapter<MalzemeCacheEntity, MalzemeAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMalzemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemMalzemeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MalzemeCacheEntity) {
            binding.txtAd.text = item.ad
            val miktar = if (item.miktarDegeri % 1.0 == 0.0) item.miktarDegeri.toInt().toString()
            else item.miktarDegeri.toString()
            val birim = item.miktarBirimi.ifBlank { "adet" }
            val kategori = item.kategoriAdi ?: "-"
            val lokasyon = item.lokasyonAdi ?: "-"
            binding.txtDetay.text = "$miktar $birim  •  $kategori  •  $lokasyon"

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
        private val DIFF = object : DiffUtil.ItemCallback<MalzemeCacheEntity>() {
            override fun areItemsTheSame(a: MalzemeCacheEntity, b: MalzemeCacheEntity) = a.id == b.id
            override fun areContentsTheSame(a: MalzemeCacheEntity, b: MalzemeCacheEntity) = a == b
        }
    }
}
