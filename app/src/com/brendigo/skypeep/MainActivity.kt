package com.brendigo.skypeep

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class MainActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply { useImmersiveMode = true }
        // QA worlds are addressable ONLY in debuggable APKs. Production
        // releases cannot bypass level progression through an Intent.
        val qaWorld = if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            intent.getIntExtra("sky_peep_qa_world", 0) else 0
        initialize(SkyPeepGame(shareText = { result ->
            runOnUiThread {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, result)
                }
                startActivity(Intent.createChooser(intent, "Share SkyPeep score"))
            }
        }, qaWorld = qaWorld), config)
    }
}
