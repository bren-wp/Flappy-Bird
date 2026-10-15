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
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val qaWorld = if (debuggable) intent.getIntExtra("sky_peep_qa_world", 0) else 0
        val qaSkin = if (debuggable) intent.getIntExtra("sky_peep_qa_skin", -1) else -1
        val qaVictory = if (debuggable) intent.getBooleanExtra("sky_peep_qa_victory", false) else false
        val qaPillars = if (debuggable) intent.getBooleanExtra("sky_peep_qa_pillars", false) else false
        initialize(SkyPeepGame(shareText = { result ->
            runOnUiThread {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, result)
                }
                startActivity(Intent.createChooser(intent, "Share SkyPeep score"))
            }
        }, qaWorld = qaWorld, qaSkin = qaSkin,
            qaVictory = qaVictory, qaPillars = qaPillars), config)
    }
}
