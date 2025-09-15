package com.nkjayanet

import android.content.res.AssetManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private var process: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)

        // Copy php7 binary (rename di assets jadi 'php7', bukan folder)
        val phpBinary = File(filesDir, "php7")
        if (!phpBinary.exists()) {
            assets.open("php7").use { input ->
                FileOutputStream(phpBinary).use { output -> input.copyTo(output) }
            }
            phpBinary.setExecutable(true)
        }

        // Copy www folder (rekursif)
        val wwwDir = File(filesDir, "www")
        if (!wwwDir.exists()) {
            copyAssetFolder(assets, "www", wwwDir.absolutePath)
        }

        // Copy php.ini (optional)
        val phpIni = File(filesDir, "php.ini")
        if (!phpIni.exists()) {
            try {
                assets.open("php.ini").use { input ->
                    FileOutputStream(phpIni).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                // Boleh diabaikan jika tidak ada php.ini di assets
            }
        }

        // Buat tmp folder untuk session/upload jika belum ada
        val tmpDir = File(filesDir, "tmp")
        if (!tmpDir.exists()) tmpDir.mkdirs()

        findViewById<Button>(R.id.btnStart).setOnClickListener { startWebServer() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopWebServer() }
    }

    private fun startWebServer() {
        if (process != null) {
            statusText.text = "Server sudah jalan di http://127.0.0.1:8080/"
            return
        }
        val phpBinary = File(filesDir, "php7")
        val wwwDir = File(filesDir, "www")
        val phpIni = File(filesDir, "php.ini")
        val tmpDir = File(filesDir, "tmp")
        if (!tmpDir.exists()) tmpDir.mkdirs()

        try {
            val cmd = if (phpIni.exists()) {
                arrayOf(phpBinary.absolutePath, "-c", phpIni.absolutePath, "-S", "127.0.0.1:8080", "-t", wwwDir.absolutePath)
            } else {
                arrayOf(phpBinary.absolutePath, "-S", "127.0.0.1:8080", "-t", wwwDir.absolutePath)
            }
            process = Runtime.getRuntime().exec(cmd)
            statusText.text = "PHP Webserver aktif di http://127.0.0.1:8080/"
        } catch (e: Exception) {
            statusText.text = "Error: ${e.message}"
        }
    }

    private fun stopWebServer() {
        process?.destroy()
        process = null
        statusText.text = "Server dimatikan"
    }

    private fun copyAssetFolder(assetManager: AssetManager, fromAssetPath: String, toPath: String): Boolean {
        try {
            val files = assetManager.list(fromAssetPath)
            if (files.isNullOrEmpty()) {
                copyAsset(assetManager, fromAssetPath, toPath)
            } else {
                val dir = File(toPath)
                if (!dir.exists()) dir.mkdirs()
                for (file in files) {
                    copyAssetFolder(assetManager, "$fromAssetPath/$file", "$toPath/$file")
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun copyAsset(assetManager: AssetManager, fromAssetPath: String, toPath: String) {
        assetManager.open(fromAssetPath).use { input ->
            FileOutputStream(File(toPath)).use { out ->
                input.copyTo(out)
            }
        }
    }
}