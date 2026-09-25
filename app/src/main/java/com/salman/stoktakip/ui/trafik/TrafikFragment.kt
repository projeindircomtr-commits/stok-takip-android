package com.salman.stoktakip.ui.trafik

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.salman.stoktakip.databinding.FragmentTrafikBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.DisHizmetler

class TrafikFragment : Fragment() {

    private var _binding: FragmentTrafikBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrafikBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.visibility = View.GONE
            }
        }

        ilceButonlariniOlustur()

        // Varsayilan: Istanbul genel gorunum
        haritayiAc(41.0082, 28.9784, 11)
    }

    private fun ilceButonlariniOlustur() {
        val tumIstanbulBtn = butonOlustur("Tüm İstanbul", secili = true)
        tumIstanbulBtn.setOnClickListener {
            secimiGuncelle(tumIstanbulBtn)
            haritayiAc(41.0082, 28.9784, 11)
        }
        binding.rowIlceler.addView(tumIstanbulBtn)

        DisHizmetler.istanbulIlceleri.forEach { (ad, koordinat) ->
            val (lat, lon) = koordinat
            val btn = butonOlustur(ad)
            btn.setOnClickListener {
                secimiGuncelle(btn)
                haritayiAc(lat, lon, 14)
            }
            binding.rowIlceler.addView(btn)
        }
    }

    private fun butonOlustur(metin: String, secili: Boolean = false): MaterialButton {
        return MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = metin
            textSize = 12f
            isAllCaps = false
            cornerRadius = 40
            setPadding(28, 8, 28, 8)
            val params = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.marginEnd = 8
            layoutParams = params
            tag = secili
            if (secili) {
                setBackgroundColor(android.graphics.Color.parseColor("#0F4C81"))
                setTextColor(android.graphics.Color.WHITE)
            }
        }
    }

    private fun secimiGuncelle(secilenBtn: MaterialButton) {
        for (i in 0 until binding.rowIlceler.childCount) {
            val cocuk = binding.rowIlceler.getChildAt(i) as? MaterialButton ?: continue
            val seciliMi = cocuk == secilenBtn
            if (seciliMi) {
                cocuk.setBackgroundColor(android.graphics.Color.parseColor("#0F4C81"))
                cocuk.setTextColor(android.graphics.Color.WHITE)
            } else {
                cocuk.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                cocuk.setTextColor(android.graphics.Color.parseColor("#0F4C81"))
            }
        }
    }

    private fun haritayiAc(lat: Double, lon: Double, zoom: Int) {
        binding.progress.visibility = View.VISIBLE
        val url = "https://embed.waze.com/iframe?zoom=$zoom&lat=$lat&lon=$lon&pin=1"
        binding.webView.loadUrl(url)
    }

    override fun onDestroyView() {
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }
}
