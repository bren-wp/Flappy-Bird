package com.brendigo.skypeep

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlin.math.abs
import kotlin.math.sin

/** Self-contained SkyPeep 2D renderer and flight game. Does not depend on missing texture files. */
class SkyPeepGame(private val share: ((String) -> Unit)? = null) : ApplicationAdapter() {
    private enum class Page { SPLASH, MENU, LEVELS, PLAY, PAUSE, RESULTS, SKINS, SETTINGS }
    private data class Gate(var x: Float, val y: Float, var passed: Boolean = false, var picked: Boolean = false)
    private data class Tap(val x: Float, val y: Float, val w: Float, val h: Float, val action: () -> Unit) {
        fun contains(v: Vector2) = v.x >= x && v.x <= x + w && v.y >= y && v.y <= y + h
    }
    private data class Words(val value: String, val x: Float, val y: Float, val scale: Float, val color: Color)
    private data class World(val name: String, val sky: Color, val column: Color, val accent: Color)
    private val worlds = listOf(
        World("SKY START", Color.valueOf("37AEEF"), Color.valueOf("F3E3C2"), Color.valueOf("7BCC37")),
        World("CLOUD PASS", Color.valueOf("5ABCF4"), Color.valueOf("FCF2D9"), Color.valueOf("FBBF3A")),
        World("CRYSTAL DROP", Color.valueOf("2D8EE1"), Color.valueOf("3A5589"), Color.valueOf("47DAFA")),
        World("WIND GATE", Color.valueOf("5CBDE6"), Color.valueOf("DAD8D1"), Color.valueOf("4A8AD0")),
        World("MOON GLOW", Color.valueOf("283894"), Color.valueOf("47397E"), Color.valueOf("AA70E6")),
        World("SUN PEAK", Color.valueOf("F4A05C"), Color.valueOf("F7E2B8"), Color.valueOf("F8BC33"))
    )
    private val names = arrayOf("DEFAULT", "CAPTAIN", "FOREST", "COSMIC", "ROYAL")
    private val colors = arrayOf("24C2E2", "2D91D2", "54BD45", "9B67E7", "13B2D6")
    private val prices = intArrayOf(0, 500, 800, 120, 1000)
    private val areas = mutableListOf<Tap>()
    private val textItems = mutableListOf<Words>()
    private val gates = mutableListOf<Gate>()
    private val owned = BooleanArray(5)
    private val stars = IntArray(6)
    private val width = 540f
    private val height = 960f
    private val birdX = 145f
    private val birdRadius = 20f
    private val gateWidth = 75f
    private lateinit var renderer: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var preferences: Preferences
    private val camera = OrthographicCamera()
    private lateinit var viewport: FitViewport
    private val pointer = Vector2()
    private var page = Page.SPLASH
    private var world = 1
    private var unlocked = 1
    private var points = 0
    private var best = 0
    private var coins = 1250
    private var gems = 75
    private var runCoins = 0
    private var currentSkin = 0
    private var soundEnabled = true
    private var musicEnabled = true
    private var playerY = 480f
    private var velocity = 0f
    private var elapsed = 0f
    private var spawnIn = 1f
    private var isStarted = false
    private var won = false
    private var notification = ""
    private var noticeFor = 0f

