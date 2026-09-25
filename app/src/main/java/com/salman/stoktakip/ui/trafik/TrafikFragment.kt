package com.salman.stoktakip.ui.trafik

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.salman.stoktakip.databinding.FragmentTrafikBinding
import com.salman.stoktakip.ui.main.MainActivity

class TrafikFragment : Fragment() {

    private var _binding: FragmentTrafikBinding? = null
    private val binding get() = _binding!!

    /**
     * IBB'nin canli trafik haritasini artik dogrudan degil, KENDI
     * SUNUCUMUZDAKI proxy uzerinden aciyoruz (bkz. trafik_proxy.php).
     * Proxy, IBB sayfasini sunucu tarafinda cekip logo/menu gibi
     * kendi arayuz ogelerini gizler ve IBB'ye ulasilamazsa otomatik
     * Waze canli haritasina yonlendirir - bu WebView tarafi hicbir
     * yedek mantigi bilmek zorunda kalmaz.
     */
    private val TRAFIK_URL = com.salman.stoktakip.BuildConfig.BASE_URL.removeSuffix("mobile-api/") + "trafik_proxy.php"

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
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.visibility = View.GONE
                binding.txtHataMesaji.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    binding.progress.visibility = View.GONE
                    binding.txtHataMesaji.visibility = View.VISIBLE
                }
            }
        }

        binding.webView.loadUrl(TRAFIK_URL)
    }

    /** Bu ekrana girince yatay moda zorla, cikinca normale don. */
    override fun onResume() {
        super.onResume()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onPause() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onPause()
    }

    override fun onDestroyView() {
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }
}
