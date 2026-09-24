package com.salman.stoktakip.ui.yakit

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
import com.salman.stoktakip.data.remote.dto.YakitKaydi
import com.salman.stoktakip.databinding.DialogYakitEkleBinding
import com.salman.stoktakip.databinding.FragmentYakitBinding
import com.salman.stoktakip.databinding.ItemMiniSatirBinding
import com.salman.stoktakip.databinding.ItemYakitBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.Resource
import com.salman.stoktakip.util.tarihiBicimlendir
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class YakitFragment : Fragment() {

    private var _binding: FragmentYakitBinding? = null
    private val binding get() = _binding!!
    private var adminMi = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentYakitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adminMi = ServiceLocator.session(requireContext()).adminMi

        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }
        binding.swipeRefresh.setOnRefreshListener { yukle() }

        // Yerel kullanicilar sadece goruntuler; ekleme/silme SADECE admin'de.
        binding.fabEkle.visibility = if (adminMi) View.VISIBLE else View.GONE
        binding.fabEkle.setOnClickListener { eklemeDialoguGoster() }

        ozetKartBasliklariniAyarla()
        yukle()
    }

    private fun ozetKartBasliklariniAyarla() {
        binding.cardBugun.imgIkon.setImageResource(R.drawable.ic_fuel)
        binding.cardBuAy.imgIkon.setImageResource(R.drawable.ic_fuel)
        binding.cardToplamLitre.imgIkon.setImageResource(R.drawable.ic_fuel)
        binding.cardToplamHarcama.imgIkon.setImageResource(R.drawable.ic_fuel)
        listOf(binding.cardBugun, binding.cardBuAy, binding.cardToplamLitre, binding.cardToplamHarcama).forEach {
            it.imgIkon.setColorFilter(android.graphics.Color.parseColor("#00B4D8"))
        }
        binding.cardBugun.txtBaslik.text = "Bugün"
        binding.cardBuAy.txtBaslik.text = "Bu Ay"
        binding.cardToplamLitre.txtBaslik.text = "Toplam Litre"
        binding.cardToplamHarcama.txtBaslik.text = "Toplam Harcama"
    }

    private fun yukle() {
        val repo = ServiceLocator.stokRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            when (val sonuc = repo.yakitKayitlariGetir()) {
                is Resource.Basarili -> ekraniDoldur(sonuc.data)
                is Resource.Hata -> if (isAdded) Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun ekraniDoldur(kayitlar: List<YakitKaydi>) {
        ozetleriHesapla(kayitlar)
        plakaOzetiniDoldur(kayitlar)
        kisiOzetiniDoldur(kayitlar)

        binding.listYakit.removeAllViews()
        if (kayitlar.isEmpty()) {
            binding.txtBos.visibility = View.VISIBLE
        } else {
            binding.txtBos.visibility = View.GONE
            kayitlar.forEach { ekleSatir(it) }
        }
    }

    private fun bugunMu(tarih: String): Boolean = gunFarki(tarih) == 0
    private fun buAyMi(tarih: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val kayit = Calendar.getInstance().apply { time = sdf.parse(tarih) ?: return false }
            val simdi = Calendar.getInstance()
            kayit.get(Calendar.YEAR) == simdi.get(Calendar.YEAR) && kayit.get(Calendar.MONTH) == simdi.get(Calendar.MONTH)
        } catch (e: Exception) { false }
    }
    private fun gunFarki(tarih: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val kayit = Calendar.getInstance().apply { time = sdf.parse(tarih) ?: return -1 }
            val simdi = Calendar.getInstance()
            if (kayit.get(Calendar.YEAR) == simdi.get(Calendar.YEAR) &&
                kayit.get(Calendar.DAY_OF_YEAR) == simdi.get(Calendar.DAY_OF_YEAR)) 0 else -1
        } catch (e: Exception) { -1 }
    }

    private fun ozetleriHesapla(kayitlar: List<YakitKaydi>) {
        val bugun = kayitlar.filter { !it.createdAt.isNullOrBlank() && bugunMu(it.createdAt) }
        val buAy = kayitlar.filter { !it.createdAt.isNullOrBlank() && buAyMi(it.createdAt) }
        val toplamLitre = kayitlar.sumOf { it.litre }
        val toplamHarcama = kayitlar.sumOf { (it.birimFiyat ?: 0.0) * it.litre }

        binding.cardBugun.txtDeger.text = "${"%.1f".format(bugun.sumOf { it.litre })} L"
        binding.cardBuAy.txtDeger.text = "${"%.1f".format(buAy.sumOf { it.litre })} L"
        binding.cardToplamLitre.txtDeger.text = "${"%.1f".format(toplamLitre)} L"
        binding.cardToplamHarcama.txtDeger.text = if (toplamHarcama > 0)
            "${"%,.0f".format(toplamHarcama)} ₺" else "-"
    }

    private fun plakaOzetiniDoldur(kayitlar: List<YakitKaydi>) {
        binding.listPlakaOzet.removeAllViews()
        val gruplu = kayitlar.groupBy { it.plaka.uppercase(Locale.getDefault()) }
            .mapValues { it.value.sumOf { k -> k.litre } }
            .toList().sortedByDescending { it.second }
        if (gruplu.isEmpty()) return
        gruplu.take(10).forEach { (plaka, litre) ->
            val satir = ItemMiniSatirBinding.inflate(layoutInflater, binding.listPlakaOzet, false)
            satir.imgIkon.visibility = View.VISIBLE
            satir.imgIkon.setImageResource(R.drawable.ic_truck)
            satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#0F4C81"))
            satir.txtBaslik.text = plaka
            satir.txtAlt.text = "${"%.1f".format(litre)} L"
            binding.listPlakaOzet.addView(satir.root)
        }
    }

    private fun kisiOzetiniDoldur(kayitlar: List<YakitKaydi>) {
        binding.listKisiOzet.removeAllViews()
        val gruplu = kayitlar
            .filter { !it.verilenAd.isNullOrBlank() || !it.verilenSoyad.isNullOrBlank() }
            .groupBy { listOfNotNull(it.verilenAd, it.verilenSoyad).joinToString(" ").trim() }
            .mapValues { it.value.sumOf { k -> k.litre } }
            .toList().sortedByDescending { it.second }
        if (gruplu.isEmpty()) {
            val bos = ItemMiniSatirBinding.inflate(layoutInflater, binding.listKisiOzet, false)
            bos.txtBaslik.text = "Kayıt yok"
            bos.txtAlt.text = ""
            binding.listKisiOzet.addView(bos.root)
            return
        }
        gruplu.take(10).forEach { (kisi, litre) ->
            val satir = ItemMiniSatirBinding.inflate(layoutInflater, binding.listKisiOzet, false)
            satir.imgIkon.visibility = View.VISIBLE
            satir.imgIkon.setImageResource(R.drawable.ic_users)
            satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#2ECC71"))
            satir.txtBaslik.text = kisi.ifBlank { "Belirtilmemiş" }
            satir.txtAlt.text = "${"%.1f".format(litre)} L"
            binding.listKisiOzet.addView(satir.root)
        }
    }

    private fun ekleSatir(y: YakitKaydi) {
        val satir = ItemYakitBinding.inflate(layoutInflater, binding.listYakit, false)
        satir.txtPlaka.text = y.plaka
        val verilenKisi = listOfNotNull(y.verilenAd, y.verilenSoyad).joinToString(" ").trim()
        val detaylar = mutableListOf<String>()
        detaylar.add(if (y.yakitTipi == "mazot") "Mazot" else "Benzin")
        if (verilenKisi.isNotEmpty()) detaylar.add("Veren: $verilenKisi")
        if (y.birimFiyat != null && y.birimFiyat > 0) {
            detaylar.add("${"%.2f".format(y.birimFiyat)} ₺/L → ${"%.0f".format(y.birimFiyat * y.litre)} ₺")
        }
        if (!y.createdAt.isNullOrBlank()) detaylar.add(tarihiBicimlendir(y.createdAt))
        satir.txtDetay.text = detaylar.joinToString(" • ")
        satir.txtLitre.text = "${"%.1f".format(y.litre)} L"

        // Yerel kullanicilar sadece goruntuler - silme SADECE admin'de.
        satir.btnSil.visibility = if (adminMi) View.VISIBLE else View.GONE
        satir.btnSil.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sil")
                .setMessage("Bu yakıt kaydını silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    val repo = ServiceLocator.stokRepository(requireContext())
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.yakitSil(y.id)
                        yukle()
                    }
                }
                .setNegativeButton("Hayır", null)
                .show()
        }

        binding.listYakit.addView(satir.root)
    }

    private fun eklemeDialoguGoster() {
        val db = DialogYakitEkleBinding.inflate(layoutInflater)
        db.dropdownYakitTipi.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("Mazot", "Benzin"))
        )
        db.dropdownYakitTipi.setText("Mazot", false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yakıt Kaydı Ekle")
            .setView(db.root)
            .setPositiveButton("Kaydet", null)
            .setNegativeButton("Vazgeç", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                        val plaka = db.editPlaka.text?.toString()?.trim().orEmpty()
                        val litre = db.editLitre.text?.toString()?.toDoubleOrNull()
                        if (plaka.isEmpty() || litre == null || litre <= 0) {
                            Snackbar.make(binding.root, "Plaka ve geçerli bir litre değeri girin.", Snackbar.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        val birimFiyat = db.editBirimFiyat.text?.toString()?.toDoubleOrNull()
                        val yakitTipi = if (db.dropdownYakitTipi.text?.toString() == "Benzin") "benzin" else "mazot"
                        val ad = db.editAd.text?.toString()?.trim()
                        val soyad = db.editSoyad.text?.toString()?.trim()
                        val telefon = db.editTelefon.text?.toString()?.trim()

                        val repo = ServiceLocator.stokRepository(requireContext())
                        viewLifecycleOwner.lifecycleScope.launch {
                            when (val sonuc = repo.yakitEkle(plaka, yakitTipi, litre, birimFiyat, ad, soyad, telefon)) {
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
        _binding = null
    }
}
