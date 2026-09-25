package com.salman.stoktakip.ui.yemek

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.R
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.data.remote.dto.YemekKaydi
import com.salman.stoktakip.databinding.DialogYemekEkleBinding
import com.salman.stoktakip.databinding.FragmentYemekBinding
import com.salman.stoktakip.databinding.ItemMiniSatirBinding
import com.salman.stoktakip.databinding.ItemYemekBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.Resource
import com.salman.stoktakip.util.tarihiBicimlendir
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class YemekFragment : Fragment() {

    private var _binding: FragmentYemekBinding? = null
    private val binding get() = _binding!!
    private var adminMi = false
    private var sonYuklenenListe: List<YemekKaydi> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentYemekBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adminMi = ServiceLocator.session(requireContext()).adminMi

        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }
        binding.swipeRefresh.setOnRefreshListener { yukle() }

        // Yemek kaydi herkes girebilir (admin sarti yok); silme SADECE admin'de.
        binding.fabEkle.visibility = View.VISIBLE
        binding.fabEkle.setOnClickListener { eklemeDialoguGoster() }
        binding.btnCsvDisaAktar.setOnClickListener { csvDisaAktar() }
        binding.btnPdfDisaAktar.setOnClickListener { pdfDisaAktar() }

        ozetKartBasliklariniAyarla()
        yukle()
    }

    private fun ozetKartBasliklariniAyarla() {
        listOf(binding.cardBugun, binding.cardBuAy, binding.cardToplamKisi, binding.cardToplamYemek).forEach {
            it.imgIkon.setImageResource(R.drawable.ic_food)
            it.imgIkon.setColorFilter(android.graphics.Color.parseColor("#2ECC71"))
        }
        binding.cardBugun.txtBaslik.text = "Bugün (Adet)"
        binding.cardBuAy.txtBaslik.text = "Bu Ay (Adet)"
        binding.cardToplamKisi.txtBaslik.text = "Toplam Kişi"
        binding.cardToplamYemek.txtBaslik.text = "Toplam Yemek"
    }

    private fun yukle() {
        val repo = ServiceLocator.stokRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            when (val sonuc = repo.yemekKayitlariGetir()) {
                is Resource.Basarili -> ekraniDoldur(sonuc.data)
                is Resource.Hata -> if (isAdded) Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun ekraniDoldur(kayitlar: List<YemekKaydi>) {
        sonYuklenenListe = kayitlar
        ozetleriHesapla(kayitlar)
        gunlukOzetiDoldur(kayitlar)
        aylikOzetiDoldur(kayitlar)

        binding.listYemek.removeAllViews()
        if (kayitlar.isEmpty()) {
            binding.txtBos.visibility = View.VISIBLE
        } else {
            binding.txtBos.visibility = View.GONE
            kayitlar.forEach { ekleSatir(it) }
        }
    }

    private fun gunAnahtari(tarih: String): String? = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val cikti = SimpleDateFormat("dd.MM.yyyy", Locale.US)
        cikti.format(sdf.parse(tarih)!!)
    } catch (e: Exception) { null }

    private fun ayAnahtari(tarih: String): String? = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val cikti = SimpleDateFormat("MMMM yyyy", Locale("tr"))
        cikti.format(sdf.parse(tarih)!!)
    } catch (e: Exception) { null }

    private fun bugunMu(tarih: String): Boolean = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val kayit = Calendar.getInstance().apply { time = sdf.parse(tarih) ?: return false }
        val simdi = Calendar.getInstance()
        kayit.get(Calendar.YEAR) == simdi.get(Calendar.YEAR) && kayit.get(Calendar.DAY_OF_YEAR) == simdi.get(Calendar.DAY_OF_YEAR)
    } catch (e: Exception) { false }

    private fun buAyMi(tarih: String): Boolean = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val kayit = Calendar.getInstance().apply { time = sdf.parse(tarih) ?: return false }
        val simdi = Calendar.getInstance()
        kayit.get(Calendar.YEAR) == simdi.get(Calendar.YEAR) && kayit.get(Calendar.MONTH) == simdi.get(Calendar.MONTH)
    } catch (e: Exception) { false }

    private fun ozetleriHesapla(kayitlar: List<YemekKaydi>) {
        val bugun = kayitlar.filter { !it.createdAt.isNullOrBlank() && bugunMu(it.createdAt) }
        val buAy = kayitlar.filter { !it.createdAt.isNullOrBlank() && buAyMi(it.createdAt) }

        binding.cardBugun.txtDeger.text = bugun.sumOf { it.yemekAdedi }.toString()
        binding.cardBuAy.txtDeger.text = buAy.sumOf { it.yemekAdedi }.toString()
        binding.cardToplamKisi.txtDeger.text = kayitlar.sumOf { it.kisiSayisi }.toString()
        binding.cardToplamYemek.txtDeger.text = kayitlar.sumOf { it.yemekAdedi }.toString()
    }

    private fun gunlukOzetiDoldur(kayitlar: List<YemekKaydi>) {
        binding.listGunlukOzet.removeAllViews()
        val gruplu = kayitlar.filter { !it.createdAt.isNullOrBlank() }
            .groupBy { gunAnahtari(it.createdAt!!) ?: "-" }
            .mapValues { it.value.sumOf { k -> k.yemekAdedi } }
            .toList()
        if (gruplu.isEmpty()) return
        gruplu.take(10).forEach { (gun, adet) ->
            val satir = ItemMiniSatirBinding.inflate(layoutInflater, binding.listGunlukOzet, false)
            satir.imgIkon.visibility = View.VISIBLE
            satir.imgIkon.setImageResource(R.drawable.ic_food)
            satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#2ECC71"))
            satir.txtBaslik.text = gun
            satir.txtAlt.text = "$adet yemek"
            binding.listGunlukOzet.addView(satir.root)
        }
    }

    private fun aylikOzetiDoldur(kayitlar: List<YemekKaydi>) {
        binding.listAylikOzet.removeAllViews()
        val gruplu = kayitlar.filter { !it.createdAt.isNullOrBlank() }
            .groupBy { ayAnahtari(it.createdAt!!) ?: "-" }
            .mapValues { it.value.sumOf { k -> k.yemekAdedi } }
            .toList()
        if (gruplu.isEmpty()) return
        gruplu.take(12).forEach { (ay, adet) ->
            val satir = ItemMiniSatirBinding.inflate(layoutInflater, binding.listAylikOzet, false)
            satir.imgIkon.visibility = View.VISIBLE
            satir.imgIkon.setImageResource(R.drawable.ic_chart)
            satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#0F4C81"))
            satir.txtBaslik.text = ay.replaceFirstChar { it.uppercase() }
            satir.txtAlt.text = "$adet yemek"
            binding.listAylikOzet.addView(satir.root)
        }
    }

    private fun ogunAdi(kod: String): String = when (kod) {
        "kahvalti" -> "Kahvaltı"
        "ogle" -> "Öğle"
        "aksam" -> "Akşam"
        else -> "Diğer"
    }

    private fun ekleSatir(y: YemekKaydi) {
        val satir = ItemYemekBinding.inflate(layoutInflater, binding.listYemek, false)
        satir.txtBaslik.text = "${y.yemekAdedi} yemek • ${y.kisiSayisi} kişi • ${ogunAdi(y.ogun)}"
        val detaylar = mutableListOf<String>()
        if (!y.notMetni.isNullOrBlank()) detaylar.add(y.notMetni)
        if (!y.createdAt.isNullOrBlank()) detaylar.add(tarihiBicimlendir(y.createdAt))
        satir.txtDetay.text = detaylar.joinToString(" • ")

        satir.btnSil.visibility = if (adminMi) View.VISIBLE else View.GONE
        satir.btnSil.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sil")
                .setMessage("Bu yemek kaydını silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    val repo = ServiceLocator.stokRepository(requireContext())
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.yemekSil(y.id)
                        yukle()
                    }
                }
                .setNegativeButton("Hayır", null)
                .show()
        }

        binding.listYemek.addView(satir.root)
    }

    private fun eklemeDialoguGoster() {
        val db = DialogYemekEkleBinding.inflate(layoutInflater)
        db.dropdownOgun.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
                listOf("Kahvaltı", "Öğle", "Akşam", "Diğer"))
        )
        db.dropdownOgun.setText("Diğer", false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yemek Kaydı Ekle")
            .setView(db.root)
            .setPositiveButton("Kaydet", null)
            .setNegativeButton("Vazgeç", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                        val kisiSayisi = db.editKisiSayisi.text?.toString()?.toIntOrNull() ?: 0
                        val yemekAdedi = db.editYemekAdedi.text?.toString()?.toIntOrNull() ?: 0
                        if (kisiSayisi <= 0 && yemekAdedi <= 0) {
                            Snackbar.make(binding.root, "Kişi sayısı veya yemek adedi girin.", Snackbar.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        val ogunSecim = when (db.dropdownOgun.text?.toString()) {
                            "Kahvaltı" -> "kahvalti"
                            "Öğle" -> "ogle"
                            "Akşam" -> "aksam"
                            else -> "diger"
                        }
                        val not = db.editNot.text?.toString()?.trim()

                        val repo = ServiceLocator.stokRepository(requireContext())
                        viewLifecycleOwner.lifecycleScope.launch {
                            when (val sonuc = repo.yemekEkle(kisiSayisi, yemekAdedi, ogunSecim, not)) {
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

    private fun csvDisaAktar() {
        val basliklar = listOf("Yemek Adedi", "Kişi Sayısı", "Öğün", "Not", "Tarih")
        val satirlar = sonYuklenenListe.map {
            listOf(
                it.yemekAdedi.toString(), it.kisiSayisi.toString(), ogunAdi(it.ogun),
                it.notMetni ?: "", it.createdAt?.let { t -> tarihiBicimlendir(t) } ?: ""
            )
        }
        com.salman.stoktakip.util.ExportYardimcisi.csvPaylas(requireContext(), "yemek_takip", basliklar, satirlar)
    }

    private fun pdfDisaAktar() {
        val basliklar = listOf("Yemek Adedi", "Kişi Sayısı", "Öğün", "Tarih")
        val satirlar = sonYuklenenListe.map {
            listOf(it.yemekAdedi.toString(), it.kisiSayisi.toString(), ogunAdi(it.ogun),
                it.createdAt?.let { t -> tarihiBicimlendir(t) } ?: "")
        }
        com.salman.stoktakip.util.ExportYardimcisi.pdfPaylas(requireContext(), "yemek_takip", "Yemek Takip Raporu", basliklar, satirlar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
