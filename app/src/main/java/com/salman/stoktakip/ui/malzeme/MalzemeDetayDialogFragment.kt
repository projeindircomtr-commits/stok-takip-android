package com.salman.stoktakip.ui.malzeme

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.salman.stoktakip.BuildConfig
import com.salman.stoktakip.data.local.entity.MalzemeCacheEntity
import com.salman.stoktakip.databinding.DialogMalzemeDetayBinding

class MalzemeDetayDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogMalzemeDetayBinding.inflate(layoutInflater)

        val ad = arguments?.getString(ARG_AD).orEmpty()
        val miktar = arguments?.getDouble(ARG_MIKTAR) ?: 0.0
        val birim = arguments?.getString(ARG_BIRIM).orEmpty()
        val kategori = arguments?.getString(ARG_KATEGORI)
        val lokasyon = arguments?.getString(ARG_LOKASYON)
        val tarih = arguments?.getString(ARG_TARIH)
        val resim = arguments?.getString(ARG_RESIM)
        val bekliyor = arguments?.getBoolean(ARG_BEKLIYOR, false) ?: false

        binding.txtAd.text = ad
        val miktarMetin = if (miktar % 1.0 == 0.0) miktar.toInt().toString() else miktar.toString()
        binding.txtMiktar.text = "$miktarMetin $birim"
        binding.txtKategori.text = "Kategori: ${kategori ?: "-"}"
        binding.txtLokasyon.text = "Lokasyon: ${lokasyon ?: "-"}"
        binding.txtTarih.text = tarih?.let { "Eklenme: ${com.salman.stoktakip.util.tarihiBicimlendir(it)}" } ?: ""
        binding.txtDurum.visibility = if (bekliyor) View.VISIBLE else View.GONE

        if (!resim.isNullOrBlank()) {
            val url = BuildConfig.BASE_URL.removeSuffix("mobile-api/") + resim
            Glide.with(binding.imgBuyuk).load(url).centerCrop().into(binding.imgBuyuk)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Kapat", null)
            .create()
    }

    companion object {
        private const val ARG_AD = "ad"
        private const val ARG_MIKTAR = "miktar"
        private const val ARG_BIRIM = "birim"
        private const val ARG_KATEGORI = "kategori"
        private const val ARG_LOKASYON = "lokasyon"
        private const val ARG_TARIH = "tarih"
        private const val ARG_RESIM = "resim"
        private const val ARG_BEKLIYOR = "bekliyor"

        fun yeni(kayit: MalzemeCacheEntity): MalzemeDetayDialogFragment =
            MalzemeDetayDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_AD, kayit.ad)
                    putDouble(ARG_MIKTAR, kayit.miktarDegeri)
                    putString(ARG_BIRIM, kayit.miktarBirimi)
                    putString(ARG_KATEGORI, kayit.kategoriAdi)
                    putString(ARG_LOKASYON, kayit.lokasyonAdi)
                    putString(ARG_TARIH, kayit.olusturmaTarihi)
                    putString(ARG_RESIM, kayit.resim)
                    putBoolean(ARG_BEKLIYOR, kayit.syncStatus == "offline_bekliyor")
                }
            }
    }
}
