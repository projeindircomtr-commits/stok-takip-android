package com.salman.stoktakip.ui.arac

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.salman.stoktakip.data.local.entity.AracCacheEntity
import com.salman.stoktakip.databinding.DialogAracEkleBinding
import com.salman.stoktakip.util.DropdownOgesi
import com.salman.stoktakip.util.bitmapDosyasiniBase64eCevir
import com.salman.stoktakip.util.kameraIcinGeciciUriOlustur

class AracEkleDialogFragment : DialogFragment() {

    private var _binding: DialogAracEkleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AraclarViewModel by activityViewModels()
    private var secilenResimUri: Uri? = null
    private var duzenlenenId: Int? = null
    private var kameraGeciciUri: Uri? = null

    private val galeriSec = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            secilenResimUri = uri
            Glide.with(this).load(uri).centerCrop().into(binding.imgOnizleme)
        }
    }

    private val kameraCek = registerForActivityResult(ActivityResultContracts.TakePicture()) { basarili ->
        if (basarili && kameraGeciciUri != null) {
            secilenResimUri = kameraGeciciUri
            Glide.with(this).load(kameraGeciciUri).centerCrop().into(binding.imgOnizleme)
        }
    }

    private val kameraIzniIste = registerForActivityResult(ActivityResultContracts.RequestPermission()) { verildi ->
        if (verildi) kamerayiBaslat()
    }

    private fun kamerayiBaslat() {
        val uri = kameraIcinGeciciUriOlustur(requireContext())
        kameraGeciciUri = uri
        kameraCek.launch(uri)
    }

    private fun resimSecimMenusuGoster(ankor: android.view.View) {
        PopupMenu(requireContext(), ankor).apply {
            menu.add("Kameradan Çek")
            menu.add("Galeriden Seç")
            setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Kameradan Çek" -> {
                        val izinVar = androidx.core.content.ContextCompat.checkSelfPermission(
                            requireContext(), android.Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (izinVar) kamerayiBaslat() else kameraIzniIste.launch(android.Manifest.permission.CAMERA)
                    }
                    "Galeriden Seç" -> galeriSec.launch("image/*")
                }
                true
            }
        }.show()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAracEkleBinding.inflate(layoutInflater)
        duzenlenenId = arguments?.getInt(ARG_ID)?.takeIf { it != 0 }

        val kategoriListesi = (arguments?.getSerializable(ARG_KATEGORILER) as? ArrayList<DropdownOgesi>).orEmpty()
        val lokasyonListesi = (arguments?.getSerializable(ARG_LOKASYONLAR) as? ArrayList<DropdownOgesi>).orEmpty()

        binding.dropdownKategori.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, kategoriListesi)
        )
        binding.dropdownLokasyon.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, lokasyonListesi)
        )

        arguments?.let { args ->
            if (duzenlenenId != null) {
                binding.editAracIsmi.setText(args.getString(ARG_ISIM))
                binding.editModel.setText(args.getString(ARG_MODEL))
                binding.editPlaka.setText(args.getString(ARG_PLAKA))
                binding.editSahip.setText(args.getString(ARG_SAHIP))
                binding.editTelefon.setText(args.getString(ARG_TELEFON))
                binding.checkKamera.isChecked = args.getString(ARG_KAMERA) == "Var"
                binding.checkGps.isChecked = args.getString(ARG_GPS) == "Var"
                kategoriListesi.find { it.id == args.getInt(ARG_KATEGORI_ID) }?.let {
                    binding.dropdownKategori.setText(it.ad, false)
                }
                lokasyonListesi.find { it.id == args.getInt(ARG_LOKASYON_ID) }?.let {
                    binding.dropdownLokasyon.setText(it.ad, false)
                }
            }
        }

        binding.btnResimSec.setOnClickListener { resimSecimMenusuGoster(it) }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (duzenlenenId != null) "Araç Güncelle" else "Yeni Araç")
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
        val isim = binding.editAracIsmi.text?.toString()?.trim().orEmpty()
        if (isim.isEmpty()) {
            binding.editAracIsmi.error = "Zorunlu alan"
            return
        }
        val model = binding.editModel.text?.toString()?.trim()
        val plaka = binding.editPlaka.text?.toString()?.trim()
        val sahip = binding.editSahip.text?.toString()?.trim()
        val telefon = binding.editTelefon.text?.toString()?.trim()
        val kamera = if (binding.checkKamera.isChecked) "Var" else "Yok"
        val gps = if (binding.checkGps.isChecked) "Var" else "Yok"
        val kategoriId = kategoriler.find { it.ad == binding.dropdownKategori.text?.toString() }?.id
        val lokasyonId = lokasyonlar.find { it.ad == binding.dropdownLokasyon.text?.toString() }?.id
        val resim = secilenResimUri?.let { bitmapDosyasiniBase64eCevir(requireContext(), it) }

        val id = duzenlenenId
        if (id != null) {
            viewModel.guncelle(id, isim, model, plaka, kamera, gps, sahip, telefon, kategoriId, lokasyonId, resim)
        } else {
            viewModel.ekle(isim, model, plaka, kamera, gps, sahip, telefon, kategoriId, lokasyonId, resim)
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_ISIM = "isim"
        private const val ARG_MODEL = "model"
        private const val ARG_PLAKA = "plaka"
        private const val ARG_SAHIP = "sahip"
        private const val ARG_TELEFON = "telefon"
        private const val ARG_KAMERA = "kamera"
        private const val ARG_GPS = "gps"
        private const val ARG_KATEGORI_ID = "kategori_id"
        private const val ARG_LOKASYON_ID = "lokasyon_id"
        private const val ARG_KATEGORILER = "kategoriler"
        private const val ARG_LOKASYONLAR = "lokasyonlar"

        fun yeni(kategoriler: List<DropdownOgesi>, lokasyonlar: List<DropdownOgesi>): AracEkleDialogFragment {
            return AracEkleDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_KATEGORILER, ArrayList(kategoriler))
                    putSerializable(ARG_LOKASYONLAR, ArrayList(lokasyonlar))
                }
            }
        }

        fun duzenle(
            kayit: AracCacheEntity,
            kategoriler: List<DropdownOgesi>,
            lokasyonlar: List<DropdownOgesi>
        ): AracEkleDialogFragment {
            return AracEkleDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, kayit.id)
                    putString(ARG_ISIM, kayit.aracIsmi)
                    putString(ARG_MODEL, kayit.model)
                    putString(ARG_PLAKA, kayit.plaka)
                    putString(ARG_SAHIP, kayit.sahip)
                    putString(ARG_TELEFON, kayit.telefon)
                    putString(ARG_KAMERA, kayit.kamera)
                    putString(ARG_GPS, kayit.gps)
                    putInt(ARG_KATEGORI_ID, kayit.kategoriId ?: 0)
                    putInt(ARG_LOKASYON_ID, kayit.lokasyonId ?: 0)
                    putSerializable(ARG_KATEGORILER, ArrayList(kategoriler))
                    putSerializable(ARG_LOKASYONLAR, ArrayList(lokasyonlar))
                }
            }
        }
    }
}
