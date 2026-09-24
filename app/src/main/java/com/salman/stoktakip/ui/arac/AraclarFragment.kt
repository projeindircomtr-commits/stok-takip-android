package com.salman.stoktakip.ui.arac

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.R
import com.salman.stoktakip.data.local.entity.AracCacheEntity
import com.salman.stoktakip.databinding.FragmentAraclarBinding
import com.salman.stoktakip.util.DropdownOgesi
import com.salman.stoktakip.util.Resource

class AraclarFragment : Fragment() {

    private var _binding: FragmentAraclarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AraclarViewModel by viewModels()
    private lateinit var adapter: AracAdapter

    private var kategoriListesi: List<DropdownOgesi> = emptyList()
    private var lokasyonListesi: List<DropdownOgesi> = emptyList()
    private var tamListe: List<AracCacheEntity> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAraclarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AracAdapter(
            onDuzenle = { kayit ->
                AracEkleDialogFragment.duzenle(kayit, kategoriListesi, lokasyonListesi)
                    .show(childFragmentManager, "duzenle")
            },
            onSil = { kayit -> silOnayiSor(kayit) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.yenile() }

        binding.fabEkle.setOnClickListener {
            AracEkleDialogFragment.yeni(kategoriListesi, lokasyonListesi).show(childFragmentManager, "ekle")
        }

        binding.editArama.addTextChangedListener { metin -> filtrele(metin?.toString().orEmpty()) }

        viewModel.araclar.observe(viewLifecycleOwner) { liste ->
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
                Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.yenile()
    }

    private fun filtrele(sorgu: String) {
        val filtreli = if (sorgu.isBlank()) tamListe else tamListe.filter {
            it.aracIsmi.contains(sorgu, ignoreCase = true) ||
                (it.plaka?.contains(sorgu, ignoreCase = true) == true) ||
                (it.sahip?.contains(sorgu, ignoreCase = true) == true)
        }
        adapter.submitList(filtreli)
        binding.txtBos.visibility = if (filtreli.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun silOnayiSor(kayit: AracCacheEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(kayit.aracIsmi)
            .setMessage(getString(R.string.silme_onay))
            .setPositiveButton(R.string.evet) { _, _ -> viewModel.sil(kayit.id) }
            .setNegativeButton(R.string.hayir, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun com.google.android.material.textfield.TextInputEditText.addTextChangedListener(afterTextChanged: (CharSequence?) -> Unit) {
    this.addTextChangedListener(object : android.text.TextWatcher {
        override fun afterTextChanged(s: android.text.Editable?) { afterTextChanged(s) }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}
