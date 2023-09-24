package com.example.family21

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.family21.databinding.ActivityCharacteristicsBinding
import data.InfoData
import java.io.File

class Characteristics : AppCompatActivity() {

    private lateinit var binding: ActivityCharacteristicsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacteristicsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val result = intent.getStringExtra("result")
        findDetail(result)
    }

    override fun onBackPressed() {
        finish()
    }

    private fun findDetail(detailArt: String?) {
        for (vendor in InfoData.values()) {
            if (vendor.art == detailArt) {
                if (vendor.figure == null) {
                    runOnUiThread {
                        binding.figureTitle.text = "ЧЕРТЕЖ НЕ НАЙДЕН"
                        binding.figureTitle.setTextColor(ContextCompat.getColor(this, R.color.black))
                    }
                } else {
                    binding.figureTitle.setOnClickListener {
                        openPdfFile(vendor.figure)
                    }
                }
                runOnUiThread {
                    binding.art.text = vendor.art
                    binding.name.text = vendor.nameDetail ?: "нет данных"
                    binding.size.text = vendor.size ?: "нет данных"
                }
            }
        }

    }

    private fun openPdfFile(file: String) {
        val modelPath = File(this.filesDir, file).absolutePath
        val pdfIntent = Intent(Intent.ACTION_VIEW)
        pdfIntent.setDataAndType(Uri.parse(modelPath), "application/pdf")
        pdfIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        try {
            startActivity(pdfIntent)
        } catch (_: ActivityNotFoundException) {}
    }
}