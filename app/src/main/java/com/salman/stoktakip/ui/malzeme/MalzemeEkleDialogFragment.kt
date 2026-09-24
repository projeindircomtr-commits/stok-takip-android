package com.salman.stoktakip.ui.malzeme

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.salman.stoktakip.data.local.entity.MalzemeCacheEntity
import com.salman.stoktakip.databinding.DialogMalzemeEkleBinding
import com.salman.stoktakip.util.DropdownOgesi
import com.salman.stoktakip.util.bitmapDosyasiniBase64eCevir

class MalzemeEkleDialogFragment : DialogFragment() {

    private var _binding: DialogMalzemeEkleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MalzemelerViewModel by activityViewModels()
    private var secilenResimUri: Uri? = null
    private var duzenlenenId: Int? = null

    private val galeriSec = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            secilenResimUri = uri
            Glide.with(this).load(uri).centerCrop().into(binding.imgOnizleme)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogMalzemeEkleBinding.inflate(layoutInflater)
        duzenlenenId = arguments?.getInt(ARG_ID)?.takeIf { it != 0 }

        binding.dropdownBirim.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("adet", "kg", "litre"))
        )

        val kategoriListesi = (arguments?.getSerializable(ARG_KATEGORILER) as? ArrayList<DropdownOgesi>).orEmpty()
        val lokasyonListesi = (arguments?.getSerializable(ARG_LOKASYONLAR) as? ArrayList<DropdownOgesi>).orEmpty()

        binding.dropdownKategori.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, kategoriListesi)
        )
        binding.dropdownLokasyon.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, lokasyonListesi)
        )

        // duzenleme modu ise mevcut degerleri doldur
        arguments?.let { args ->
            if (duzenlenenId != null) {
                binding.editAd.setText(args.getString(ARG_AD))
                binding.editMiktar.setText(args.getDouble(ARG_MIKTAR).let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                })
                binding.dropdownBirim.setText(args.getString(ARG_BIRIM, "adet"), false)
                kategoriListesi.find { it.id == args.getInt(ARG_KATEGORI_ID) }?.let {
                    binding.dropdownKategori.setText(it.ad, false)
                }
                lokasyonListesi.find { it.id == args.getInt(ARG_LOKASYON_ID) }?.let {
                    binding.dropdownLokasyon.setText(it.ad, false)
                }
            }
        }

        binding.btnResimSec.setOnClickListener { galeriSec.launch("image/*") }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (duzenlenenId != null) "Malzeme Güncelle" else "Yeni Malzeme")
            .setView(binding.root)
            .setPositiveButton(if (duzenlenenId != null) "Güncelle" else "Ekle", null)
            .setNegativeButton("Vazgeç", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                        kaydet(kategoriListesi, lokasyonListesi)
                    }
                }
            }
    }

    private fun kaydet(kategoriler: List<DropdownOgesi>, lokasyonlar: List<DropdownOgesi>) {
        val ad = binding.editAd.text?.toString()?.trim().orEmpty()
        if (ad.isEmpty()) {
            binding.editAd.error = "Zorunlu alan"
            return
        }
        val miktar = binding.editMiktar.text?.toString()?.toDoubleOrNull() ?: 0.0
        val birim = binding.dropdownBirim.text?.toString()?.ifBlank { "adet" } ?: "adet"
        val kategoriId = kategoriler.find { it.ad == binding.dropdownKategori.text?.toString() }?.id
        val lokasyonId = lokasyonlar.find { it.ad == binding.dropdownLokasyon.text?.toString() }?.id
        val resim = secilenResimUri?.let { bitmapDosyasiniBase64eCevir(requireContext(), it) }

        val id = duzenlenenId
        if (id != null) {
            viewModel.guncelle(id, ad, miktar, birim, kategoriId, lokasyonId, resim)
        } else {
            viewModel.ekle(ad, miktar, birim, kategoriId, lokasyonId, resim)
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_AD = "ad"
        private const val ARG_MIKTAR = "miktar"
        private const val ARG_BIRIM = "birim"
        private const val ARG_KATEGORI_ID = "kategori_id"
        private const val ARG_LOKASYON_ID = "lokasyon_id"
        private const val ARG_KATEGORILER = "kategoriler"
        private const val ARG_LOKASYONLAR = "lokasyonlar"

        fun yeni(kategoriler: List<DropdownOgesi>, lokasyonlar: List<DropdownOgesi>): MalzemeEkleDialogFragment {
            return MalzemeEkleDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_KATEGORILER, ArrayList(kategoriler))
                    putSerializable(ARG_LOKASYONLAR, ArrayList(lokasyonlar))
                }
            }
        }

        fun duzenle(
            kayit: MalzemeCacheEntity,
            kategoriler: List<DropdownOgesi>,
            lokasyonlar: List<DropdownOgesi>
        ): MalzemeEkleDialogFragment {
            return MalzemeEkleDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, kayit.id)
                    putString(ARG_AD, kayit.ad)
                    putDouble(ARG_MIKTAR, kayit.miktarDegeri)
                    putString(ARG_BIRIM, kayit.miktarBirimi)
                    putInt(ARG_KATEGORI_ID, kayit.kategoriId ?: 0)
                    putInt(ARG_LOKASYON_ID, kayit.lokasyonId ?: 0)
                    putSerializable(ARG_KATEGORILER, ArrayList(kategoriler))
                    putSerializable(ARG_LOKASYONLAR, ArrayList(lokasyonlar))
                }
            }
        }
    }
}
