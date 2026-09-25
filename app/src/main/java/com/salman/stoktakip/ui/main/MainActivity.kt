package com.salman.stoktakip.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import androidx.activity.OnBackPressedCallback
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

    private val bildirimIzniIste = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* kullanici ne secerse secsin devam - reddederse sadece uygulama-ici banner calisir */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bildirimIzniIste.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val session = ServiceLocator.session(this)
        if (!session.girisYapilmisMi) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (savedInstanceState == null) {
            goster(DashboardFragment(), anaSayfaMi = true)
        }

        // Sistem "geri" tusu/hareketi: once acik olan cekmeceyi kapat,
        // sonra ekran gecmisinde geri git, en sonda Ana Sayfa'daysak
        // uygulamayi normal sekilde kapat.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.drawerLayout.isDrawerOpen(Gravity.START) -> binding.drawerLayout.closeDrawers()
                    supportFragmentManager.backStackEntryCount > 0 -> supportFragmentManager.popBackStack()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        val menuOgeleri = mutableListOf(
            Triple("Ana Sayfa", com.salman.stoktakip.R.drawable.ic_home) { gosterAnaSayfayaDon() },
            Triple("Malzemeler", com.salman.stoktakip.R.drawable.ic_box) { goster(MalzemelerFragment()) },
            Triple("Malzeme Ekle", com.salman.stoktakip.R.drawable.ic_box) { goster(MalzemelerFragment.yeniKayitIle()) },
            Triple("Araçlar", com.salman.stoktakip.R.drawable.ic_truck) { goster(AraclarFragment()) },
            Triple("Araç Ekle", com.salman.stoktakip.R.drawable.ic_truck) { goster(AraclarFragment.yeniKayitIle()) },
            Triple("Yakıt Takip", com.salman.stoktakip.R.drawable.ic_fuel) { goster(com.salman.stoktakip.ui.yakit.YakitFragment()) },
            Triple("Yemek Takip", com.salman.stoktakip.R.drawable.ic_food) { goster(com.salman.stoktakip.ui.yemek.YemekFragment()) },
            Triple("Hava Durumu (39 İlçe)", com.salman.stoktakip.R.drawable.ic_snow) { goster(com.salman.stoktakip.ui.hava.HavaDurumuFragment()) },
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
                binding.txtBekleyenDrawer.text = "Güncellenecek stok: $sayi"
            }
        }

        lifecycleScope.launch {
            repo.tumVeriyiYenile()
        }
    }

    /**
     * Ekran degistirir. Ana Sayfa disindaki her ekran geri yigina (back stack)
     * eklenir, boylece telefonun geri tusu/hareketi bir onceki ekrana doner -
     * direkt uygulamadan cikmaz.
     */
    private fun goster(fragment: Fragment, anaSayfaMi: Boolean = false) {
        val islem = supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
        if (!anaSayfaMi) {
            islem.addToBackStack(null)
        }
        islem.commit()
    }

    /** Drawer'daki "Ana Sayfa" tum gecmisi temizleyip koke doner. */
    private fun gosterAnaSayfayaDon() {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, DashboardFragment())
            .commit()
    }

    /** Fragmanlar hamburger ikonuna basildiginda bunu cagirir. */
    fun cekmeceyiAc() {
        binding.drawerLayout.openDrawer(Gravity.START)
    }
}
