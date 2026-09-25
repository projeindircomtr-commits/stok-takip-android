package com.salman.stoktakip.ui.kullanici

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.databinding.DialogKullaniciEkleBinding
import com.salman.stoktakip.databinding.FragmentKullanicilarBinding
import com.salman.stoktakip.databinding.ItemKullaniciBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.launch

class KullanicilarFragment : Fragment() {

    private var _binding: FragmentKullanicilarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentKullanicilarBinding.inflate(inflater, container, false)
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
            when (val sonuc = repo.kullanicilarGetir()) {
                is Resource.Basarili -> {
                    binding.listKullanicilar.removeAllViews()
                    sonuc.data.forEach { k ->
                        val satir = ItemKullaniciBinding.inflate(layoutInflater, binding.listKullanicilar, false)
                        satir.txtAdSoyad.text = k.adSoyad
                        satir.txtDetay.text = "@${k.kullaniciAdi} • ${if (k.rol == "admin") "Yönetici" else "Kullanıcı"}"
                        binding.listKullanicilar.addView(satir.root)
                    }
                }
                is Resource.Hata -> if (isAdded) Snackbar.make(binding.root, sonuc.mesaj, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun eklemeDialoguGoster() {
        val dialogBinding = DialogKullaniciEkleBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yeni Kullanıcı")
            .setView(dialogBinding.root)
            .setPositiveButton("Ekle", null)
            .setNegativeButton("Vazgeç", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.Dialog.BUTTON_POSITIVE).setOnClickListener {
                        val adSoyad = dialogBinding.editAdSoyad.text?.toString()?.trim().orEmpty()
                        val kullaniciAdi = dialogBinding.editKullaniciAdi.text?.toString()?.trim().orEmpty()
                        val sifre = dialogBinding.editSifre.text?.toString().orEmpty()
                        val rol = if (dialogBinding.checkAdmin.isChecked) "admin" else "user"

                        if (adSoyad.isEmpty() || kullaniciAdi.isEmpty() || sifre.length < 6) {
                            Snackbar.make(binding.root, "Ad soyad, kullanıcı adı zorunlu; şifre en az 6 karakter olmalı.", Snackbar.LENGTH_LONG).show()
                            return@setOnClickListener
                        }

                        val repo = ServiceLocator.stokRepository(requireContext())
                        viewLifecycleOwner.lifecycleScope.launch {
                            when (val sonuc = repo.kullaniciEkle(adSoyad, kullaniciAdi, sifre, rol)) {
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
