package com.salman.stoktakip.ui.malzeme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.salman.stoktakip.data.local.entity.MalzemeCacheEntity
import com.salman.stoktakip.databinding.FragmentMalzemelerBinding
import com.salman.stoktakip.util.DropdownOgesi
import com.salman.stoktakip.util.Resource

class MalzemelerFragment : Fragment() {

    private var _binding: FragmentMalzemelerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MalzemelerViewModel by viewModels()
    private lateinit var adapter: MalzemeAdapter

    private var kategoriListesi: List<DropdownOgesi> = emptyList()
    private var lokasyonListesi: List<DropdownOgesi> = emptyList()
    private var tamListe: List<MalzemeCacheEntity> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMalzemelerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            (activity as? com.salman.stoktakip.ui.main.MainActivity)?.cekmeceyiAc()
        }
        binding.btnCsvDisaAktar.setOnClickListener { csvDisaAktar() }
        binding.btnPdfDisaAktar.setOnClickListener { pdfDisaAktar() }

        adapter = MalzemeAdapter(
            onDuzenle = { kayit ->
                MalzemeEkleDialogFragment.duzenle(kayit, kategoriListesi, lokasyonListesi)
                    .show(childFragmentManager, "duzenle")
            },
            onSil = { kayit -> silOnayiSor(kayit) },
            onDetay = { kayit -> MalzemeDetayDialogFragment.yeni(kayit).show(childFragmentManager, "detay") }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.yenile() }

        binding.fabEkle.setOnClickListener {
            MalzemeEkleDialogFragment.yeni(kategoriListesi, lokasyonListesi).show(childFragmentManager, "ekle")
        }

        binding.editArama.addTextChangedListener(afterTextChanged = { metin ->
            filtrele(metin?.toString().orEmpty())
        })

        viewModel.malzemeler.observe(viewLifecycleOwner) { liste ->
            tamListe = liste
            filtrele(binding.editArama.text?.toString().orEmpty())
        }

        viewModel.kategoriler.observe(viewLifecycleOwner) { liste ->
            kategoriListesi = liste.map { DropdownOgesi(it.id, it.ad) }
        }
        viewModel.lokasyonlar.observe(viewLifecycleOwner) { liste ->
            lokasyonListesi = liste.map { DropdownOgesi(it.id, it.ad) }
        }

        viewModel.yenileniyor.observe(viewLifecycleOwner) { binding.swipeRefresh.isRefreshing = it }

        viewModel.bekleyenSayi.observe(viewLifecycleOwner) { sayi ->
            binding.txtCevrimdisiUyari.visibility = if (sayi > 0) View.VISIBLE else View.GONE
        }

        viewModel.islemSonucu.observe(viewLifecycleOwner) { sonuc ->
            if (sonuc is Resource.Hata) {
                com.google.android.material.snackbar.Snackbar.make(binding.root, sonuc.mesaj, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.yenile()

        arguments?.getString(ARG_ILK_SORGU)?.let { sorgu ->
            if (sorgu.isNotBlank()) binding.editArama.setText(sorgu)
        }

        if (arguments?.getBoolean(ARG_AUTO_ACIL, false) == true) {
            arguments?.putBoolean(ARG_AUTO_ACIL, false)
            view.postDelayed({
                if (isAdded) {
                    MalzemeEkleDialogFragment.yeni(kategoriListesi, lokasyonListesi).show(childFragmentManager, "ekle")
                }
            }, 400)
        }
    }

    private fun filtrele(sorgu: String) {
        val filtreli = if (sorgu.isBlank()) tamListe else tamListe.filter {
            it.ad.contains(sorgu, ignoreCase = true) ||
                (it.kategoriAdi?.contains(sorgu, ignoreCase = true) == true) ||
                (it.lokasyonAdi?.contains(sorgu, ignoreCase = true) == true)
        }
        adapter.submitList(filtreli)
        binding.txtBos.visibility = if (filtreli.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun silOnayiSor(kayit: MalzemeCacheEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(kayit.ad)
            .setMessage(getString(com.salman.stoktakip.R.string.silme_onay))
            .setPositiveButton(com.salman.stoktakip.R.string.evet) { _, _ -> viewModel.sil(kayit.id) }
            .setNegativeButton(com.salman.stoktakip.R.string.hayir, null)
            .show()
    }

    private fun csvDisaAktar() {
        val basliklar = listOf("Malzeme", "Miktar", "Birim", "Kategori", "Lokasyon")
        val satirlar = tamListe.map {
            listOf(it.ad, it.miktarDegeri.toString(), it.miktarBirimi, it.kategoriAdi ?: "-", it.lokasyonAdi ?: "-")
        }
        com.salman.stoktakip.util.ExportYardimcisi.csvPaylas(requireContext(), "malzemeler", basliklar, satirlar)
    }

    private fun pdfDisaAktar() {
        val basliklar = listOf("Malzeme", "Miktar", "Birim", "Kategori", "Lokasyon")
        val satirlar = tamListe.map {
            listOf(it.ad, it.miktarDegeri.toString(), it.miktarBirimi, it.kategoriAdi ?: "-", it.lokasyonAdi ?: "-")
        }
        com.salman.stoktakip.util.ExportYardimcisi.pdfPaylas(requireContext(), "malzemeler", "Malzeme Listesi", basliklar, satirlar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_AUTO_ACIL = "auto_acil"
        private const val ARG_ILK_SORGU = "ilk_sorgu"

        /** Drawer'daki "Malzeme Ekle" ile acilinca ekleme formu otomatik gorunur. */
        fun yeniKayitIle(): MalzemelerFragment = MalzemelerFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_AUTO_ACIL, true) }
        }

        /** Ana sayfadaki arama kutusundan gelince arama sorgusu ile acilir. */
        fun aramaIle(sorgu: String): MalzemelerFragment = MalzemelerFragment().apply {
            arguments = Bundle().apply { putString(ARG_ILK_SORGU, sorgu) }
        }
    }
}

/** EditText icin sade bir addTextChangedListener yardimcisi. */
private fun com.google.android.material.textfield.TextInputEditText.addTextChangedListener(afterTextChanged: (CharSequence?) -> Unit) {
    this.addTextChangedListener(object : android.text.TextWatcher {
        override fun afterTextChanged(s: android.text.Editable?) { afterTextChanged(s) }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}
