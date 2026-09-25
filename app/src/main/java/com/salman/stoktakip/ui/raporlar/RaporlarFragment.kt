package com.salman.stoktakip.ui.raporlar

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
import com.salman.stoktakip.R
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.data.remote.dto.KritikMalzeme
import com.salman.stoktakip.databinding.FragmentRaporlarBinding
import com.salman.stoktakip.databinding.ItemMiniSatirBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.ExportYardimcisi
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.launch

class RaporlarFragment : Fragment() {

    private var _binding: FragmentRaporlarBinding? = null
    private val binding get() = _binding!!
    private var sonKritikListe: List<KritikMalzeme> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRaporlarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }
        binding.swipeRefresh.setOnRefreshListener { yukle() }
        binding.btnCsvDisaAktar.setOnClickListener { csvDisaAktar() }
        binding.btnPdfDisaAktar.setOnClickListener { pdfDisaAktar() }

        kartBasliklariniAyarla()
        yukle()
    }

    private fun kartBasliklariniAyarla() {
        binding.cardMalzeme.imgIkon.setImageResource(R.drawable.ic_box)
        binding.cardArac.imgIkon.setImageResource(R.drawable.ic_truck)
        binding.cardKategori.imgIkon.setImageResource(R.drawable.ic_tag)
        binding.cardKritik.imgIkon.setImageResource(R.drawable.ic_warning)
        listOf(binding.cardMalzeme, binding.cardArac, binding.cardKategori).forEach {
            it.imgIkon.setColorFilter(Color.parseColor("#0F4C81"))
        }
        binding.cardKritik.imgIkon.setColorFilter(Color.parseColor("#E74C3C"))
        binding.cardMalzeme.txtBaslik.text = "Toplam Malzeme"
        binding.cardArac.txtBaslik.text = "Toplam Araç"
        binding.cardKategori.txtBaslik.text = "Kategori Sayısı"
        binding.cardKritik.txtBaslik.text = "Kritik Stok"
    }

    private fun yukle() {
        val repo = ServiceLocator.stokRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true

            when (val sonuc = repo.dashboardGetir()) {
                is Resource.Basarili -> {
                    val d = sonuc.data
                    binding.cardMalzeme.txtDeger.text = d.malzemeSayisi.toString()
                    binding.cardArac.txtDeger.text = d.aracSayisi.toString()
                    binding.cardKategori.txtDeger.text = d.kategoriSayisi.toString()
                    binding.cardKritik.txtDeger.text = d.kritikStok.toString()
                    grafikleriCiz(d.kategoriDagilimi.orEmpty(), d.lokasyonDagilimi.orEmpty())
                }
                else -> {}
            }

            when (val sonuc = repo.kritikStokGetir()) {
                is Resource.Basarili -> {
                    sonKritikListe = sonuc.data
                    binding.listKritikStok.removeAllViews()
                    if (sonuc.data.isEmpty()) {
                        binding.txtBos.visibility = View.VISIBLE
                    } else {
                        binding.txtBos.visibility = View.GONE
                        sonuc.data.forEach { m ->
                            val satir = ItemMiniSatirBinding.inflate(layoutInflater, binding.listKritikStok, false)
                            satir.imgIkon.visibility = View.VISIBLE
                            satir.imgIkon.setImageResource(R.drawable.ic_warning)
                            satir.imgIkon.setColorFilter(Color.parseColor("#E74C3C"))
                            satir.txtBaslik.text = m.ad
                            val miktar = if (m.miktarDegeri % 1.0 == 0.0) m.miktarDegeri.toInt().toString() else m.miktarDegeri.toString()
                            satir.txtAlt.text = "$miktar ${m.miktarBirimi} • ${m.kategori ?: "-"} • ${m.lokasyon ?: "-"}"
                            binding.listKritikStok.addView(satir.root)
                        }
                    }
                }
                is Resource.Hata -> if (isAdded) Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                else -> {}
            }

            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun grafikleriCiz(
        kategoriler: List<com.salman.stoktakip.data.remote.dto.KategoriDagilim>,
        lokasyonlar: List<com.salman.stoktakip.data.remote.dto.LokasyonDagilim>
    ) {
        try {
            if (kategoriler.isEmpty()) {
                binding.barChart.clear(); binding.barChart.invalidate()
            } else {
                val girdiler = kategoriler.mapIndexed { i, k -> BarEntry(i.toFloat(), k.adet.toFloat()) }
                val set = BarDataSet(girdiler, "Malzeme Sayısı").apply {
                    color = Color.parseColor("#00B4D8"); valueTextSize = 11f
                }
                binding.barChart.apply {
                    data = BarData(set)
                    xAxis.valueFormatter = IndexAxisValueFormatter(kategoriler.map { it.ad })
                    xAxis.granularity = 1f
                    xAxis.setDrawGridLines(false)
                    axisRight.isEnabled = false
                    description.isEnabled = false
                    legend.isEnabled = false
                    animateY(500)
                    invalidate()
                }
            }

            if (lokasyonlar.isEmpty() || lokasyonlar.sumOf { it.adet } == 0) {
                binding.pieChart.clear(); binding.pieChart.invalidate()
            } else {
                val renkler = listOf(
                    Color.parseColor("#0F4C81"), Color.parseColor("#00B4D8"), Color.parseColor("#2ECC71"),
                    Color.parseColor("#F39C12"), Color.parseColor("#E74C3C"), Color.parseColor("#9B59B6")
                )
                val girdiler = lokasyonlar.filter { it.adet > 0 }.map { PieEntry(it.adet.toFloat(), it.ad) }
                val set = PieDataSet(girdiler, "").apply { colors = renkler; valueTextSize = 11f }
                binding.pieChart.apply {
                    data = PieData(set)
                    description.isEnabled = false
                    legend.isEnabled = true
                    legend.orientation = Legend.LegendOrientation.HORIZONTAL
                    legend.textSize = 10f
                    holeRadius = 45f
                    animateY(500)
                    invalidate()
                }
            }
        } catch (e: Exception) {
            // Grafik cizilemese bile ekran cokmesin.
        }
    }

    private fun csvDisaAktar() {
        val basliklar = listOf("Malzeme", "Miktar", "Birim", "Kategori", "Lokasyon")
        val satirlar = sonKritikListe.map {
            listOf(it.ad, it.miktarDegeri.toString(), it.miktarBirimi, it.kategori ?: "-", it.lokasyon ?: "-")
        }
        ExportYardimcisi.csvPaylas(requireContext(), "kritik_stok_raporu", basliklar, satirlar)
    }

    private fun pdfDisaAktar() {
        val basliklar = listOf("Malzeme", "Miktar", "Kategori", "Lokasyon")
        val satirlar = sonKritikListe.map {
            listOf(it.ad, "${it.miktarDegeri} ${it.miktarBirimi}", it.kategori ?: "-", it.lokasyon ?: "-")
        }
        ExportYardimcisi.pdfPaylas(requireContext(), "kritik_stok_raporu", "Kritik Stok Raporu", basliklar, satirlar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
