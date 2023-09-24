package com.example.family21

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.family21.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var module: Module
    private lateinit var labels: List<String>
    private var isStarted: Boolean = true
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissions()
        loadModule()
        labels = loadLabels()
        binding.resultText.setOnClickListener {
            if (binding.resultText.text != "СТАРТ") {
                val intent = Intent(this, Characteristics::class.java)
                intent.putExtra("result", binding.resultText.text)
                startActivity(intent)
            } else {
                isStarted = false
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED)  {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 101)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                        .also {
                            it.setSurfaceProvider(binding.cameraView.surfaceProvider)
                        }
                    val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                    val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(224, 224))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    imageAnalysis.setAnalyzer(executor, object : ImageAnalysis.Analyzer {
                        override fun analyze(image: ImageProxy) {
                            val rotation = image.imageInfo.rotationDegrees
                            analyzeImage(image, rotation)
                            image.close()
                        }
                    })
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                } catch (_: Exception) {}
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun loadModule() {
        val modelFile = File(this.filesDir, "model.ptl")
        val inputStream = assets.open("model.ptl")
        val outputStream = FileOutputStream(modelFile)
        val buffer = ByteArray(2084)
        var byteRead: Int
        while (run {
                byteRead = inputStream.read(buffer)
                byteRead
            } != -1) {
            outputStream.write(buffer, 0, byteRead)
        }
        inputStream.close()
        outputStream.close()
        module = LiteModuleLoader.load(modelFile.absolutePath)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyzeImage(image: ImageProxy, rotation: Int) {
        val inputTensor =
            TensorImageUtils.imageYUV420CenterCropToFloat32Tensor(
                image.image,
                rotation,
                224,
                224,
                FloatArray(3) {0f},
                FloatArray(3) {1f}
            )
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
        val scores = outputTensor.dataAsFloatArray
        var maxScore = -Float.MAX_VALUE
        var maxScoreIndex = -1
        for (id in scores.indices) {
            if (scores[id] > maxScore) {
                maxScore = scores[id]
                maxScoreIndex = id
            }
        }
        val result = labels[maxScoreIndex]
        if (!isStarted) {
            runOnUiThread {
                binding.resultText.text = result
            }
        }

    }

    private fun loadLabels(): List<String> {
        val buffer = BufferedReader(InputStreamReader(assets.open("labels.txt"))).readLine()
        return buffer.split(' ')
    }

}