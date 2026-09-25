package com.salman.stoktakip.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Liste ekranlarindaki "Excel (CSV)" ve "PDF / Yazdir" butonlari icin
 * basit disa aktarma yardimcisi. Uretilen dosya once uygulamanin cache
 * klasorune yazilir, sonra sistem paylasim menusuyle (WhatsApp, Drive,
 * dosya yoneticisi vb.) kullaniciya sunulur.
 */
object ExportYardimcisi {

    fun csvPaylas(context: Context, dosyaAdi: String, basliklar: List<String>, satirlar: List<List<String>>) {
        val dosya = File(context.cacheDir, "$dosyaAdi.csv")
        FileOutputStream(dosya).use { out ->
            out.write(0xEF); out.write(0xBB); out.write(0xBF) // UTF-8 BOM, Excel Turkce karakterleri dogru gostersin
            out.write((basliklar.joinToString(";") { it.temizle() } + "\n").toByteArray(Charsets.UTF_8))
            satirlar.forEach { satir ->
                out.write((satir.joinToString(";") { it.temizle() } + "\n").toByteArray(Charsets.UTF_8))
            }
        }
        paylas(context, dosya, "text/csv")
    }

    fun pdfPaylas(context: Context, dosyaAdi: String, baslik: String, basliklar: List<String>, satirlar: List<List<String>>) {
        val document = PdfDocument()
        val genislik = 842 // A4 yatay, punto
        val yukseklik = 595
        var sayfaNo = 1
        var pageInfo = PdfDocument.PageInfo.Builder(genislik, yukseklik, sayfaNo).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val baslikPaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val hucrePaint = Paint().apply { textSize = 10f }

        val solMargin = 24f
        val ustMargin = 36f
        var y = ustMargin

        canvas.drawText(baslik, solMargin, y, baslikPaint)
        y += 26f

        val sutunGenisligi = (genislik - 2 * solMargin) / basliklar.size
        fun basliklariCiz() {
            var x = solMargin
            basliklar.forEach { h ->
                canvas.drawText(h, x, y, headerPaint)
                x += sutunGenisligi
            }
            y += 18f
        }
        basliklariCiz()

        satirlar.forEach { satir ->
            if (y > yukseklik - 40) {
                document.finishPage(page)
                sayfaNo++
                pageInfo = PdfDocument.PageInfo.Builder(genislik, yukseklik, sayfaNo).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = ustMargin
                basliklariCiz()
            }
            var x = solMargin
            satir.forEach { hucre ->
                canvas.drawText(hucre.take(28), x, y, hucrePaint)
                x += sutunGenisligi
            }
            y += 16f
        }
        document.finishPage(page)

        val dosya = File(context.cacheDir, "$dosyaAdi.pdf")
        FileOutputStream(dosya).use { document.writeTo(it) }
        document.close()

        paylas(context, dosya, "application/pdf")
    }

    private fun paylas(context: Context, dosya: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dosya)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Paylaş / Kaydet"))
    }

    private fun String.temizle(): String = this.replace(";", ",").replace("\n", " ")
}
