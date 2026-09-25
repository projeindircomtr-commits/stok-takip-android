package com.salman.stoktakip.ui.evrak

import android.app.DatePickerDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.data.remote.dto.Evrak
import com.salman.stoktakip.databinding.DialogEvrakEkleBinding
import com.salman.stoktakip.databinding.FragmentEvrakBinding
import com.salman.stoktakip.databinding.ItemEvrakBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.DropdownOgesi
import com.salman.stoktakip.util.Resource
import com.salman.stoktakip.util.bitmapDosyasiniBase64eCevir
import com.salman.stoktakip.util.kameraIcinGeciciUriOlustur
import kotlinx.coroutines.launch
import java.util.Calendar

class EvrakFragment : Fragment() {

    private var _binding: FragmentEvrakBinding? = null
    private val binding get() = _binding!!
    private var adminMi = false
    private var secilenResimUri: Uri? = null
    private var kameraGeciciUri: Uri? = null
    private var dialogBindingRef: DialogEvrakEkleBinding? = null

    private val galeriSec = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            secilenResimUri = uri
            dialogBindingRef?.let { Glide.with(this).load(uri).centerCrop().into(it.imgOnizleme) }
        }
    }
    private val kameraCek = registerForActivityResult(ActivityResultContracts.TakePicture()) { basarili ->
        if (basarili && kameraGeciciUri != null) {
            secilenResimUri = kameraGeciciUri
            dialogBindingRef?.let { Glide.with(this).load(kameraGeciciUri).centerCrop().into(it.imgOnizleme) }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEvrakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adminMi = ServiceLocator.session(requireContext()).adminMi

        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }
        binding.swipeRefresh.setOnRefreshListener { yukle() }
        binding.fabEkle.visibility = if (adminMi) View.VISIBLE else View.GONE
        binding.fabEkle.setOnClickListener { eklemeDialoguGoster() }

        yukle()
    }

    private fun yukle() {
        val repo = ServiceLocator.stokRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            when (val sonuc = repo.evraklarGetir()) {
                is Resource.Basarili -> {
                    val riskliSayisi = sonuc.data.count { it.kalanGun <= 15 }
                    if (riskliSayisi > 0) {
                        binding.txtUyari.visibility = View.VISIBLE
                        binding.txtUyari.text = "⚠️ $riskliSayisi evrakın süresi 15 gün içinde doluyor veya doldu"
                    } else {
                        binding.txtUyari.visibility = View.GONE
                    }

                    binding.listEvraklar.removeAllViews()
                    if (sonuc.data.isEmpty()) {
                        binding.txtBos.visibility = View.VISIBLE
                    } else {
                        binding.txtBos.visibility = View.GONE
                        sonuc.data.forEach { ekleSatir(it) }
                    }
                }
                is Resource.Hata -> if (isAdded) Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun evrakTipiAdi(kod: String): String = when (kod) {
        "ruhsat" -> "Ruhsat"; "sigorta" -> "Sigorta"; "muayene" -> "Muayene"
        "ehliyet" -> "Ehliyet"; "k_belgesi" -> "K Belgesi"; else -> "Diğer"
    }

    private fun ekleSatir(e: Evrak) {
        val satir = ItemEvrakBinding.inflate(layoutInflater, binding.listEvraklar, false)
        satir.txtBaslik.text = "${e.ilgiliAd} • ${evrakTipiAdi(e.evrakTipi)}"
        val detaylar = mutableListOf("Geçerlilik: ${e.sonGecerlilikTarihi}")
        if (!e.plaka.isNullOrBlank()) detaylar.add(e.plaka)
        if (!e.notMetni.isNullOrBlank()) detaylar.add(e.notMetni)
        satir.txtDetay.text = detaylar.joinToString(" • ")

        when {
            e.kalanGun < 0 -> {
                satir.txtDurum.text = "SÜRESİ DOLDU"
                satir.txtDurum.setBackgroundColor(android.graphics.Color.parseColor("#E74C3C"))
                satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#E74C3C"))
            }
            e.kalanGun <= 15 -> {
                satir.txtDurum.text = "${e.kalanGun} GÜN KALDI"
                satir.txtDurum.setBackgroundColor(android.graphics.Color.parseColor("#F39C12"))
                satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#F39C12"))
            }
            else -> {
                satir.txtDurum.text = "${e.kalanGun} GÜN"
                satir.txtDurum.setBackgroundColor(android.graphics.Color.parseColor("#2ECC71"))
                satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#0F4C81"))
            }
        }

        satir.btnSil.visibility = if (adminMi) View.VISIBLE else View.GONE
        satir.btnSil.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sil")
                .setMessage("Bu evrak kaydını silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    val repo = ServiceLocator.stokRepository(requireContext())
                    viewLifecycleOwner.lifecycleScope.launch { repo.evrakSil(e.id); yukle() }
                }
                .setNegativeButton("Hayır", null)
                .show()
        }

        binding.listEvraklar.addView(satir.root)
    }

    private fun resimSecimMenusuGoster(ankor: View) {
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

    private fun eklemeDialoguGoster() {
        secilenResimUri = null
        val db = DialogEvrakEkleBinding.inflate(layoutInflater)
        dialogBindingRef = db

        db.dropdownEvrakTipi.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
                listOf("Ruhsat", "Sigorta", "Muayene", "Ehliyet", "K Belgesi", "Diğer"))
        )
        db.dropdownEvrakTipi.setText("Diğer", false)
        db.btnResimSec.setOnClickListener { resimSecimMenusuGoster(it) }

        var secilenTarih: String? = null
        db.editTarih.setOnClickListener {
            val takvim = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, yil, ay, gun ->
                secilenTarih = "%04d-%02d-%02d".format(yil, ay + 1, gun)
                db.editTarih.setText("%02d.%02d.%04d".format(gun, ay + 1, yil))
            }, takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH)).show()
        }

        val repo = ServiceLocator.stokRepository(requireContext())
        var aracListesi: List<DropdownOgesi> = emptyList()
        viewLifecycleOwner.lifecycleScope.launch {
            repo.araclarGozlemle().collect { liste ->
                aracListesi = liste.map { DropdownOgesi(it.id, "${it.aracIsmi} (${it.plaka ?: "-"})") }
                db.dropdownArac.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, aracListesi))
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Evrak Ekle")
            .setView(db.root)
            .setPositiveButton("Kaydet", null)
            .setNegativeButton("Vazgeç", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                        val ilgiliAd = db.editIlgiliAd.text?.toString()?.trim().orEmpty()
                        val tarih = secilenTarih
                        if (ilgiliAd.isEmpty() || tarih == null) {
                            Snackbar.make(binding.root, "İlgili ad ve tarih zorunludur.", Snackbar.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        val evrakTipi = when (db.dropdownEvrakTipi.text?.toString()) {
                            "Ruhsat" -> "ruhsat"; "Sigorta" -> "sigorta"; "Muayene" -> "muayene"
                            "Ehliyet" -> "ehliyet"; "K Belgesi" -> "k_belgesi"; else -> "diger"
                        }
                        val aracId = aracListesi.find { it.ad == db.dropdownArac.text?.toString() }?.id
                        val notMetni = db.editNot.text?.toString()?.trim()
                        val resim = secilenResimUri?.let { bitmapDosyasiniBase64eCevir(requireContext(), it) }

                        viewLifecycleOwner.lifecycleScope.launch {
                            when (val sonuc = repo.evrakEkle(aracId, evrakTipi, ilgiliAd, tarih, resim, notMetni)) {
                                is Resource.Basarili -> { dialog.dismiss(); yukle() }
                                is Resource.Hata -> Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                                else -> {}
                            }
                        }
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialogBindingRef = null
        _binding = null
    }
}
