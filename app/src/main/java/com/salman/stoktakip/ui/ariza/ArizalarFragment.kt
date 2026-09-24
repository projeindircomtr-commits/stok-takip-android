package com.salman.stoktakip.ui.ariza

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.data.remote.dto.ArizaBildirim
import com.salman.stoktakip.databinding.DialogArizaEkleBinding
import com.salman.stoktakip.databinding.FragmentArizalarBinding
import com.salman.stoktakip.databinding.ItemArizaBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.Resource
import com.salman.stoktakip.util.tarihiBicimlendir
import kotlinx.coroutines.launch

class ArizalarFragment : Fragment() {

    private var _binding: FragmentArizalarBinding? = null
    private val binding get() = _binding!!
    private var secilenDurum: String? = "acik"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentArizalarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }
        binding.swipeRefresh.setOnRefreshListener { yukle() }
        binding.fabEkle.setOnClickListener { eklemeDialoguGoster() }

        binding.tabDurum.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                secilenDurum = when (tab.position) {
                    0 -> "acik"
                    1 -> "kapali"
                    else -> null
                }
                yukle()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        yukle()
    }

    private fun yukle() {
        val repo = ServiceLocator.stokRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            when (val sonuc = repo.arizalarGetir(secilenDurum)) {
                is Resource.Basarili -> {
                    binding.listArizalar.removeAllViews()
                    if (sonuc.data.isEmpty()) {
                        binding.txtBos.visibility = View.VISIBLE
                    } else {
                        binding.txtBos.visibility = View.GONE
                        sonuc.data.forEach { ekleSatir(it) }
                    }
                }
                is Resource.Hata -> if (isAdded) Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun ekleSatir(a: ArizaBildirim) {
        val satir = ItemArizaBinding.inflate(layoutInflater, binding.listArizalar, false)
        satir.txtPlaka.text = a.plaka
        val acik = a.durum == "acik"
        satir.txtDurum.text = if (acik) "AÇIK" else "KAPALI"
        satir.txtDurum.setBackgroundColor(
            android.graphics.Color.parseColor(if (acik) "#E74C3C" else "#2ECC71")
        )
        satir.txtNot.text = a.arizaNotu

        val detaylar = mutableListOf<String>()
        if (!a.soforAdi.isNullOrBlank()) detaylar.add("Şoför: ${a.soforAdi}")
        if (!a.telefon.isNullOrBlank()) detaylar.add("Tel: ${a.telefon}")
        if (!a.konum.isNullOrBlank()) detaylar.add(a.konum)
        if (!a.createdAt.isNullOrBlank()) detaylar.add(tarihiBicimlendir(a.createdAt))
        satir.txtDetay.text = detaylar.joinToString(" • ")

        satir.btnKapat.visibility = if (acik) View.VISIBLE else View.GONE
        satir.btnKapat.setOnClickListener {
            val repo = ServiceLocator.stokRepository(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                when (val sonuc = repo.arizaDurumGuncelle(a.id, "kapali")) {
                    is Resource.Basarili -> yukle()
                    is Resource.Hata -> Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                    else -> {}
                }
            }
        }

        satir.btnSil.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sil")
                .setMessage("Bu arıza kaydını silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    val repo = ServiceLocator.stokRepository(requireContext())
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.arizaSil(a.id)
                        yukle()
                    }
                }
                .setNegativeButton("Hayır", null)
                .show()
        }

        binding.listArizalar.addView(satir.root)
    }

    private fun eklemeDialoguGoster() {
        val db = DialogArizaEkleBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Arıza Bildirimi")
            .setView(db.root)
            .setPositiveButton("Kaydet", null)
            .setNegativeButton("Vazgeç", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                        val plaka = db.editPlaka.text?.toString()?.trim().orEmpty()
                        val notMetni = db.editArizaNotu.text?.toString()?.trim().orEmpty()
                        if (plaka.isEmpty() || notMetni.isEmpty()) {
                            Snackbar.make(binding.root, "Plaka ve arıza notu zorunludur.", Snackbar.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        val sofor = db.editSofor.text?.toString()?.trim()
                        val telefon = db.editTelefon.text?.toString()?.trim()
                        val konum = db.editKonum.text?.toString()?.trim()

                        val repo = ServiceLocator.stokRepository(requireContext())
                        viewLifecycleOwner.lifecycleScope.launch {
                            when (val sonuc = repo.arizaEkle(plaka, sofor, telefon, konum, notMetni)) {
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