    override fun create() {
        renderer = ShapeRenderer()
        batch = SpriteBatch()
        font = BitmapFont()
        viewport = FitViewport(width, height, camera)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        preferences = Gdx.app.getPreferences("SkyPeepProgressV1")
        best = preferences.getInteger("best", 0)
        unlocked = preferences.getInteger("unlocked", 1).coerceIn(1, 6)
        world = preferences.getInteger("world", 1).coerceIn(1, unlocked)
        coins = preferences.getInteger("coins", 1250).coerceAtLeast(0)
        gems = preferences.getInteger("gems", 75).coerceAtLeast(0)
        currentSkin = preferences.getInteger("skin", 0).coerceIn(0, 4)
        soundEnabled = preferences.getBoolean("sound", true)
        musicEnabled = preferences.getBoolean("music", true)
        owned[0] = true
        for (i in 1..4) owned[i] = preferences.getBoolean("owned_" + i, false)
        if (!owned[currentSkin]) currentSkin = 0
        for (i in 0..5) stars[i] = preferences.getInteger("stars_" + i, 0).coerceIn(0, 3)
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true)
    }

    override fun resize(w: Int, h: Int) { viewport.update(w, h, true) }

    override fun render() {
        val dt = Gdx.graphics.deltaTime.coerceIn(0f, 1f / 30f)
        elapsed += dt
        noticeFor = (noticeFor - dt).coerceAtLeast(0f)
        if (page == Page.PLAY) update(dt)
        viewport.apply()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        renderer.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined
        areas.clear()
        textItems.clear()
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        background()
        when (page) {
            Page.SPLASH -> splash()
            Page.MENU -> menu()
            Page.LEVELS -> levels()
            Page.PLAY -> gameplay(true)
            Page.PAUSE -> { gameplay(false); pause() }
            Page.RESULTS -> results()
            Page.SKINS -> skins()
            Page.SETTINGS -> settings()
        }
        if (noticeFor > 0) {
            panel(78f, 809f, 384f, 58f, Color.valueOf("183A61"))
            label(notification, 270f, 839f, 1.4f)
        }
        renderer.end()
        batch.begin()
        for (item in textItems) {
            font.data.setScale(item.scale)
            font.color = item.color
            val measurement = GlyphLayout(font, item.value)
            font.draw(batch, item.value, item.x - measurement.width / 2f, item.y + measurement.height / 2f)
        }
        batch.end()
        input()
    }

    private fun input() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            page = when (page) {
                Page.PLAY -> Page.PAUSE
                Page.PAUSE -> Page.PLAY
                else -> Page.MENU
            }
            return
        }
        if (!Gdx.input.justTouched()) return
        pointer.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(pointer)
        if (pointer.x !in 0f..width || pointer.y !in 0f..height) return
        val target = areas.asReversed().firstOrNull { it.contains(pointer) }
        when {
            target != null -> target.action()
            page == Page.SPLASH -> page = Page.MENU
            page == Page.PLAY -> { isStarted = true; velocity = 360f }
            else -> Unit
        }
    }

    private fun startGame(target: Int) {
        world = target.coerceIn(1, unlocked)
        page = Page.PLAY
        playerY = 480f
        velocity = 0f
        points = 0
        runCoins = 0
        isStarted = false
        won = false
        spawnIn = 0.8f
        gates.clear()
        save()
    }

    private fun update(dt: Float) {
        if (!isStarted) return
        velocity -= (910f + world * 12f) * dt
        playerY += velocity * dt
        spawnIn -= dt
        val speed = 174f + world * 12f
        if (spawnIn <= 0f) {
            gates += Gate(width + 45f, MathUtils.random(280f, 680f))
            spawnIn = (2.10f - world * 0.075f).coerceAtLeast(1.5f)
        }
        val gap = 295f - world * 11f
        for (gate in gates) {
            gate.x -= speed * dt
            if (!gate.passed && gate.x + gateWidth < birdX) {
                gate.passed = true
                points++
                if (points >= 10 + world * 4) { endGame(true); return }
            }
            if (!gate.picked && abs(gate.x + gateWidth / 2f - birdX) < 26f && abs(playerY - gate.y) < 42f) {
                gate.picked = true
                runCoins++
            }
            if (hitColumn(gate.x, 0f, gate.y - gap / 2f) ||
                hitColumn(gate.x, gate.y + gap / 2f, height - gate.y - gap / 2f)) {
                endGame(false)
                return
            }
        }
        gates.removeAll { it.x < -gateWidth - 20f }
        if (playerY < birdRadius || playerY > height - birdRadius) endGame(false)
    }

    private fun hitColumn(x: Float, y: Float, h: Float): Boolean {
        if (h <= 0f) return false
        val bx = birdX.coerceIn(x, x + gateWidth)
        val by = playerY.coerceIn(y, y + h)
        val dx = birdX - bx
        val dy = playerY - by
        return dx * dx + dy * dy < birdRadius * birdRadius
    }

    private fun endGame(success: Boolean) {
        won = success
        isStarted = false
        page = Page.RESULTS
        best = maxOf(best, points)
        coins += points + runCoins
        if (success) {
            gems += 2
            stars[world - 1] = maxOf(stars[world - 1], if (runCoins >= 3) 3 else 2)
            unlocked = maxOf(unlocked, (world + 1).coerceAtMost(6))
        }
        save()
    }

    private fun save() {
        preferences.putInteger("best", best)
        preferences.putInteger("unlocked", unlocked)
        preferences.putInteger("world", world)
        preferences.putInteger("coins", coins)
        preferences.putInteger("gems", gems)
        preferences.putInteger("skin", currentSkin)
        preferences.putBoolean("music", musicEnabled)
        preferences.putBoolean("sound", soundEnabled)
        for (i in 1..4) preferences.putBoolean("owned_" + i, owned[i])
        for (i in 0..5) preferences.putInteger("stars_" + i, stars[i])
        preferences.flush()
    }

    private fun notifyUser(value: String) { notification = value; noticeFor = 1.8f }
    private fun label(value: String, x: Float, y: Float, scale: Float = 1.9f, color: Color = Color.WHITE) {
        textItems += Words(value, x, y, scale, color)
    }

    private fun panel(x: Float, y: Float, w: Float, h: Float, color: Color) {
        renderer.color = Color.valueOf("163151")
        renderer.rect(x - 3f, y - 4f, w + 6f, h + 8f)
        renderer.color = color
        renderer.rect(x, y, w, h)
    }

    private fun action(value: String, x: Float, y: Float, w: Float, h: Float, color: Color, scale: Float = 1.9f, run: () -> Unit) {
        panel(x, y, w, h, color)
        label(value, x + w / 2f, y + h / 2f, scale)
        areas += Tap(x, y, w, h, run)
    }

    private fun cloud(x: Float, y: Float, s: Float) {
        renderer.color = Color(1f, 1f, 1f, 0.8f)
        renderer.ellipse(x - 53f * s, y - 14f * s, 114f * s, 37f * s)
        renderer.circle(x - 11f * s, y + 8f * s, 28f * s, 20)
        renderer.circle(x + 28f * s, y + 7f * s, 31f * s, 20)
    }

    private fun background() {
        val t = worlds[world - 1]
        renderer.color = if (page == Page.PLAY || page == Page.PAUSE || page == Page.RESULTS) t.sky else worlds[0].sky
        renderer.rect(0f, 0f, width, height)
        if (world == 5 && page == Page.PLAY) {
            renderer.color = Color.valueOf("FFE9AD")
            renderer.circle(114f, 765f, 66f, 40)
            renderer.color = t.sky
            renderer.circle(137f, 786f, 60f, 40)
        } else if (world == 6 && page == Page.PLAY) {
            renderer.color = Color.valueOf("FFEEA1")
            renderer.circle(426f, 745f, 92f, 40)
        }
        for (i in 0..11) {
            val x = ((i * 183f + 40f - elapsed * (8f + i % 3 * 4f)) % 800f + 800f) % 800f - 120f
            cloud(x, (135 + i * 157 % 670).toFloat(), if (i % 3 == 0) 1.2f else .7f)
        }
        for (i in 0..7) {
            val x = ((i * 137f + 32f - elapsed * 5f) % 800f + 800f) % 800f - 90f
            val y = (95 + i * 127 % 460).toFloat()
            renderer.color = Color.valueOf("81719C")
            renderer.triangle(x - 40f, y, x + 50f, y, x + 3f, y - 78f)
            renderer.color = Color.valueOf("76CD50")
            renderer.ellipse(x - 42f, y - 5f, 91f, 19f)
        }
        cloud(45f, 0f, 1.5f)
        cloud(480f, -8f, 1.7f)
    }

    private fun bird(x: Float, y: Float, s: Float) {
        val wingY = (sin(elapsed * 14f) * 7f * s)
        renderer.color = Color.valueOf("EF4747")
        renderer.triangle(x-19f*s, y, x-61f*s, y+16f*s, x-46f*s, y-8f*s)
        renderer.color = Color.valueOf("153149")
        renderer.ellipse(x-33f*s, y-24f*s, 70f*s, 54f*s)
        renderer.color = Color.valueOf(colors[currentSkin])
        renderer.ellipse(x-29f*s, y-21f*s, 62f*s, 47f*s)
        renderer.color = Color.valueOf("FFF2CD")
        renderer.ellipse(x-8f*s, y-19f*s, 39f*s, 19f*s)
        renderer.color = Color.valueOf("0B4673")
        renderer.ellipse(x-13f*s, y-18f*s+wingY, 33f*s, 28f*s)
        renderer.color = Color.valueOf(colors[currentSkin])
        renderer.ellipse(x-11f*s, y-15f*s+wingY, 28f*s, 22f*s)
        renderer.color = Color.WHITE
        renderer.circle(x+12f*s, y+10f*s, 12f*s, 22)
        renderer.color = Color.valueOf("202336")
        renderer.circle(x+16f*s, y+11f*s, 5f*s, 16)
        renderer.color = Color.valueOf("FFD057")
        renderer.triangle(x+24f*s, y+5f*s, x+44f*s, y, x+24f*s, y-5f*s)
        renderer.color = Color.valueOf("714025")
        renderer.ellipse(x-9f*s, y+23f*s, 36f*s, 16f*s)
        renderer.color = Color.valueOf("FFB943")
        renderer.circle(x+1f*s, y+26f*s, 12f*s, 20)
        renderer.circle(x+22f*s, y+25f*s, 11f*s, 20)
        renderer.color = Color.valueOf("B5ECFF")
        renderer.circle(x+1f*s, y+27f*s, 8f*s, 20)
        renderer.circle(x+22f*s, y+26f*s, 7f*s, 20)
        if (currentSkin == 4) {
            renderer.color = Color.valueOf("FFE46B")
            renderer.triangle(x-5f*s, y+40f*s, x+5f*s, y+58f*s, x+18f*s, y+40f*s)
        }
    }

    private fun branding() {
        label("SKY", 185f, 835f, 4.8f, Color.valueOf("9BEAFF"))
        label("PEEP", 365f, 835f, 4.8f, Color.valueOf("FFCA4C"))
    }

    private fun wallet() {
        panel(14f, 892f, 163f, 50f, Color.valueOf("18426E"))
        renderer.color = Color.valueOf("FFD544")
        renderer.circle(38f, 917f, 17f, 24)
        label(coins.toString(), 113f, 918f)
        panel(348f, 892f, 113f, 50f, Color.valueOf("18426E"))
        renderer.color = Color.valueOf("C46BF1")
        renderer.triangle(355f, 923f, 370f, 940f, 385f, 923f)
        renderer.triangle(355f, 923f, 385f, 923f, 370f, 904f)
        label(gems.toString(), 425f, 918f)
    }

    private fun splash() {
        branding()
        bird(265f, 545f, 3f)
        action("TAP TO START", 95f, 160f, 350f, 90f, Color.valueOf("FFBC39"), 2.4f) { page = Page.MENU }
    }

    private fun menu() {
        wallet()
        branding()
        bird(260f, 642f, 2f)
        action("PLAY", 100f, 399f, 340f, 112f, Color.valueOf("F9B72F"), 3.2f) { startGame(world) }
        action("LEVELS", 32f, 270f, 230f, 89f, Color.valueOf("258ED3")) { page = Page.LEVELS }
        action("SKINS", 279f, 270f, 230f, 89f, Color.valueOf("A362E3")) { page = Page.SKINS }
        action("SHOP", 32f, 165f, 230f, 89f, Color.valueOf("5BBE36")) { page = Page.SKINS }
        action("SETTINGS", 279f, 165f, 230f, 89f, Color.valueOf("6E91B5"), 1.65f) { page = Page.SETTINGS }
    }

    private fun levels() {
        label("SKY WORLDS", 270f, 835f, 3.1f, Color.valueOf("FFF5C6"))
        for (i in 0..5) {
            val x = if (i % 2 == 0) 25f else 275f
            val y = 619f - (i / 2) * 203f
            panel(x, y, 240f, 168f, if (i + 1 <= unlocked) worlds[i].accent else Color.valueOf("8494A9"))
            label((i + 1).toString(), x+120f, y+125f, 2.8f)
            label(worlds[i].name, x+120f, y+76f, 1.25f)
            label(if (i + 1 <= unlocked) stars[i].toString() + " / 3" else "LOCKED", x+120f, y+34f, 1.5f)
            val level = i + 1
            areas += Tap(x, y, 240f, 168f) {
                if (level <= unlocked) startGame(level) else notifyUser("COMPLETE PREVIOUS WORLD")
            }
        }
        action("BACK", 25f, 45f, 165f, 65f, Color.valueOf("218DD0")) { page = Page.MENU }
    }

    private fun pillar(x: Float, y: Float, h: Float, upper: Boolean) {
        if (h <= 0f) return
        val t = worlds[world - 1]
        renderer.color = Color.valueOf("102F56")
        renderer.rect(x - 3f, y, gateWidth + 6f, h)
        renderer.color = t.column
        renderer.rect(x, y, gateWidth, h)
        renderer.color = Color(1f, 1f, 1f, .30f)
        renderer.rect(x + 9f, y, 9f, h)
        val cap = if (upper) y else y + h - 24f
        renderer.color = t.accent
        renderer.rect(x - 8f, cap, gateWidth + 16f, 24f)
        renderer.color = Color.valueOf("F6F5E5")
        renderer.circle(x + gateWidth / 2f, cap + 12f, 9f, 22)
        if (world == 3 || world == 5) {
            renderer.color = if (world == 3) Color.valueOf("72E8F7") else Color.valueOf("CF7BFB")
            renderer.triangle(x+12f, cap+24f, x+37f, cap+51f, x+62f, cap+24f)
        } else if (world == 4) {
            renderer.color = Color.valueOf("2576B4")
            renderer.rect(x+27f, cap+2f, 23f, 19f)
        }
    }

    private fun gameplay(clickable: Boolean) {
        val gap = 295f - world * 11f
        for (g in gates) {
            pillar(g.x, 0f, g.y - gap / 2f, false)
            pillar(g.x, g.y + gap / 2f, height - g.y - gap / 2f, true)
            if (!g.picked) {
                renderer.color = Color.valueOf("FFC33E")
                renderer.circle(g.x + gateWidth / 2f, g.y, 16f, 24)
            }
        }
        bird(birdX, playerY, 1.35f)
        wallet()
        panel(208f, 881f, 117f, 74f, Color.valueOf("F9FEFF"))
        label(points.toString(), 266f, 918f, 2.7f, Color.valueOf("16406E"))
        if (clickable) {
            action("II", 474f, 888f, 52f, 54f, Color.valueOf("188DD0"), 2f) { page = Page.PAUSE }
        }
        if (clickable && !isStarted) {
            panel(65f, 390f, 410f, 148f, Color.valueOf("234D75"))
            label("TAP TO FLY", 270f, 493f, 2.6f)
            label(worlds[world-1].name, 270f, 452f, 1.7f)
            label("PASS " + (10 + world * 4) + " GATES", 270f, 414f, 1.4f)
        }
    }

    private fun pause() {
        renderer.color = Color(0f, 0.09f, 0.2f, .7f)
        renderer.rect(0f, 0f, width, height)
        panel(55f, 215f, 430f, 540f, Color.valueOf("FFF1D7"))
        bird(270f, 743f, 1.3f)
        label("PAUSED", 270f, 657f, 3.1f, Color.valueOf("664B35"))
        action("RESUME", 103f, 545f, 334f, 83f, Color.valueOf("53C83C")) { page = Page.PLAY }
        action("RESTART", 103f, 449f, 334f, 83f, Color.valueOf("F0B62E")) { startGame(world) }
        action("HOME", 103f, 353f, 334f, 83f, Color.valueOf("EE5049")) { page = Page.MENU }
        action(if (musicEnabled) "MUSIC ON" else "MUSIC OFF", 100f, 265f, 166f, 65f, Color.valueOf("67AFC8"), 1.3f) {
            musicEnabled = !musicEnabled; save()
        }
        action(if (soundEnabled) "SFX ON" else "SFX OFF", 280f, 265f, 163f, 65f, Color.valueOf("67AFC8"), 1.3f) {
            soundEnabled = !soundEnabled; save()
        }
    }

    private fun results() {
        panel(42f, 270f, 456f, 465f, Color.valueOf("FFF0D9"))
        bird(270f, 732f, 1.5f)
        label(if (won) "LEVEL COMPLETE" else "GAME OVER", 270f, 647f, 2.6f, Color.valueOf("DD6046"))
        label("SCORE       BEST       COINS", 270f, 556f, 1.45f, Color.valueOf("634B39"))
        label(points.toString() + "           " + best + "           " + (points + runCoins), 270f, 504f, 2f, Color.valueOf("634B39"))
        label(if (won) "NEXT WORLD UNLOCKED" else "TRY AGAIN!", 270f, 399f, 1.4f, Color.valueOf("634B39"))
        action("HOME", 20f, 149f, 156f, 92f, Color.valueOf("278FCB"), 1.7f) { page = Page.MENU }
        action(if (won && world < 6) "NEXT" else "RETRY", 188f, 149f, 167f, 92f, Color.valueOf("53C63C"), 1.8f) {
            startGame(if (won && world < 6) world + 1 else world)
        }
        action("SHARE", 368f, 149f, 152f, 92f, Color.valueOf("F3B235"), 1.7f) {
            val summary = "SkyPeep | " + worlds[world-1].name + " | Score " + points + " | Best " + best
            if (share != null) share.invoke(summary) else Gdx.app.clipboard.contents = summary
            notifyUser("RESULT READY TO SHARE")
        }
    }

    private fun skins() {
        wallet()
        label("SKINS", 270f, 821f, 3.5f, Color.valueOf("FFF4C8"))
        bird(265f, 660f, 2.1f)
        panel(162f, 542f, 216f, 69f, Color.valueOf("F3DAAC"))
        label(names[currentSkin] + " EQUIPPED", 270f, 575f, 1.3f, Color.valueOf("643C29"))
        for (i in 0..4) {
            val x = 12f + (i % 3) * 177f
            val y = if (i < 3) 340f else 165f
            panel(x, y, 163f, 153f, Color.valueOf(if (currentSkin == i) "75CF60" else "FFE9C5"))
            renderer.color = Color.valueOf(colors[i])
            renderer.circle(x+82f, y+114f, 26f, 26)
            label(names[i], x+82f, y+64f, 1.28f, Color.valueOf("6C4430"))
            label(if (owned[i]) "EQUIP" else prices[i].toString() + (if (i == 3) " GEMS" else " COINS"),
                x+82f, y+27f, 1.03f, Color.valueOf("6C4430"))
            val which = i
            areas += Tap(x, y, 163f, 153f) { equip(which) }
        }
        action("BACK", 356f, 35f, 160f, 70f, Color.valueOf("2B98CE")) { page = Page.MENU }
    }

    private fun equip(i: Int) {
        when {
            owned[i] -> { currentSkin = i; notifyUser(names[i] + " EQUIPPED") }
            i == 3 && gems >= prices[i] -> { gems -= prices[i]; owned[i] = true; currentSkin = i; notifyUser("COSMIC UNLOCKED") }
            i != 3 && coins >= prices[i] -> { coins -= prices[i]; owned[i] = true; currentSkin = i; notifyUser(names[i] + " UNLOCKED") }
            else -> notifyUser("NOT ENOUGH CURRENCY")
        }
        save()
    }

    private fun settings() {
        label("SETTINGS", 270f, 815f, 3.1f, Color.valueOf("FFF6D1"))
        panel(55f, 328f, 430f, 425f, Color.valueOf("FFF2D9"))
        action(if (musicEnabled) "MUSIC ON" else "MUSIC OFF", 92f, 627f, 356f, 82f, Color.valueOf("5AAED8")) {
            musicEnabled = !musicEnabled; save()
        }
        action(if (soundEnabled) "SOUND ON" else "SOUND OFF", 92f, 512f, 356f, 82f, Color.valueOf("5AAED8")) {
            soundEnabled = !soundEnabled; save()
        }
        label("SCORES & PURCHASES SAVE LOCALLY", 270f, 396f, 1.21f, Color.valueOf("614938"))
        action("BACK", 135f, 205f, 270f, 85f, Color.valueOf("359CD4")) { page = Page.MENU }
    }

    override fun dispose() {
        save()
        renderer.dispose()
        font.dispose()
        batch.dispose()
    }
}
