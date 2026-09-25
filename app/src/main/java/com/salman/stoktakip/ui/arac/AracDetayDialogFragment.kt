package com.salman.stoktakip.ui.arac

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.salman.stoktakip.BuildConfig
import com.salman.stoktakip.data.local.entity.AracCacheEntity
import com.salman.stoktakip.databinding.DialogAracDetayBinding

class AracDetayDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAracDetayBinding.inflate(layoutInflater)

        val isim = arguments?.getString(ARG_ISIM).orEmpty()
        val model = arguments?.getString(ARG_MODEL)
        val plaka = arguments?.getString(ARG_PLAKA)
        val kamera = arguments?.getString(ARG_KAMERA)
        val gps = arguments?.getString(ARG_GPS)
        val sahip = arguments?.getString(ARG_SAHIP)
        val telefon = arguments?.getString(ARG_TELEFON)
        val kategori = arguments?.getString(ARG_KATEGORI)
        val lokasyon = arguments?.getString(ARG_LOKASYON)
        val resim = arguments?.getString(ARG_RESIM)
        val tarih = arguments?.getString(ARG_TARIH)
        val bekliyor = arguments?.getBoolean(ARG_BEKLIYOR, false) ?: false

        binding.txtAd.text = if (!model.isNullOrBlank()) "$isim ($model)" else isim
        binding.txtPlaka.text = "Plaka: ${plaka ?: "-"}"

        val ozellikler = mutableListOf<String>()
        if (kamera == "Var") ozellikler.add("Kamera Var")
        if (gps == "Var") ozellikler.add("GPS Var")
        binding.txtOzellikler.text = if (ozellikler.isEmpty()) "Kamera/GPS bilgisi yok" else ozellikler.joinToString(" • ")

        binding.txtSahip.text = "Sahip: ${sahip ?: "-"}${if (!telefon.isNullOrBlank()) " • Tel: $telefon" else ""}"
        binding.txtKategoriLokasyon.text = "${kategori ?: "-"} • ${lokasyon ?: "-"}"
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
        private const val ARG_ISIM = "isim"
        private const val ARG_MODEL = "model"
        private const val ARG_PLAKA = "plaka"
        private const val ARG_KAMERA = "kamera"
        private const val ARG_GPS = "gps"
        private const val ARG_SAHIP = "sahip"
        private const val ARG_TELEFON = "telefon"
        private const val ARG_KATEGORI = "kategori"
        private const val ARG_LOKASYON = "lokasyon"
        private const val ARG_RESIM = "resim"
        private const val ARG_TARIH = "tarih"
        private const val ARG_BEKLIYOR = "bekliyor"

        fun yeni(kayit: AracCacheEntity): AracDetayDialogFragment =
            AracDetayDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ISIM, kayit.aracIsmi)
                    putString(ARG_MODEL, kayit.model)
                    putString(ARG_PLAKA, kayit.plaka)
                    putString(ARG_KAMERA, kayit.kamera)
                    putString(ARG_GPS, kayit.gps)
                    putString(ARG_SAHIP, kayit.sahip)
                    putString(ARG_TELEFON, kayit.telefon)
                    putString(ARG_KATEGORI, kayit.kategoriAdi)
                    putString(ARG_LOKASYON, kayit.lokasyonAdi)
                    putString(ARG_RESIM, kayit.resim)
                    putString(ARG_TARIH, kayit.olusturmaTarihi)
                    putBoolean(ARG_BEKLIYOR, kayit.syncStatus == "offline_bekliyor")
                }
            }
    }
}
