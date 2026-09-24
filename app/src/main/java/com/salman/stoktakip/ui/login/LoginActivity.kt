package com.salman.stoktakip.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.data.SessionManager
import com.salman.stoktakip.databinding.ActivityLoginBinding
import com.salman.stoktakip.ui.main.MainActivity
import com.salman.stoktakip.util.Resource
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = ServiceLocator.session(this)

        if (session.girisYapilmisMi) {
            anaEkraneGit()
            return
        }

        binding.btnGiris.setOnClickListener { girisDeneme() }
        binding.editSifre.setOnEditorActionListener { _, _, _ -> girisDeneme(); true }
    }

    private fun girisDeneme() {
        val kullaniciAdi = binding.editKullaniciAdi.text?.toString()?.trim().orEmpty()
        val sifre = binding.editSifre.text?.toString().orEmpty()

        if (kullaniciAdi.isEmpty() || sifre.isEmpty()) {
            hataGoster("Kullanıcı adı ve şifre zorunludur.")
            return
        }

        yukleniyorGoster(true)
        lifecycleScope.launch {
            val sonuc = ServiceLocator.authRepository(this@LoginActivity).girisYap(kullaniciAdi, sifre)
            yukleniyorGoster(false)
            when (sonuc) {
                is Resource.Basarili -> anaEkraneGit()
                is Resource.Hata -> hataGoster(sonuc.mesaj)
                else -> {}
            }
        }
    }

    private fun anaEkraneGit() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun hataGoster(mesaj: String) {
        binding.txtHata.text = mesaj
        binding.txtHata.visibility = android.view.View.VISIBLE
    }

    private fun yukleniyorGoster(yukleniyor: Boolean) {
        binding.progressGiris.visibility = if (yukleniyor) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnGiris.isEnabled = !yukleniyor
        binding.txtHata.visibility = android.view.View.GONE
    }
}
