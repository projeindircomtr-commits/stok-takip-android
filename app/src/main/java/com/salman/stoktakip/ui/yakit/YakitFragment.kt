package com.salman.stoktakip.ui.yakit

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.data.remote.dto.YakitKaydi
import com.salman.stoktakip.databinding.DialogYakitEkleBinding
import com.salman.stoktakip.databinding.FragmentYakitBinding
import com.salman.stoktakip.databinding.ItemYakitBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.Resource
import com.salman.stoktakip.util.tarihiBicimlendir
import kotlinx.coroutines.launch

class YakitFragment : Fragment() {

    private var _binding: FragmentYakitBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentYakitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.cekmeceyiAc() }
        binding.swipeRefresh.setOnRefreshListener { yukle() }
        binding.fabEkle.setOnClickListener { eklemeDialoguGoster() }
        yukle()
    }

    private fun yukle() {
        val repo = ServiceLocator.stokRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            when (val sonuc = repo.yakitKayitlariGetir()) {
                is Resource.Basarili -> {
                    binding.listYakit.removeAllViews()
                    binding.txtToplamLitre.text = "${"%.1f".format(sonuc.data.sumOf { it.litre })} L"
                    binding.txtKayitSayisi.text = sonuc.data.size.toString()
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

    private fun ekleSatir(y: YakitKaydi) {
        val satir = ItemYakitBinding.inflate(layoutInflater, binding.listYakit, false)
        satir.txtPlaka.text = y.plaka
        val verilenKisi = listOfNotNull(y.verilenAd, y.verilenSoyad).joinToString(" ").trim()
        val detaylar = mutableListOf<String>()
        detaylar.add(if (y.yakitTipi == "mazot") "Mazot" else "Benzin")
        if (verilenKisi.isNotEmpty()) detaylar.add("Veren: $verilenKisi")
        if (!y.verilenTelefon.isNullOrBlank()) detaylar.add(y.verilenTelefon)
        if (!y.createdAt.isNullOrBlank()) detaylar.add(tarihiBicimlendir(y.createdAt))
        satir.txtDetay.text = detaylar.joinToString(" • ")
        satir.txtLitre.text = "${"%.1f".format(y.litre)} L"

        satir.btnSil.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sil")
                .setMessage("Bu yakıt kaydını silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    val repo = ServiceLocator.stokRepository(requireContext())
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.yakitSil(y.id)
                        yukle()
                    }
                }
                .setNegativeButton("Hayır", null)
                .show()
        }

        binding.listYakit.addView(satir.root)
    }

    private fun eklemeDialoguGoster() {
        val db = DialogYakitEkleBinding.inflate(layoutInflater)
        db.dropdownYakitTipi.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("Mazot", "Benzin"))
        )
        db.dropdownYakitTipi.setText("Mazot", false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yakıt Kaydı Ekle")
            .setView(db.root)
            .setPositiveButton("Kaydet", null)
            .setNegativeButton("Vazgeç", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                        val plaka = db.editPlaka.text?.toString()?.trim().orEmpty()
                        val litre = db.editLitre.text?.toString()?.toDoubleOrNull()
                        if (plaka.isEmpty() || litre == null || litre <= 0) {
                            Snackbar.make(binding.root, "Plaka ve geçerli bir litre değeri girin.", Snackbar.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        val yakitTipi = if (db.dropdownYakitTipi.text?.toString() == "Benzin") "benzin" else "mazot"
                        val ad = db.editAd.text?.toString()?.trim()
                        val soyad = db.editSoyad.text?.toString()?.trim()
                        val telefon = db.editTelefon.text?.toString()?.trim()

                        val repo = ServiceLocator.stokRepository(requireContext())
                        viewLifecycleOwner.lifecycleScope.launch {
                            when (val sonuc = repo.yakitEkle(plaka, yakitTipi, litre, ad, soyad, telefon)) {
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
