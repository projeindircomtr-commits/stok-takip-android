package com.salman.stoktakip.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.databinding.ActivityMainBinding
import com.salman.stoktakip.ui.arac.AraclarFragment
import com.salman.stoktakip.ui.depo.DepoFragment
import com.salman.stoktakip.ui.login.LoginActivity
import com.salman.stoktakip.ui.malzeme.MalzemelerFragment
import com.salman.stoktakip.ui.profil.ProfilFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = ServiceLocator.session(this)
        if (!session.girisYapilmisMi) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, MalzemelerFragment())
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragman = when (item.itemId) {
                com.salman.stoktakip.R.id.nav_malzemeler -> MalzemelerFragment()
                com.salman.stoktakip.R.id.nav_araclar -> AraclarFragment()
                com.salman.stoktakip.R.id.nav_depo -> DepoFragment()
                com.salman.stoktakip.R.id.nav_profil -> ProfilFragment()
                else -> null
            }
            if (fragman != null) {
                supportFragmentManager.beginTransaction()
                    .replace(binding.fragmentContainer.id, fragman)
                    .commit()
                true
            } else false
        }

        // Acilista sunucudan taze veri cek (offline ise sessizce onbellek kalir).
        lifecycleScope.launch {
            ServiceLocator.stokRepository(this@MainActivity).tumVeriyiYenile()
        }
    }
}
