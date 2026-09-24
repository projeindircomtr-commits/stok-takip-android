package com.salman.stoktakip.ui.depo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.databinding.FragmentDepoBinding
import com.salman.stoktakip.databinding.ItemBasitSatirBinding
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.launch

class DepoFragment : Fragment() {

    private var _binding: FragmentDepoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDepoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = ServiceLocator.session(requireContext())
        val repo = ServiceLocator.stokRepository(requireContext())

        if (session.adminMi) {
            binding.rowKategoriEkle.visibility = View.VISIBLE
            binding.rowLokasyonEkle.visibility = View.VISIBLE
        }

        binding.btnKategoriEkle.setOnClickListener {
            val ad = binding.editYeniKategori.text?.toString()?.trim().orEmpty()
            if (ad.isEmpty()) return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                when (val sonuc = repo.kategoriEkle(ad)) {
                    is Resource.Hata -> Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                    else -> binding.editYeniKategori.text?.clear()
                }
            }
        }

        binding.btnLokasyonEkle.setOnClickListener {
            val ad = binding.editYeniLokasyon.text?.toString()?.trim().orEmpty()
            if (ad.isEmpty()) return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                when (val sonuc = repo.lokasyonEkle(ad)) {
                    is Resource.Hata -> Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                    else -> binding.editYeniLokasyon.text?.clear()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.kategorilerGozlemle().collect { liste ->
                binding.listKategoriler.removeAllViews()
                liste.forEach { kategori ->
                    val satir = ItemBasitSatirBinding.inflate(layoutInflater, binding.listKategoriler, false)
                    satir.root.text = kategori.ad
                    binding.listKategoriler.addView(satir.root)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.lokasyonlarGozlemle().collect { liste ->
                binding.listLokasyonlar.removeAllViews()
                liste.forEach { lokasyon ->
                    val satir = ItemBasitSatirBinding.inflate(layoutInflater, binding.listLokasyonlar, false)
                    satir.root.text = lokasyon.ad
                    binding.listLokasyonlar.addView(satir.root)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
