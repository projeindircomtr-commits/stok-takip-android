package com.salman.stoktakip.ui.profil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.databinding.FragmentProfilBinding
import com.salman.stoktakip.ui.login.LoginActivity
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = ServiceLocator.session(requireContext())
        val repo = ServiceLocator.stokRepository(requireContext())

        binding.txtAdSoyad.text = session.adSoyad ?: session.kullaniciAdi
        binding.txtRol.text = if (session.adminMi) "Yönetici" else "Kullanıcı"

        viewLifecycleOwner.lifecycleScope.launch {
            repo.bekleyenSayiGozlemle().collect { sayi ->
                binding.txtBekleyen.text = if (sayi > 0) "$sayi kayıt senkronizasyon bekliyor" else ""
            }
        }

        binding.btnSenkronize.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val sonuc = repo.bekleyenleriSenkronize()
                repo.tumVeriyiYenile()
                val mesaj = when (sonuc) {
                    is Resource.Basarili -> "Senkronizasyon tamamlandı."
                    is Resource.Hata -> sonuc.mesaj
                    else -> ""
                }
                if (mesaj.isNotEmpty()) Snackbar.make(binding.root, mesaj, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.btnCikis.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Çıkış Yap")
                .setMessage("Oturumu kapatmak istediğinize emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        ServiceLocator.authRepository(requireContext()).cikisYap()
                        startActivity(
                            Intent(requireContext(), LoginActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        requireActivity().finish()
                    }
                }
                .setNegativeButton("Hayır", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
