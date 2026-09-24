package com.salman.stoktakip.ui.raporlar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.databinding.FragmentRaporlarBinding
import com.salman.stoktakip.databinding.ItemMiniSatirBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.launch

class RaporlarFragment : Fragment() {

    private var _binding: FragmentRaporlarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRaporlarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }
        binding.swipeRefresh.setOnRefreshListener { yukle() }
        yukle()
    }

    private fun yukle() {
        val repo = ServiceLocator.stokRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            when (val sonuc = repo.kritikStokGetir()) {
                is Resource.Basarili -> {
                    binding.listKritikStok.removeAllViews()
                    if (sonuc.data.isEmpty()) {
                        binding.txtBos.visibility = View.VISIBLE
                    } else {
                        binding.txtBos.visibility = View.GONE
                        sonuc.data.forEach { m ->
                            val satir = ItemMiniSatirBinding.inflate(layoutInflater, binding.listKritikStok, false)
                            satir.imgIkon.visibility = android.view.View.VISIBLE
                            satir.imgIkon.setImageResource(com.salman.stoktakip.R.drawable.ic_warning)
                            satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#E74C3C"))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
