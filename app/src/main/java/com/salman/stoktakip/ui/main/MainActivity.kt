package com.salman.stoktakip.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.salman.stoktakip.data.ServiceLocator
import com.salman.stoktakip.databinding.ActivityMainBinding
import com.salman.stoktakip.databinding.ItemDrawerMenuBinding
import com.salman.stoktakip.ui.arac.AraclarFragment
import com.salman.stoktakip.ui.dashboard.DashboardFragment
import com.salman.stoktakip.ui.depo.DepoFragment
import com.salman.stoktakip.ui.kullanici.KullanicilarFragment
import com.salman.stoktakip.ui.login.LoginActivity
import com.salman.stoktakip.ui.malzeme.MalzemelerFragment
import com.salman.stoktakip.ui.raporlar.RaporlarFragment
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
            goster(DashboardFragment())
        }

        val menuOgeleri = mutableListOf(
            Triple("Ana Sayfa", com.salman.stoktakip.R.drawable.ic_home) { goster(DashboardFragment()) },
            Triple("Malzemeler", com.salman.stoktakip.R.drawable.ic_box) { goster(MalzemelerFragment()) },
            Triple("Malzeme Ekle", com.salman.stoktakip.R.drawable.ic_box) { goster(MalzemelerFragment.yeniKayitIle()) },
            Triple("Araçlar", com.salman.stoktakip.R.drawable.ic_truck) { goster(AraclarFragment()) },
            Triple("Araç Ekle", com.salman.stoktakip.R.drawable.ic_truck) { goster(AraclarFragment.yeniKayitIle()) },
            Triple("Arızalı Araçlar", com.salman.stoktakip.R.drawable.ic_wrench) { goster(com.salman.stoktakip.ui.ariza.ArizalarFragment()) },
            Triple("Yakıt Takip", com.salman.stoktakip.R.drawable.ic_fuel) { goster(com.salman.stoktakip.ui.yakit.YakitFragment()) },
            Triple("Kategoriler / Lokasyon", com.salman.stoktakip.R.drawable.ic_pin) { goster(DepoFragment()) },
            Triple("Raporlar", com.salman.stoktakip.R.drawable.ic_chart) { goster(RaporlarFragment()) }
        )
        if (session.adminMi) {
            menuOgeleri.add(Triple("Kullanıcılar", com.salman.stoktakip.R.drawable.ic_users) { goster(KullanicilarFragment()) })
        }

        menuOgeleri.forEach { (baslik, ikon, aksiyon) ->
            val satir = ItemDrawerMenuBinding.inflate(layoutInflater, binding.navMenuItems, false)
            satir.txtBaslik.text = baslik
            satir.imgIkon.setImageResource(ikon)
            satir.root.setOnClickListener {
                aksiyon()
                binding.drawerLayout.closeDrawers()
            }
            binding.navMenuItems.addView(satir.root)
        }

        binding.btnDrawerCikis.setOnClickListener {
            lifecycleScope.launch {
                ServiceLocator.authRepository(this@MainActivity).cikisYap()
                startActivity(
                    Intent(this@MainActivity, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }
        }

        val repo = ServiceLocator.stokRepository(this)
        lifecycleScope.launch {
            repo.bekleyenSayiGozlemle().collect { sayi ->
                binding.txtBekleyenDrawer.text = "Bekleyen senkron: $sayi"
            }
        }

        lifecycleScope.launch {
            repo.tumVeriyiYenile()
        }
    }

    private fun goster(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    /** Fragmanlar hamburger ikonuna basildiginda bunu cagirir. */
    fun cekmeceyiAc() {
        binding.drawerLayout.openDrawer(Gravity.START)
    }
}
