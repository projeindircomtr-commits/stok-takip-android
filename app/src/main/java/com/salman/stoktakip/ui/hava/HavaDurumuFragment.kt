package com.salman.stoktakip.ui.hava

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.R
import com.salman.stoktakip.databinding.FragmentHavaDurumuBinding
import com.salman.stoktakip.databinding.ItemIlceHavaBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.DisHizmetler
import com.salman.stoktakip.util.IlceHava
import kotlinx.coroutines.launch

class HavaDurumuFragment : Fragment() {

    private var _binding: FragmentHavaDurumuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHavaDurumuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }
        binding.swipeRefresh.setOnRefreshListener { yukle() }
        yukle()
    }

    private fun yukle() {
        binding.progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val liste = DisHizmetler.tumIlcelerHavaDurumuGetir()
            binding.progress.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            if (liste == null) {
                if (isAdded) Snackbar.make(binding.root, "Hava durumu alınamadı, internet bağlantınızı kontrol edin.", Snackbar.LENGTH_LONG).show()
                return@launch
            }

            val riskliSayisi = liste.count { it.riskli }
            if (riskliSayisi > 0) {
                binding.txtUyari.visibility = View.VISIBLE
                binding.txtUyari.text = "⚠️ $riskliSayisi ilçede donma/kar riski var — araç ve ekip hazırlığını kontrol edin"
            } else {
                binding.txtUyari.visibility = View.GONE
            }

            binding.listIlceler.removeAllViews()
            liste.forEach { ilce -> ekleSatir(ilce) }
        }
    }

    private fun ekleSatir(ilce: IlceHava) {
        val satir = ItemIlceHavaBinding.inflate(layoutInflater, binding.listIlceler, false)
        satir.txtIlce.text = ilce.ad
        satir.txtSicaklik.text = "${ilce.sicaklik}°C"
        satir.txtDetay.text = "${ilce.havaMetni} • En düşük ${ilce.minSicaklik}°C • Rüzgar ${ilce.ruzgarKmh} km/s"

        if (ilce.riskli) {
            satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#00B4D8"))
            satir.txtSicaklik.setTextColor(android.graphics.Color.parseColor("#E74C3C"))
            satir.root.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF5F5"))
        } else {
            satir.imgIkon.setColorFilter(android.graphics.Color.parseColor("#B9CBD4"))
            satir.txtSicaklik.setTextColor(android.graphics.Color.parseColor("#0F4C81"))
            satir.root.setCardBackgroundColor(android.graphics.Color.WHITE)
        }

        binding.listIlceler.addView(satir.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
