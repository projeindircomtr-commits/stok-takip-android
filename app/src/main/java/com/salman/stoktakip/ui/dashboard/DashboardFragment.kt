package com.salman.stoktakip.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.databinding.FragmentDashboardBinding
import com.salman.stoktakip.databinding.ItemMiniSatirBinding
import com.salman.stoktakip.ui.arac.AraclarFragment
import com.salman.stoktakip.ui.depo.DepoFragment
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.ui.malzeme.MalzemelerFragment
import com.salman.stoktakip.ui.raporlar.RaporlarFragment
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = ServiceLocator.session(requireContext())
        val repo = ServiceLocator.stokRepository(requireContext())

        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }
        binding.txtHosgeldin.text = "Hoş geldiniz, ${session.adSoyad ?: session.kullaniciAdi}"

        binding.cardMalzemeler.txtBaslik.text = "Malzemeler"
        binding.cardAraclar.txtBaslik.text = "Araçlar"
        binding.cardKategoriler.txtBaslik.text = "Kategoriler"
        binding.cardLokasyonlar.txtBaslik.text = "Lokasyonlar"
        binding.cardKritikStok.txtBaslik.text = "Kritik Stok"
        binding.cardBekleyenSenkron.txtBaslik.text = "Bekleyen Senkron"

        binding.cardSaat.imgIkon.visibility = View.GONE
        binding.cardHava.imgIkon.visibility = View.GONE
        binding.cardUsd.imgIkon.visibility = View.GONE
        binding.cardEur.imgIkon.visibility = View.GONE
        binding.cardSaat.txtBaslik.text = "Güncel Saat"
        binding.cardHava.txtBaslik.text = "İstanbul Hava"
        binding.cardUsd.txtBaslik.text = "USD / TRY"
        binding.cardEur.txtBaslik.text = "EUR / TRY"
        binding.cardSaat.txtDeger.textSize = 15f
        binding.cardHava.txtDeger.textSize = 15f
        binding.cardUsd.txtDeger.textSize = 15f
        binding.cardEur.txtDeger.textSize = 15f
        binding.cardSaat.txtDeger.text = "…"
        binding.cardHava.txtDeger.text = "…"
        binding.cardUsd.txtDeger.text = "…"
        binding.cardEur.txtDeger.text = "…"

        binding.cardMalzemeler.root.setOnClickListener { git(MalzemelerFragment()) }
        binding.cardAraclar.root.setOnClickListener { git(AraclarFragment()) }
        binding.cardKategoriler.root.setOnClickListener { git(DepoFragment()) }
        binding.cardLokasyonlar.root.setOnClickListener { git(DepoFragment()) }
        binding.cardKritikStok.root.setOnClickListener { git(RaporlarFragment()) }
        binding.cardBekleyenSenkron.root.setOnClickListener { senkronizeEt(repo) }

        binding.btnAra.setOnClickListener {
            val sorgu = binding.editAra.text?.toString()?.trim().orEmpty()
            git(MalzemelerFragment.aramaIle(sorgu))
        }

        binding.btnSenkronize.setOnClickListener { senkronizeEt(repo) }

        binding.swipeRefresh.setOnRefreshListener { verileriYukle(repo); disVerileriYukle() }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.malzemelerGozlemle().collect { liste ->
                binding.listSonMalzemeler.removeAllViews()
                liste.take(5).forEach { m ->
                    val satir = ItemMiniSatirBinding.inflate(layoutInflater, binding.listSonMalzemeler, false)
                    satir.txtBaslik.text = m.ad
                    satir.txtAlt.text = "${formatMiktar(m.miktarDegeri)} ${m.miktarBirimi} • ${m.kategoriAdi ?: "-"}"
                    binding.listSonMalzemeler.addView(satir.root)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.araclarGozlemle().collect { liste ->
                binding.listSonAraclar.removeAllViews()
                liste.take(5).forEach { a ->
                    val satir = ItemMiniSatirBinding.inflate(layoutInflater, binding.listSonAraclar, false)
                    satir.txtBaslik.text = a.aracIsmi
                    satir.txtAlt.text = "${a.plaka ?: "-"} • ${a.lokasyonAdi ?: "-"}"
                    binding.listSonAraclar.addView(satir.root)
                }
            }
        }

        verileriYukle(repo)
        disVerileriYukle()
        saatHandler.post(saatRunnable)
    }

    private val saatHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val saatRunnable = object : Runnable {
        override fun run() {
            if (_binding == null) return
            val simdi = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale("US")).format(java.util.Date())
            binding.cardSaat.txtDeger.text = simdi
            saatHandler.postDelayed(this, 1000)
        }
    }

    private fun disVerileriYukle() {
        viewLifecycleOwner.lifecycleScope.launch {
            val hava = com.salman.stoktakip.util.DisHizmetler.havaDurumuGetir()
            if (isAdded) binding.cardHava.txtDeger.text = hava ?: "Alınamadı"
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val usd = com.salman.stoktakip.util.DisHizmetler.kurGetir("USD")
            if (isAdded) binding.cardUsd.txtDeger.text = usd?.let { String.format(java.util.Locale.US, "%.4f", it) } ?: "Alınamadı"
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val eur = com.salman.stoktakip.util.DisHizmetler.kurGetir("EUR")
            if (isAdded) binding.cardEur.txtDeger.text = eur?.let { String.format(java.util.Locale.US, "%.4f", it) } ?: "Alınamadı"
        }
    }

    private fun git(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(com.salman.stoktakip.R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun senkronizeEt(repo: com.salman.stoktakip.data.repo.StokRepository) {
        viewLifecycleOwner.lifecycleScope.launch {
            val sonuc = repo.bekleyenleriSenkronize()
            verileriYukle(repo)
            val mesaj = when (sonuc) {
                is Resource.Basarili -> "Senkronizasyon tamamlandı."
                is Resource.Hata -> sonuc.mesaj
                else -> ""
            }
            if (mesaj.isNotEmpty() && isAdded) Snackbar.make(binding.root, mesaj, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun verileriYukle(repo: com.salman.stoktakip.data.repo.StokRepository) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            repo.tumVeriyiYenile()
            when (val sonuc = repo.dashboardGetir()) {
                is Resource.Basarili -> guncelle(sonuc.data)
                else -> { /* offline: sadece onbellek sayilariyla devam */ }
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun guncelle(d: com.salman.stoktakip.data.remote.dto.Dashboard) {
        if (!isAdded) return
        binding.cardMalzemeler.txtDeger.text = d.malzemeSayisi.toString()
        binding.cardAraclar.txtDeger.text = d.aracSayisi.toString()
        binding.cardKategoriler.txtDeger.text = d.kategoriSayisi.toString()
        binding.cardLokasyonlar.txtDeger.text = d.lokasyonSayisi.toString()
        binding.cardKritikStok.txtDeger.text = d.kritikStok.toString()
        binding.cardBekleyenSenkron.txtDeger.text = d.bekleyenSenkron.toString()

        grafikleriCiz(d)
    }

    private fun grafikleriCiz(d: com.salman.stoktakip.data.remote.dto.Dashboard) {
        val kategoriler = d.kategoriDagilimi.orEmpty()
        val barGirdiler = kategoriler.mapIndexed { i, k -> BarEntry(i.toFloat(), k.adet.toFloat()) }
        val barSet = BarDataSet(barGirdiler, "Malzeme Sayısı").apply {
            color = Color.parseColor("#00B4D8")
            valueTextSize = 11f
        }
        binding.barChartKategori.apply {
            data = BarData(barSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(kategoriler.map { it.ad })
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            animateY(500)
            invalidate()
        }

        val lokasyonlar = d.lokasyonDagilimi.orEmpty()
        val renkler = listOf(
            Color.parseColor("#0F4C81"), Color.parseColor("#00B4D8"), Color.parseColor("#2ECC71"),
            Color.parseColor("#F39C12"), Color.parseColor("#E74C3C"), Color.parseColor("#9B59B6")
        )
        val pieGirdiler = lokasyonlar.map { PieEntry(it.adet.toFloat(), it.ad) }
        val pieSet = PieDataSet(pieGirdiler, "").apply {
            colors = renkler
            valueTextSize = 11f
        }
        binding.pieChartLokasyon.apply {
            data = PieData(pieSet)
            description.isEnabled = false
            legend.isEnabled = true
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.textSize = 10f
            holeRadius = 45f
            animateY(500)
            invalidate()
        }
    }

    private fun formatMiktar(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    override fun onDestroyView() {
        super.onDestroyView()
        saatHandler.removeCallbacks(saatRunnable)
        _binding = null
    }
}
