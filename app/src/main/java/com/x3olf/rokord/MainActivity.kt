package com.x3olf.rokord

import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private fun getLocalIp(): String {
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val ip = Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
        return if (ip == "0.0.0.0") "" else ip
    }

    private fun isSameSubnet(a: String, b: String): Boolean {
        val pA = a.split(".")
        val pB = b.split(".")
        return pA.size == 4 && pB.size == 4 && pA[0] == pB[0] && pA[1] == pB[1] && pA[2] == pB[2]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rokuIpInput = findViewById<EditText>(R.id.rokuIpInput)
        val tokenInput = findViewById<EditText>(R.id.tokenInput)
        val pairButton = findViewById<Button>(R.id.pairButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        pairButton.setOnClickListener {
            val phoneIp = getLocalIp()
            val rokuIp = rokuIpInput.text.toString().trim()
            val token = tokenInput.text.toString().trim()

            if (token.isEmpty()) {
                statusText.text = "Status: Enter your login token in the mobile app first."
                return@setOnClickListener
            }
            if (phoneIp.isEmpty() || rokuIp.isEmpty() || !isSameSubnet(phoneIp, rokuIp)) {
                statusText.text = "Blocked: Must be on the SAME network connection (Phone: $phoneIp, Roku: $rokuIp)."
                return@setOnClickListener
            }

            statusText.text = "Status: Same connection verified ($phoneIp -> $rokuIp). Sending pair request..."
            thread {
                try {
                    val url = URL("http://$rokuIp:8060/launch/dev?token=${java.net.URLEncoder.encode(token, "UTF-8")}")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 5000
                        readTimeout = 5000
                        doOutput = true
                    }
                    OutputStreamWriter(conn.outputStream).use { it.write("") }
                    val code = conn.responseCode
                    conn.disconnect()
                    runOnUiThread {
                        statusText.text = "Paired over same connection ($phoneIp -> $rokuIp), response: $code"
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        statusText.text = "Pairing error on same connection: ${e.message}"
                    }
                }
            }
        }
    }
}
