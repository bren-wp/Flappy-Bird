package com.brendigo.skypeep

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
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
        val qaScore = if (debuggable) intent.getIntExtra("sky_peep_qa_score", -1) else -1
        val qaMapUnlocked = if (debuggable) intent.getIntExtra("sky_peep_qa_map_unlocked", -1) else -1
        val qaLoss = if (debuggable) intent.getBooleanExtra("sky_peep_qa_loss", false) else false
        initialize(SkyPeepGame(shareText = { result ->
            runOnUiThread {
                // Android may destroy the activity while the system chooser opens.
                if (isFinishing || isDestroyed) return@runOnUiThread
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, result)
                }
                try {
                    startActivity(Intent.createChooser(shareIntent, "Share SkyPeep score"))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this, "No sharing app available", Toast.LENGTH_SHORT).show()
                }
            }
        }, qaWorld = qaWorld, qaSkin = qaSkin,
            qaVictory = qaVictory, qaPillars = qaPillars,
            qaScore = qaScore, qaMapUnlocked = qaMapUnlocked,
            qaLoss = qaLoss), config)
    }
}
