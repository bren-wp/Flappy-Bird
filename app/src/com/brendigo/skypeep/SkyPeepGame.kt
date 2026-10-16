/*
 * SkyPeep gameplay, visual integration and menus.
 * Derived from the original user-supplied FlappyBird Android/libGDX starter.
 * Original 2018 Kotlin starter copyright Konstantinos Drakonakis (Apache-2.0).
 * Reference illustrations supplied by the user; no online image assets.
 */
package com.brendigo.skypeep

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** All locations refer to the original 941x1672 portrait artwork. No scrolling. */
class SkyPeepGame(private val shareText: ((String) -> Unit)? = null,
                  private val qaWorld: Int = 0,
                  private val qaSkin: Int = -1,
                  private val qaVictory: Boolean = false,
                  private val qaPillars: Boolean = false,
                  private val qaScore: Int = -1,
                  private val qaMapUnlocked: Int = -1,
                  private val qaLoss: Boolean = false,
                  private val qaBackgroundMotion: Boolean = false,
                  private val qaDifficulty: Int = -1) : ApplicationAdapter() {
    companion object {
        private const val W = 941f
        private const val H = 1672f
        private const val BIRD_X = 172f
        private val BIRD_W = SkyPeepBirdLayout.WIDTH
        private val BIRD_H = SkyPeepBirdLayout.HEIGHT
        // Rendered pillar assets occupy the full width, including their caps.
        private const val GATE_WIDTH = 275f
        private const val MAX_LEVEL = 6
    }

    private enum class Screen { SPLASH, MENU, MAP, PLAY, PAUSE, RESULT, SKINS, SHOP, SETTINGS }
    private data class Gate(var x: Float, var centerY: Float, var scored: Boolean = false,
                            var coinTaken: Boolean = false, var gemTaken: Boolean = false,
                            val gem: Boolean = false)

    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: ExtendViewport
    private var visibleFrame = SkyPeepScreenFrame.forPixels(941,1672)
    private lateinit var prefs: Preferences
    private val pictures = mutableMapOf<String, Texture>()
    private var loadedScreenArt: String? = null
    private var loadedLevelArt = 0
    private lateinit var fontSmall: BitmapFont
    private lateinit var fontLarge: BitmapFont
    private lateinit var solid: Texture
    private val chrome = mutableMapOf<String, com.badlogic.gdx.graphics.g2d.NinePatch>()
    private val chromeTextures = mutableMapOf<String, Texture>()
    private var costumes: Map<Int, Texture> = emptyMap()
    private var victoryStar: Texture? = null
    private var pillarStem: Texture? = null
    private var trailTexture: Texture? = null
    private var selectedTrail = 0
    private val ownedTrails = BooleanArray(4)
    private var music: Music? = null
    private val sounds = mutableMapOf<String, Sound>()
    private val tmp = GlyphLayout()
    private val touch = Vector2()

    private var screen = Screen.SPLASH
    private var lastLoggedScreen: Screen? = null
    private var level = 1
    private var unlocked = 1
    private var resultWon = false
    private var score = 0
    private var best = 0
    private var coins = 1250
    private var gems = 75
    private var earnedCoins = 0
    private var selectedSkin = 0
    private val ownedSkins = BooleanArray(5)
    private val stars = IntArray(MAX_LEVEL)
    // Route progress is independent for BREEZE, EXPLORER and TEMPEST.
    // Existing stars and global best remain intact for save compatibility.
    private var difficulty = SkyDifficulty.EXPLORER
    private val routeStars = Array(SkyDifficulty.entries.size) { IntArray(MAX_LEVEL) }
    private val routeBests = IntArray(SkyDifficulty.entries.size)
    private var precisionPasses = 0
    private var precisionStreak = 0
    private var earnedStars = 0
    private var musicEnabled = true
    private var effectsEnabled = true
    private var vibrationEnabled = false
    private var birdY = 815f
    private var verticalSpeed = 0f
    private var flapTimer = 0f
    private var readyForFirstTap = false
    private var animationTime = 0f
    // Separate motion clock freezes scenery when paused or in a menu.
    private var flightBackgroundTime = 0f
    private var lastBackgroundLogSecond = -1
    private var lastPhysicsLogSample = -1
    private var nextGate = 1.5f
    private val gates = mutableListOf<Gate>()
    private var message = ""
    private var messageTimer = 0f

    override fun create() {
        batch = SpriteBatch()
        camera = OrthographicCamera()
        viewport = ExtendViewport(W, H, camera)
        resize(Gdx.graphics.width, Gdx.graphics.height)
        batch.projectionMatrix = camera.combined
        prefs = Gdx.app.getPreferences("SkyPeepSettingsAndProgress")
        best = prefs.getInteger("best", 0).coerceAtLeast(0)
        unlocked = prefs.getInteger("unlocked", 1).coerceIn(1, MAX_LEVEL)
        level = prefs.getInteger("level", 1).coerceIn(1, unlocked)
        coins = prefs.getInteger("coins", 1250).coerceAtLeast(0)
        gems = prefs.getInteger("gems", 75).coerceAtLeast(0)
        selectedSkin = prefs.getInteger("skin", 0).coerceIn(0, 4)
        musicEnabled = prefs.getBoolean("music", true)
        effectsEnabled = prefs.getBoolean("effects", true)
        vibrationEnabled = prefs.getBoolean("vibration", false)
        ownedSkins[0] = true
        for (i in 1..4) ownedSkins[i] = prefs.getBoolean("owned_$i", false)
        if (!ownedSkins[selectedSkin]) selectedSkin = 0
        for (i in 0 until MAX_LEVEL) stars[i] = prefs.getInteger("stars_${i+1}", 0).coerceIn(0, 3)
        difficulty = SkyDifficulty.fromStored(
            prefs.getInteger("difficulty", SkyDifficulty.EXPLORER.ordinal))
        for (mode in SkyDifficulty.entries) {
            routeBests[mode.ordinal] = prefs.getInteger(
                "route_best_${mode.ordinal}",
                if (mode == SkyDifficulty.EXPLORER) best else 0).coerceAtLeast(0)
            for (i in 0 until MAX_LEVEL) {
                val fallback = if (mode == SkyDifficulty.EXPLORER) stars[i] else 0
                routeStars[mode.ordinal][i] = prefs.getInteger(
                    "route_stars_${mode.ordinal}_${i+1}", fallback).coerceIn(0, 3)
            }
        }
        // QA may select a different license but never opens locked worlds in release APKs.
        if (qaDifficulty in SkyDifficulty.entries.indices)
            difficulty = SkyDifficulty.fromStored(qaDifficulty)
        Gdx.app.log("SkyPeepDifficulty", "loaded=${difficulty.title}")
        ownedTrails[0] = true
        for (i in 1..3) ownedTrails[i] = prefs.getBoolean("trail_owned_$i",false)
        selectedTrail = prefs.getInteger("trail_selected",0).coerceIn(0,3)
        if (!ownedTrails[selectedTrail]) selectedTrail = 0
        Gdx.app.log("SkyPeepShop",
            "loaded=${SkyPeepTrailCatalog.all[selectedTrail].name}")

        // Screen textures are loaded on demand, rather than keeping every
        // full-portrait menu illustration in GPU memory at the same time.
        syncScreenArt()
        // Only retain textures for the selected level. Preloading all six worlds
        // multiplied the texture-memory footprint on lower-end Android devices.
        loadLevelArt(level)
        for (pose in arrayOf("idle", "up", "down")) load("bird_$pose")
        load("coin_asset")
        load("gem_asset")
        fontSmall = BitmapFont(Gdx.files.internal("ui_36.fnt"))
        fontLarge = BitmapFont(Gdx.files.internal("ui_62.fnt"))
        val pixel = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixel.setColor(Color.WHITE)
        pixel.fill()
        solid = Texture(pixel)
        pixel.dispose()
        prepareChrome()
        costumes = SkyPeepBirdCostumes.prepare()
        victoryStar = createVictoryStar()
        trailTexture = SkyPeepTrailArtwork.create()
        for (kind in arrayOf("flap", "coin", "win", "fail", "click")) {
            try { sounds[kind] = Gdx.audio.newSound(Gdx.files.internal("$kind.wav")) }
            catch (_: Exception) { /* audio is optional on unsupported devices */ }
        }
        try {
            music = Gdx.audio.newMusic(Gdx.files.internal("music.ogg"))
            music?.isLooping = true
            music?.volume = .40f
            if (musicEnabled) music?.play()
        } catch (_: Exception) { music = null }
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true)
        if (qaMapUnlocked in 1..MAX_LEVEL) {
            // Debug-only screenshot fixture: a freshly unlocked world must
            // never be shown as completed before its first successful flight.
            unlocked = qaMapUnlocked
            level = qaMapUnlocked
            for (i in 0 until MAX_LEVEL) {
                stars[i] = if (i < unlocked - 1) 3 else 0
                routeStars[difficulty.ordinal][i] = stars[i]
            }
            screen = Screen.MAP
            Gdx.app.log("SkyPeepMap","map_loaded unlocked=$unlocked selected=$level")
        }
        if (qaWorld in 1..MAX_LEVEL) {
            unlocked = MAX_LEVEL
            if(qaSkin in 0..4) {
                selectedSkin = qaSkin
                ownedSkins[qaSkin] = true
                Gdx.app.log("SkyPeepSkin","skin=" + SkyPeepSkinCatalog.get(qaSkin).name)
            }
            begin(qaWorld)
            if (qaBackgroundMotion) readyForFirstTap = false
            if (qaScore >= 0) {
                score = qaScore.coerceAtMost(999999)
                Gdx.app.log("SkyPeepHUD","debug_score=$score level=$level")
            }
            if (qaVictory) {
                score = targetScore()
                finish(true)
                Gdx.app.log("SkyPeepQA","victory level=" + level)
            } else if (qaLoss) {
                finish(false)
                Gdx.app.log("SkyPeepQA","loss level=" + level)
            }
        }
    }

    private fun loadLevelArt(newLevel: Int) {
        require(newLevel in 1..MAX_LEVEL)
        if (loadedLevelArt == newLevel) return
        if (loadedLevelArt != 0) {
            for (asset in levelArtNames(loadedLevelArt)) {
                pictures.remove(asset)?.dispose()
            }
        }
        for (asset in levelArtNames(newLevel)) {
            load(asset, if (asset.startsWith("bg_")) "jpg" else "png")
        }
        pillarStem?.dispose()
        pillarStem = SkyPeepPillarStem.create(newLevel)
        loadedLevelArt = newLevel
    }

    private fun levelArtNames(world: Int): List<String> =
        listOf("bg_$world", "pillar_${world}_top", "pillar_${world}_bottom") +
            listOf("left", "score", "right", "pause").map { "hud_${it}_$world" }

    private fun syncScreenArt() {
        // Reuse the current illustration while in the same screen, and dispose
        // the previous full-screen JPG before switching to another UI page.
        // Victory must never render the source illustration's baked-in GAME OVER text.
        val required = if (screen == Screen.RESULT && resultWon) null
                       else SkyPeepAssets.screenFor(screen.name)
        if (required == loadedScreenArt) return
        loadedScreenArt?.let { pictures.remove(it)?.dispose() }
        if (required != null) load(required, "jpg")
        loadedScreenArt = required
    }

    private fun load(name: String, suffix: String = "png") {
        val texture = Texture(Gdx.files.internal("$name.$suffix"))
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pictures[name] = texture
    }

    private fun image(name: String, x: Float, y: Float, width: Float, height: Float) {
        pictures[name]?.let { batch.draw(it, x, y, width, height) }
    }

    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        visibleFrame = SkyPeepScreenFrame.forPixels(width, height)
        // ExtendViewport consumes the entire device display. Recenter its
        // expanded world around the original reference art rather than
        // shifting the buttons, HUD, collision world or tap targets.
        viewport.update(width, height, false)
        camera.position.set(W*.5f, H*.5f, 0f)
        camera.update()
        Gdx.app.log("SkyPeepViewport",
            "device=${width}x${height} frame=" +
            "${visibleFrame.left},${visibleFrame.bottom}.." +
            "${visibleFrame.right},${visibleFrame.top}")
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime.coerceIn(0f, .05f)
        animationTime += delta
        if (screen == Screen.PLAY && !readyForFirstTap) flightBackgroundTime += delta
        if (messageTimer > 0f) messageTimer -= delta
        handleInput()
        if (screen == Screen.PLAY) updateGame(delta)
        syncScreenArt()
        if (screen != lastLoggedScreen) {
            Gdx.app.log("SkyPeepNav","screen=" + screen.name)
            lastLoggedScreen = screen
        }
        Gdx.gl.glClearColor(.16f,.67f,.96f,1f)
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        drawFullBleedEdges()
        when (screen) {
            Screen.SPLASH -> image("screen_splash", 0f, 0f, W, H)
            Screen.MENU -> { image("screen_menu", 0f, 0f, W, H); drawMenuWallet() }
            Screen.MAP -> { image("screen_map", 0f, 0f, W, H); drawMapProgress() }
            Screen.PLAY -> drawGameplay()
            Screen.PAUSE -> { image("screen_pause", 0f, 0f, W, H); drawPauseToggles() }
            Screen.RESULT -> {
                if (resultWon) drawVictory() else {
                    image("screen_over", 0f, 0f, W, H)
                    drawResult()
                }
            }
            Screen.SKINS -> { image("screen_skins", 0f, 0f, W, H); drawSkins() }
            Screen.SHOP -> { image("screen_menu",0f,0f,W,H); drawShop() }
            Screen.SETTINGS -> { image("screen_menu", 0f, 0f, W, H); drawSettings() }
        }
        if (messageTimer > 0 && screen != Screen.PLAY) {
            badge("dark",110f,1375f,W-220f,103f)
            fitted(message,1437f,fontSmall,1.4f,W-290f)
        }
        batch.end()
    }

    /**
     * Paint the *real artwork's outer edge pixels* into ExtendViewport's
     * additional display area. The illustrated 941x1672 UI stays undistorted
     * and every original touch rectangle remains at its reference position.
     * No bars are left blank on tall phones or wide tablets.
     */
    private fun drawFullBleedEdges() {
        val name = when(screen) {
            Screen.PLAY -> "bg_$level"
            Screen.RESULT -> if(resultWon) "bg_$level" else "screen_over"
            Screen.SHOP, Screen.SETTINGS -> "screen_menu"
            Screen.SPLASH -> "screen_splash"
            Screen.MENU -> "screen_menu"
            Screen.MAP -> "screen_map"
            Screen.PAUSE -> "screen_pause"
            Screen.SKINS -> "screen_skins"
        }
        val texture = pictures[name] ?: return
        val f=visibleFrame
        if(f.extraY>.01f) {
            // LibGDX source pixels use top-left origin. These thin strips
            // extend natural sky above and ground/clouds below the artwork.
            batch.draw(texture,0f,-f.extraY,W,f.extraY,
                0,texture.height-4,texture.width,4,false,false)
            batch.draw(texture,0f,H,W,f.extraY,
                0,0,texture.width,4,false,false)
        }
        if(f.extraX>.01f) {
            batch.draw(texture,-f.extraX,-f.extraY,
                f.extraX,H+2f*f.extraY,
                0,0,4,texture.height,false,false)
            batch.draw(texture,W,-f.extraY,
                f.extraX,H+2f*f.extraY,
                texture.width-4,0,4,texture.height,false,false)
        }
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            when (screen) {
                Screen.PLAY -> screen = Screen.PAUSE
                Screen.PAUSE -> screen = Screen.PLAY
                Screen.SPLASH -> screen = Screen.MENU
                else -> screen = Screen.MENU
            }
            playSound("click")
            return
        }
        if (!Gdx.input.justTouched()) return
        touch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(touch)
        if (touch.x < visibleFrame.left || touch.x > visibleFrame.right ||
            touch.y < visibleFrame.bottom || touch.y > visibleFrame.top) return
        // Extra viewport strips are scenery only on menu/dialogue pages.
        // During gameplay tapping any visible sky should still flap.
        if (screen != Screen.PLAY &&
            (touch.x < 0f || touch.x > W ||
             touch.y < 0f || touch.y > H)) return
        val x = touch.x
        val yTop = H - touch.y
        when (screen) {
            Screen.SPLASH -> { screen = Screen.MENU; playSound("click") }
            Screen.MENU -> clickMenu(x, yTop)
            Screen.MAP -> clickMap(x, yTop)
            Screen.PLAY -> {
                if (hit(x,yTop,806f,-visibleFrame.extraY,135f,155f)) {
                    screen = Screen.PAUSE
                    playSound("click")
                }
                else flap()
            }
            Screen.PAUSE -> clickPause(x,yTop)
            Screen.RESULT -> clickResult(x,yTop)
            Screen.SKINS -> clickSkins(x,yTop)
            Screen.SHOP -> clickShop(x,yTop)
            Screen.SETTINGS -> clickSettings(x,yTop)
        }
    }

    private fun hit(x: Float,y: Float, l: Float,t: Float,w: Float,h: Float): Boolean =
        x >= l && x <= l+w && y >= t && y <= t+h

    private fun clickMenu(x: Float, y: Float) {
        playSound("click")
        when {
            hit(x,y,170f,895f,590f,248f) -> begin(level)
            hit(x,y,95f,1155f,364f,169f) -> screen = Screen.MAP
            hit(x,y,471f,1155f,370f,169f) -> screen = Screen.SKINS
            hit(x,y,95f,1318f,364f,173f) -> screen = Screen.SHOP
            hit(x,y,471f,1318f,370f,173f) -> screen = Screen.SETTINGS
            hit(x,y,243f,0f,100f,130f) -> toast("EARN COINS IN FLIGHT")
            hit(x,y,815f,0f,110f,130f) -> toast("COMPLETE WORLDS FOR GEMS")
        }
    }

    private fun clickMap(x: Float, y: Float) {
        playSound("click")
        when {
            SkyPeepMapLayout.back.contains(x,y) -> screen = Screen.MENU
            SkyPeepMapLayout.play.contains(x,y) -> begin(level)
            else -> {
                val tapped=SkyPeepMapLayout.worldAt(x,y) ?: return
                if (tapped > unlocked) {
                    Gdx.app.log("SkyPeepMap","world_locked=$tapped unlocked=$unlocked")
                    toast("COMPLETE PREVIOUS LEVEL")
                    return
                }
                level=tapped
                save()
                Gdx.app.log("SkyPeepMap","selected_world=$level")
                toast("SELECTED: ${levelTitle(level)}")
            }
        }
    }

    private fun clickPause(x: Float, y: Float) {
        // Blank gutters and the darkened background remain inert.
        val action = SkyPeepUiTouch.pauseActionAt(x, y) ?: return
        playSound("click")
        when (action) {
            SkyPeepUiTouch.PauseAction.RESUME -> screen = Screen.PLAY
            SkyPeepUiTouch.PauseAction.RESTART -> begin(level)
            SkyPeepUiTouch.PauseAction.HOME -> { screen = Screen.MENU; save() }
            SkyPeepUiTouch.PauseAction.MUSIC -> setMusic(!musicEnabled)
            SkyPeepUiTouch.PauseAction.SFX -> {
                effectsEnabled = !effectsEnabled
                save()
            }
        }
    }

    private fun clickResult(x: Float, y: Float) {
        // A tap in the card's empty space or between drawn buttons is inert.
        // Touch geometry is kept outside rendering code for unit testing.
        when (SkyPeepResultTouch.actionAt(x, y, resultWon)) {
            SkyPeepResultTouch.Action.HOME -> {
                playSound("click")
                screen = Screen.MENU
                save()
            }
            SkyPeepResultTouch.Action.RETRY_OR_NEXT -> {
                playSound("click")
                val nextLevel = if (resultWon && level < MAX_LEVEL) level + 1 else level
                begin(nextLevel)
            }
            SkyPeepResultTouch.Action.SHARE -> {
                playSound("click")
                val msg = "SkyPeep | ${levelTitle(level)} | ${difficulty.title} | Score $score | Best ${routeBests[difficulty.ordinal]}"
                if (shareText != null) shareText.invoke(msg)
                else Gdx.app.clipboard.contents = msg
                toast("SHARE RESULT")
            }
            null -> Unit
        }
    }

    private fun clickSkins(x: Float, y: Float) {
        playSound("click")
        if (y < 140f && x in 240f..355f) {
            toast("EARN COINS IN FLIGHT")
            return
        }
        if (y < 140f && x in 825f..935f) {
            toast("COMPLETE WORLDS FOR GEMS")
            return
        }
        if (SkyPeepUiTouch.skinsBack(x,y)) { screen = Screen.MENU; return }
        if (hit(x,y,285f,810f,398f,120f)) { selectedSkin = 0; save(); toast("DEFAULT EQUIPPED"); return }
        // Card art and prices are informational, only the BUY/EQUIP control is actionable.
        val skin = SkyPeepUiTouch.skinPurchaseAt(x,y) ?: return
        if (ownedSkins[skin]) {
            selectedSkin = skin
            save()
            toast("${skinName(skin)} EQUIPPED")
            return
        }
        val choice = SkyPeepSkinCatalog.get(skin)
        val useGem = choice.currency == SkinCurrency.GEMS
        val price = choice.price
        if (!choice.affordable(coins,gems)) {
            toast("NOT ENOUGH ${if (useGem) "GEMS" else "COINS"}")
            playSound("fail")
        } else {
            if (useGem) gems -= price else coins -= price
            ownedSkins[skin]=true
            selectedSkin=skin
            save()
            toast("${skinName(skin)} UNLOCKED")
            playSound("win")
        }
    }

    private fun clickShop(x:Float,yTop:Float) {
        if(SkyPeepUiTouch.shopBack(x,yTop)) {
            playSound("click")
            screen=Screen.MENU
            save()
            return
        }
        // A purchase must originate from the visible BUY/EQUIP badge.
        val offerId = SkyPeepUiTouch.shopPurchaseAt(x,yTop) ?: return
        val offer = SkyPeepTrailCatalog.all[offerId]
        val purchased=SkyPeepTrailCatalog.purchase(
            offer.id,ownedTrails[offer.id],coins,gems)
        if (!purchased.success) {
            toast("NOT ENOUGH " + (if(offer.currency==TrailCurrency.GEMS) "GEMS" else "COINS"))
            playSound("fail")
            return
        }
        coins=purchased.coins
        gems=purchased.gems
        ownedTrails[offer.id]=true
        selectedTrail=offer.id
        save()
        toast(if(purchased.alreadyOwned) "${offer.name} EQUIPPED"
              else "${offer.name} UNLOCKED")
        // Avoid writing wallet balances into globally accessible diagnostic logs.
        Gdx.app.log("SkyPeepShop","selected=${offer.name}")
        playSound(if(purchased.alreadyOwned) "click" else "win")
    }

    private fun clickSettings(x: Float, y: Float) {
        if (SkyPeepUiTouch.settingsBack(x,y)) {
            playSound("click")
            screen=Screen.MENU
            save()
            return
        }
        // Non-interactive gutters stay silent, and the visible fourth row
        // alone cycles the player's route license.
        val row = SkyPeepUiTouch.settingsRowAt(x,y) ?: return
        playSound("click")
        when (row) {
            0 -> setMusic(!musicEnabled)
            1 -> { effectsEnabled=!effectsEnabled; save() }
            2 -> { vibrationEnabled=!vibrationEnabled; save() }
            3 -> {
                difficulty = difficulty.next()
                save()
                toast("SKY ROUTE: ${difficulty.title}")
                Gdx.app.log("SkyPeepDifficulty","selected=${difficulty.title}")
            }
        }
    }

    private fun setMusic(enabled:Boolean) {
        musicEnabled=enabled
        if (enabled) music?.play() else music?.pause()
        save()
    }

    private fun begin(chosen: Int) {
        level = chosen.coerceIn(1, unlocked)
        loadLevelArt(level)
        screen = Screen.PLAY
        score=0
        earnedCoins=0
        resultWon=false
        precisionPasses=0
        precisionStreak=0
        earnedStars=0
        nextGate=2.0f
        gates.clear()
        if (qaPillars) gates.add(Gate(W - 300f,1050f))
        verticalSpeed=0f
        birdY=840f
        flapTimer=0f
        animationTime=0f
        flightBackgroundTime=0f
        lastBackgroundLogSecond=-1
        lastPhysicsLogSample=-1
        readyForFirstTap=true
        save()
    }

    private fun flap() {
        readyForFirstTap=false
        verticalSpeed=690f
        flapTimer=.13f
        playSound("flap")
    }

    private fun updateGame(dt: Float) {
        // Debug-only GL fixture: observe active background movement without
        // collisions/gravity terminating the run during emulator capture.
        if (qaBackgroundMotion && qaWorld in 1..MAX_LEVEL) return
        if (readyForFirstTap) return
        flapTimer = max(0f,flapTimer-dt)
        val gravity=SkyRouteRules.gravity(difficulty)
        val skyCurrent=SkyRouteRules.windAcceleration(level,difficulty,flightBackgroundTime)
        // Debug-only real-physics QA: log while a live flight updates, never
        // during the frozen-background visual screenshot fixture.
        if (qaDifficulty >= 0 && qaWorld in 1..MAX_LEVEL) {
            val sample=(flightBackgroundTime * 5f).toInt()
            if(sample!=lastPhysicsLogSample) {
                lastPhysicsLogSample=sample
                Gdx.app.log("SkyPeepPhysics",
                    "world=$level mode=${difficulty.title} wind=${skyCurrent.toInt()} " +
                    "gravity=${gravity.toInt()} gap=${SkyRouteRules.gap(level,difficulty).toInt()} " +
                    "speed=${SkyRouteRules.speed(level,difficulty).toInt()}")
            }
        }
        // Preserve Explorer's original -900 px/s terminal velocity, while
        // making Breeze gentler and Tempest's descent more demanding.
        verticalSpeed = max(-900f * difficulty.gravityFactor,
            verticalSpeed + (skyCurrent - gravity)*dt)
        birdY += verticalSpeed * dt
        val birdBody=SkyPeepContact.body(BIRD_X,birdY,BIRD_W,BIRD_H)
        if (SkyPeepContact.hitsWorldEdge(birdBody,
                visibleFrame.bottom,visibleFrame.top)) {
            finish(false)
            return
        }
        val gap = SkyRouteRules.gap(level,difficulty)
        val moveSpeed = SkyRouteRules.speed(level,difficulty)
        nextGate -= dt
        if (nextGate <= 0f) {
            nextGate=SkyRouteRules.spawnSeconds(level,difficulty)
            val center = MathUtils.random(570f,1110f)
            gates.add(Gate(SkyPeepScreenFrame.pillarSpawnX(visibleFrame),
                center,gem=(score>0 && MathUtils.random(0,9)==0)))
        }
        val iterator=gates.iterator()
        while (iterator.hasNext()) {
            val gate=iterator.next()
            gate.x -= moveSpeed * dt
            val gateWidth=GATE_WIDTH
            val topLimit=gate.centerY+gap*.5f
            val bottomLimit=gate.centerY-gap*.5f
            if (SkyPeepContact.hitsGate(
                    birdBody,gate.x,gateWidth,bottomLimit,topLimit)) {
                finish(false)
                return
            }
            // Match the actual drawn coin centre and avoid collecting an
            // item solely because the gate crosses the bird's horizontal band.
            if (!gate.coinTaken && FlightRules.overlapsPickup(
                playerX = BIRD_X + BIRD_W * .5f,
                playerY = birdY + BIRD_H * .5f,
                pickupX = gate.x + 118f,
                pickupY = gate.centerY + 9f,
                radiusX = 128f,
                radiusY = 102f
            )) {
                gate.coinTaken=true
                coins = SkyPeepEconomy.credit(coins,5)
                earnedCoins = SkyPeepEconomy.credit(earnedCoins,5)
                playSound("coin")
            }
            // Gems have their own hitbox; collecting a coin must not grant a distant gem.
            if (gate.gem && !gate.gemTaken &&
                FlightRules.overlapsPickup(
                    playerX = BIRD_X + BIRD_W * .5f,
                    playerY = birdY + BIRD_H * .5f,
                    pickupX = gate.x + 118f,
                    pickupY = gate.centerY + 93f,
                    radiusX = 68f,
                    radiusY = 57f
                )) {
                gate.gemTaken=true
                gems = SkyPeepEconomy.credit(gems,1)
                playSound("coin")
            }
            if (!gate.scored && gate.x+gateWidth < BIRD_X+40f) {
                gate.scored=true
                score++
                best=max(best,score)
                routeBests[difficulty.ordinal]=max(routeBests[difficulty.ordinal],score)
                // Precision routes reward keeping SkyPeep on the centreline.
                // This gives each world an optional skill challenge beyond
                // merely passing the obstacles, without forcing microtransactions.
                val precise=SkyRouteRules.isPrecisionPass(
                    birdY+BIRD_H*.5f,gate.centerY,gap)
                if (precise) {
                    precisionPasses++
                    precisionStreak++
                    val reward=SkyRouteRules.precisionReward(precisionStreak)
                    if(reward>0) {
                        coins=SkyPeepEconomy.credit(coins,reward)
                        earnedCoins=SkyPeepEconomy.credit(earnedCoins,reward)
                        playSound("coin")
                    }
                } else precisionStreak=0
                if (score >= targetScore()) { finish(true); return }
            }
            if (gate.x < SkyPeepScreenFrame.pillarDespawnX(visibleFrame))
                iterator.remove()
        }
    }

    private fun targetScore() = SkyRouteRules.target(level,difficulty)
    private fun finish(won: Boolean) {
        resultWon = won
        screen = Screen.RESULT
        best=max(best,score)
        routeBests[difficulty.ordinal]=max(routeBests[difficulty.ordinal],score)
        if (won) {
            val bonus = SkyRouteRules.completionBonus(level,difficulty)
            coins = SkyPeepEconomy.credit(coins,bonus)
            earnedCoins = SkyPeepEconomy.credit(earnedCoins,bonus)
            earnedStars=SkyRouteRules.starsOnCompletion(precisionPasses,score)
            routeStars[difficulty.ordinal][level-1]=max(
                routeStars[difficulty.ordinal][level-1],earnedStars)
            stars[level-1]=max(stars[level-1],earnedStars)
            unlocked=max(unlocked,min(MAX_LEVEL,level+1))
            playSound("win")
        } else {
            playSound("fail")
            if (vibrationEnabled) try { Gdx.input.vibrate(40) } catch (_:Exception) { }
        }
        save()
    }

    private fun drawGameplay() {
        // Keep the illustrated composition edge-to-edge while introducing
        // subtle depth. No wrap seam, external image, or gameplay collision shift.
        pictures["bg_$level"]?.let { background ->
            val crop = SkyPeepBackgroundMotion.crop(
                background.width, flightBackgroundTime, !readyForFirstTap
            )
            if (qaBackgroundMotion && qaWorld in 1..MAX_LEVEL) {
                val second = flightBackgroundTime.toInt()
                if (second != lastBackgroundLogSecond) {
                    Gdx.app.log("SkyPeepBackground", "second=$second offset_px=${crop.sourceX} world=$level")
                    lastBackgroundLogSecond = second
                }
            }
            batch.draw(background,0f,0f,W,H,
                crop.sourceX,0,crop.sourceWidth,background.height,false,false)
        }
        val gap = SkyRouteRules.gap(level,difficulty)
        for (gate in gates) {
            val top = pictures["pillar_${level}_top"]!!
            val bottom = pictures["pillar_${level}_bottom"]!!
            val topY = gate.centerY+gap*.5f
            val bottomY = gate.centerY-gap*.5f
            val pillarWidth=GATE_WIDTH
            // The pole stems are rendered behind the photographed caps as scalable 2D shapes.
            // Continuous, textured shafts sit behind the original illustrated caps.
            val stemX = gate.x + 53f
            val stemW = pillarWidth - 105f
            drawStoneStem(stemX,stemW,
                PillarGeometry.topTailStart(topY,top.height.toFloat()),
                visibleFrame.top)
            drawStoneStem(stemX,stemW,visibleFrame.bottom,
                PillarGeometry.bottomTailHeight(bottomY,bottom.height.toFloat()))
            image("pillar_${level}_top",gate.x,topY,pillarWidth,top.height.toFloat())
            image("pillar_${level}_bottom",gate.x,bottomY-bottom.height,
                pillarWidth,bottom.height.toFloat())
            if (!gate.coinTaken) {
                drawCoin(gate.x+118f,gate.centerY+9f,37f)
            }
            if (gate.gem && !gate.gemTaken) {
                drawDiamond(gate.x+113f,gate.centerY+93f,41f)
            }
        }
        drawFlightTrail()
        drawBird()
        drawHUD()
        if (precisionStreak >= 2) {
            fitted("SKY ROUTE x$precisionStreak",
                (birdY+BIRD_H+30f).coerceAtMost(H-235f),
                fontSmall,.80f,245f,BIRD_X+BIRD_W*.5f)
        }
        if (readyForFirstTap) {
            badge("dark",290f,355f,360f,88f)
            fitted("TAP TO FLY",412f,fontSmall,1.5f,331f)
        }
    }

    private fun drawStoneStem(x:Float,width:Float,start:Float,end:Float) {
        val tile=pillarStem ?: return
        var y=max(visibleFrame.bottom,start)
        val stop=min(visibleFrame.top,end)
        while(y<stop-0.5f) {
            val size=min(144f,stop-y)
            // Crop the final partial repeat instead of distorting its texture.
            // Its trailing source rows meet the previous full repeat cleanly.
            val sourceHeight = kotlin.math.ceil(size.toDouble()).toInt().coerceIn(1, 144)
            val sourceY = PillarGeometry.trailingSourceY(size)
            batch.draw(tile,x,y,width,size,0,sourceY,128,sourceHeight,false,false)
            y+=size
        }
    }

    private fun drawFlightTrail() {
        if (readyForFirstTap) return
        val tx=trailTexture ?: return
        val tint=when(selectedTrail) {
            1 -> Color.valueOf("43DBFF")
            2 -> Color.valueOf("FFD145")
            3 -> Color.valueOf("C078FF")
            else -> Color.WHITE
        }
        batch.color=tint
        val drift=kotlin.math.sin(animationTime*9f)*7f
        batch.draw(tx,BIRD_X-158f,birdY+41f+drift,180f,129f)
        batch.color=Color.WHITE
    }

    private fun drawBird() {
        val pose=if (flapTimer>0 || ((animationTime*8f).toInt()%3==1)) "up"
                 else if ((animationTime*8f).toInt()%3==2) "down" else "idle"
        // Preserve the exact original 2D source pixels; slight angular bob through vertical physics.
        val tx=pictures["bird_$pose"] ?: return
        val alt=if (selectedSkin==0) Color.WHITE else when (selectedSkin) {
            1 -> Color(.83f,.98f,1f,1f)
            2 -> Color(.50f,1f,.55f,1f)
            3 -> Color(.65f,.55f,1f,1f)
            else -> Color(1f,.91f,.76f,1f)
        }
        batch.color=alt
        // Each bird pose is cropped differently in the source sprites.
        // Use its own natural aspect ratio without shifting SkyPeep's centre.
        batch.draw(tx,
            BIRD_X + SkyPeepBirdLayout.leftFor(tx.width,tx.height),
            birdY,
            SkyPeepBirdLayout.widthFor(tx.width,tx.height),BIRD_H)
        batch.color=Color.WHITE
        // Separate costume layer remains crisp across each of the 3 wing poses.
        if (selectedSkin != 0) costumes[selectedSkin]?.let { costume ->
            batch.draw(costume,
                BIRD_X + SkyPeepBirdLayout.COSTUME_X,
                birdY + BIRD_H - SkyPeepBirdLayout.COSTUME_Y_FROM_TOP,
                SkyPeepBirdLayout.COSTUME_WIDTH,
                SkyPeepBirdLayout.COSTUME_HEIGHT)
        }
    }

    private fun drawHUD() {
        // Exact cropped reference HUD layers. Dynamic figures replace reference mockup values.
        // On tall displays HUD art belongs to the PHYSICAL top of the
        // expanded world, never floating below an unused top inset.
        val top=visibleFrame.top
        image("hud_left_$level", 14f, top-123f, 269f, 113f)
        image("hud_right_$level", 635f, top-124f, 191f, 115f)
        image("hud_pause_$level", 811f, top-129f, 125f, 122f)
        image("hud_score_$level", 286f, top-218f, 367f, 210f)
        // The original six wallet plates are now cleaned at build time.
        // Keep illustrated gemstone, coin and edge strokes 1:1; no generic
        // overlay pills that hide the original decorative cyan borders.
        fitted(SkyPeepHudNumbers.compact(coins),top-55f,fontSmall,1.78f,115f,191f)
        fitted(SkyPeepHudNumbers.compact(gems),top-53f,fontSmall,1.75f,63f,766f)
        // Each original score face is cleared and reconstructed during build.
        // Render changing points on its own themed face, with a crisp outline.
        drawWingedScore()
    }

    private fun drawWingedScore() {
        val text=score.coerceAtLeast(0).toString()
        // The score is part of the bright illustrated winged HUD, not a
        // separate dark overlay. Keep the large reference-style outlined
        // numerals centred and automatically fit three-plus digit results.
        val baseline=visibleFrame.top-65f
        val size=1.30f
        val available=171f
        val center=W/2f
        val outline=Color.valueOf(if(level==6) "603217" else "10264B")
        val ink=Color.valueOf(when(level) {
            1 -> "159DE8"
            2 -> "164F9C"
            3 -> "FFE45C"
            4 -> "2584BE"
            5 -> "A9C1FF"
            else -> "FFBE31"
        })
        for (dx in floatArrayOf(-2.8f,0f,2.8f))
            for (dy in floatArrayOf(-2.8f,0f,2.8f))
                if (dx!=0f || dy!=0f)
                    fitted(text,baseline+dy,fontLarge,size,available,center+dx,outline)
        fitted(text,baseline,fontLarge,size,available,center,ink)
    }

    private fun drawCoin(x:Float,y:Float,r:Float) {
        image("coin_asset",x-r,y-r,r*2f,r*2f)
    }

    private fun drawDiamond(x:Float,y:Float,r:Float) {
        image("gem_asset",x-r,y-r,r*2f,r*2f)
    }

    private fun drawMenuWallet() {
        // Cover only screenshot mockup's sample numeric labels, retaining the original visual buttons.
        // At the reference values preserve the screenshot pixels exactly.
        if(coins!=1250) {
            badge("dark",119f,H-103f,136f,65f)
            fitted(coins.toString(),H-54f,fontSmall,1.9f,127f,186f)
        }
        if(gems!=75) {
            badge("dark",743f,H-103f,76f,65f)
            fitted(gems.toString(),H-54f,fontSmall,1.9f,69f,781f)
        }
    }

    private fun drawMapProgress() {
        // A compact route tag fills the existing gap between BACK and PLAY;
        // the six island illustrations and their layout remain untouched.
        fitted("${difficulty.title} ROUTE",92f,fontSmall,.78f,295f,365f)
        // Locked icons in the original supplied map stay intact. Open and
        // completed islands use different statuses and earned real stars.
        for(world in 1..MAX_LEVEL) {
            if (world > unlocked) continue
            val marker=SkyPeepMapLayout.marker(world)
            if(world>1) {
                val cleared=SkyPeepMapProgress.status(
                    world,unlocked,routeStars[difficulty.ordinal][world-1]) ==
                    MapWorldStatus.COMPLETED
                badge(if(cleared)"green" else "blue",
                    marker.x-58f,H-marker.y-35f,116f,70f)
                fitted(if(cleared)"CLEAR" else "OPEN",H-marker.y+17f,
                    fontSmall,1.03f,112f,marker.x)
            }
            val earned=routeStars[difficulty.ordinal][world-1].coerceIn(0,3)
            if(earned>0) {
                val starRow=SkyPeepMapLayout.starRow(world)
                for(i in 0 until earned) victoryStar?.let { star ->
                    batch.draw(star,starRow.x-79f+59f*i,
                        H-starRow.y-25f,51f,51f)
                }
            }
            if(world==level) {
                badge("wood",marker.x-82f,H-marker.y+47f,164f,55f)
                fitted("SELECTED",H-marker.y+82f,fontSmall,1f,146f,
                    marker.x)
            }
        }
    }

    private fun drawVictory() {
        // Winner screen deliberately has its own composition: no sad bird or
        // baked GAME OVER heading from the loss-only reference artwork.
        image("bg_$level",0f,0f,W,H)
        drawRect(0f,0f,W,H,Color(.06f,.16f,.32f,.29f))
        image("bird_up",366f,1375f,212f,185f)
        if (selectedSkin != 0) costumes[selectedSkin]?.let {
            batch.draw(it,412f,1468f,120f,72f)
        }
        badge("wood",120f,1180f,701f,155f)
        fitted("LEVEL COMPLETE",1284f,fontLarge,1.32f,644f)
        fitted(levelTitle(level).uppercase(Locale.ROOT),1190f,fontSmall,1.55f,580f)
        fitted(difficulty.title,1155f,fontSmall,.90f,220f)
        // A genuine three-star rating based on how accurately the player
        // navigated each sky route, not an unconditional triple gold reward.
        for (i in 0..2) victoryStar?.let {
            if(i >= earnedStars) batch.color=Color(.38f,.44f,.56f,.85f)
            batch.draw(it,291f+i*120f,1009f,115f,115f)
            batch.color=Color.WHITE
        }
        badge("cream",99f,448f,743f,551f)
        fitted("FLIGHT COMPLETED",924f,fontSmall,1.55f,675f,W/2f,Color.valueOf("6B3D26"))
        badge("dark",137f,706f,313f,146f)
        badge("dark",492f,706f,311f,146f)
        fitted("SCORE",826f,fontSmall,1.30f,275f,294f)
        fitted("BEST",826f,fontSmall,1.30f,275f,648f)
        fitted(score.toString(),781f,fontLarge,1.12f,272f,294f)
        fitted(routeBests[difficulty.ordinal].toString(),781f,fontLarge,1.12f,272f,648f)
        badge("wood",137f,508f,666f,156f)
        fitted("COINS EARNED",643f,fontSmall,1.45f,608f)
        fitted("+$earnedCoins",586f,fontLarge,1.1f,608f)
        // Touch zones match the illustrated arcade buttons, not the card.
        SkyPeepResultTouch.victoryButtons.forEach { button ->
            val surface = when (button.action) {
                SkyPeepResultTouch.Action.HOME -> "blue"
                SkyPeepResultTouch.Action.RETRY_OR_NEXT -> "green"
                SkyPeepResultTouch.Action.SHARE -> "wood"
            }
            badge(surface, button.left, H - button.top - button.height,
                button.width, button.height)
        }
        fitted("HOME",303f,fontSmall,1.72f,231f,185f)
        fitted(if(level<MAX_LEVEL) "NEXT" else "REPLAY",303f,fontSmall,1.72f,251f,471f)
        fitted("SHARE",303f,fontSmall,1.72f,232f,757f)
    }

    private fun drawResult() {
        // Static illustration labels remain; the actual run values replace sample values.
        badge("cream",201f,H-947f,153f,90f)
        badge("cream",402f,H-947f,160f,90f)
        badge("cream",665f,H-947f,122f,90f)
        fitted(score.toString(),H-874f,fontLarge,1f,142f,277f,Color.valueOf("673A22"))
        fitted(routeBests[difficulty.ordinal].toString(),H-874f,fontLarge,1f,148f,483f,Color.valueOf("673A22"))
        fitted(earnedCoins.toString(),H-874f,fontLarge,1f,112f,726f,Color.valueOf("673A22"))
        val progress = min(1f,score.toFloat()/targetScore())
        badge("dark",293f,H-1100f,463f,57f)
        if(progress>.02f) badge("blue",299f,H-1095f,450f*progress,47f)
        fitted("$score / ${targetScore()}",H-1058f,fontSmall,1.6f,409f,526f)
        if(resultWon) {
            badge("green",315f,H-1432f,316f,176f)
            fitted(if(level<MAX_LEVEL) "NEXT" else "REPLAY",H-1350f,fontSmall,1.65f,300f,475f)
        }
    }

    private fun drawPauseToggles() {
        toggle(565f,H-1218f,musicEnabled)
        toggle(565f,H-1324f,effectsEnabled)
    }

    private fun drawSkins() {
        if(coins!=1250) {
            badge("wallet",120f,H-103f,164f,65f)
            fitted(SkyPeepHudNumbers.compact(coins),H-35f,fontSmall,1.28f,153f,199f)
        }
        if(gems!=89) {
            // Cover the source mockup's "89" with the correct brown wallet
            // face. Smaller type and a higher baseline keep live digits INSIDE.
            badge("wallet",733f,H-103f,111f,65f)
            fitted(SkyPeepHudNumbers.compact(gems),H-35f,fontSmall,1.28f,104f,788f)
        }
        if (selectedSkin!=0) {
            badge("cream",341f,H-821f,282f,68f)
            fitted(skinName(selectedSkin).uppercase(Locale.ROOT),H-775f,fontSmall,1.3f,265f,W/2f,Color.valueOf("603F2B"))
        }
        for (i in 1..4) {
            if (ownedSkins[i]) {
                val button=SkyPeepUiTouch.skinPurchaseRect(i)
                badge("green",button.left,H-button.top-button.height,button.width,button.height)
                fitted(if(selectedSkin==i) "EQUIPPED" else "EQUIP",H-1398f,fontSmall,1.1f,171f,button.left+button.width/2f)
            }
        }
    }

    /** Reuse the real illustrated aviator (and equipped costume) as a
     * decorative mascot on screens lacking a supplied baked-in bird panel.
     * Unlike a screenshot overlay, this stays a proper transparent sprite.
     */
    private fun drawMenuMascot(x:Float,y:Float,width:Float) {
        val bird=pictures["bird_idle"] ?: return
        val scale=width/BIRD_W
        val height=width*bird.height/bird.width
        batch.draw(bird,x,y,width,height)
        if(selectedSkin>0) costumes[selectedSkin]?.let { costume ->
            batch.draw(costume,
                x+SkyPeepBirdLayout.COSTUME_X*scale,
                y+height-SkyPeepBirdLayout.COSTUME_Y_FROM_TOP*scale,
                SkyPeepBirdLayout.COSTUME_WIDTH*scale,
                SkyPeepBirdLayout.COSTUME_HEIGHT*scale)
        }
    }

    private fun drawShop() {
        // Dedicated functional store, not the old SKINS screen duplicated.
        drawRect(0f,0f,W,H,Color(.04f,.13f,.30f,.72f))
        badge("cream",72f,180f,797f,1320f)
        badge("wood",117f,1365f,708f,122f)
        // Native bird illustration gives the SHOP its own SkyPeep identity,
        // as in the supplied PAUSE/SKINS design rather than a generic dialog.
        drawMenuMascot(383f,1481f,175f)
        fitted("FLIGHT TRAILS",1444f,fontLarge,1.28f,657f)
        badge("dark",118f,1252f,338f,90f)
        badge("dark",483f,1252f,338f,90f)
        fitted("COINS  $coins",1312f,fontSmall,1.38f,305f,288f)
        fitted("GEMS  $gems",1312f,fontSmall,1.38f,305f,653f)
        val tx=trailTexture
        for(offer in SkyPeepTrailCatalog.all) {
            val y=1060f-offer.id*195f
            badge("cream",99f,y,743f,174f)
            badge("wood",119f,y+24f,110f,122f)
            if(tx!=null) {
                batch.color=when(offer.id) {
                    1 -> Color.valueOf("43DBFF")
                    2 -> Color.valueOf("FFD145")
                    3 -> Color.valueOf("C078FF")
                    else -> Color.WHITE
                }
                batch.draw(tx,132f,y+48f,90f,74f)
                batch.color=Color.WHITE
            }
            fitted(offer.name.uppercase(Locale.ROOT),y+121f,fontSmall,1.10f,322f,410f,Color.valueOf("5B3526"))
            fitted(SkyPeepTrailCatalog.priceLabel(offer),y+66f,
                fontSmall,1.05f,322f,410f,Color.valueOf("734428"))
            val buyButton = SkyPeepUiTouch.shopPurchaseRect(offer.id)
            badge(if(selectedTrail==offer.id)"green" else if(ownedTrails[offer.id])"blue" else "wood",
                buyButton.left,H-buyButton.top-buyButton.height,buyButton.width,buyButton.height)
            val caption=when {
                selectedTrail==offer.id -> "EQUIPPED"
                ownedTrails[offer.id] -> "EQUIP"
                else -> "BUY"
            }
            fitted(caption,y+103f,fontSmall,1.18f,207f,706f)
        }
        badge("green",279f,245f,384f,137f)
        fitted("BACK",337f,fontLarge,1.22f,340f)
    }

    private fun drawSettings() {
        // Branded parchment and wooden controls, shared with the PAUSED art style.
        drawRect(0f,0f,W,H,Color(0f,.07f,.17f,.68f))
        badge("cream",91f,348f,759f,1020f)
        badge("wood",132f,1214f,677f,139f)
        drawMenuMascot(381f,1350f,179f)
        fitted("SETTINGS",1340f,fontLarge,1.55f,627f)
        val names=arrayOf("MUSIC","SOUND EFFECTS","VIBRATION","DIFFICULTY")
        for(i in names.indices) {
            val yy=1010f-i*143f
            badge("wood",137f,yy,666f,119f)
            label(names[i],162f,yy+77f,fontSmall)
            if(i<3) {
                toggle(657f,yy+28f,when(i) {
                    0 -> musicEnabled
                    1 -> effectsEnabled
                    else -> vibrationEnabled
                })
            } else fitted(difficulty.title,yy+77f,fontSmall,.90f,214f,686f)
        }
        badge("blue",137f,421f,310f,116f)
        fitted("BEST " + routeBests[difficulty.ordinal],494f,fontSmall,1.1f,285f,292f)
        badge("green",492f,421f,313f,116f)
        fitted("WORLDS " + unlocked + "/6",494f,fontSmall,1.3f,290f,649f)
        badge("green",185f,243f,575f,129f)
        fitted("BACK",352f,fontLarge,1.30f,531f)
    }

    /** Rounded, antialiased-like gradient chrome rendered once, reused on each screen. */
    private fun createVictoryStar(): Texture {
        // Render an antialiased-looking five-point star as one reusable texture.
        val side=128
        val pix=Pixmap(side,side,Pixmap.Format.RGBA8888)
        pix.setColor(0f,0f,0f,0f)
        pix.fill()
        fun star(cx:Int,cy:Int,outer:Int,inner:Int,tint:Color) {
            val xx=IntArray(10)
            val yy=IntArray(10)
            for(i in 0..9) {
                val angle=-Math.PI/2+i*Math.PI/5
                val r=if(i%2==0)outer else inner
                xx[i]=cx+(kotlin.math.cos(angle)*r).toInt()
                yy[i]=cy+(kotlin.math.sin(angle)*r).toInt()
            }
            pix.setColor(tint)
            for(i in 0..9) {
                val next=(i+1)%10
                pix.fillTriangle(cx,cy,xx[i],yy[i],xx[next],yy[next])
            }
        }
        star(64,67,59,29,Color.valueOf("49220F"))
        star(64,61,56,27,Color.valueOf("FFB52D"))
        star(60,56,47,22,Color.valueOf("FFE34D"))
        val tx=Texture(pix)
        tx.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear)
        pix.dispose()
        return tx
    }

    private fun prepareChrome() {
        val palette=mapOf(
            "dark" to Color.valueOf("103963"),
            "green" to Color.valueOf("48C62D"),
            "cream" to Color.valueOf("F6E4C4"),
            "wood" to Color.valueOf("92552F"),
            "wallet" to Color.valueOf("573729"),
            "blue" to Color.valueOf("169CE8"),
            "red" to Color.valueOf("C83A40")
        )
        for ((name,base) in palette) {
            val pix=Pixmap(192,80,Pixmap.Format.RGBA8888)
            pix.setColor(0f,0f,0f,0f)
            pix.fill()
            // Render the same premium illustration treatment used by
            // reference PLAY/PAUSE buttons: thick chocolate silhouette,
            // coloured inset bevel, broad glossy shoulder and shaded base.
            // The border is antialiased and the patch remains 9-slice safe.
            for(y in 0 until 80) for(x in 0 until 192) {
                val dx=max(0f,kotlin.math.abs(x-95.5f)-64.5f)
                val dy=max(0f,kotlin.math.abs(y-39.5f)-9.5f)
                val d=kotlin.math.sqrt(dx*dx+dy*dy)
                if(d>30f) continue
                val edgeAlpha=((30f-d)/1.65f).coerceIn(0f,1f)
                val vertical=y/79f
                val c=when {
                    d>=25f -> Color(.17f,.075f,.055f,edgeAlpha)
                    d>=22f -> Color(.39f,.20f,.12f,edgeAlpha)
                    d>=19f -> Color(
                        (base.r*.66f).coerceIn(0f,1f),
                        (base.g*.61f).coerceIn(0f,1f),
                        (base.b*.57f).coerceIn(0f,1f),edgeAlpha)
                    else -> {
                        var brightness=1.19f-.29f*vertical
                        if(y<=17) brightness+=.07f
                        if(y>=66) brightness-=.12f
                        if(y in 13..17 && x in 28..154)
                            brightness+=.085f
                        if(d>=15f) brightness-=.045f
                        Color((base.r*brightness).coerceIn(0f,1f),
                              (base.g*brightness).coerceIn(0f,1f),
                              (base.b*brightness).coerceIn(0f,1f),edgeAlpha)
                    }
                }
                pix.drawPixel(x,y,Color.rgba8888(c))
            }
            val image=Texture(pix).also {
                it.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear)
            }
            chromeTextures[name]=image
            chrome[name]=com.badlogic.gdx.graphics.g2d.NinePatch(image,32,32,32,32)
            pix.dispose()
        }
    }

    private fun badge(name:String,x:Float,y:Float,width:Float,height:Float) {
        if(width<=0f || height<=0f) return
        if(width>=64f && height>=64f) chrome[name]?.draw(batch,x,y,width,height)
        else chromeTextures[name]?.let { batch.draw(it,x,y,width,height) }
    }

    private fun toggle(x:Float,y:Float,enabled:Boolean) {
        badge(if(enabled) "green" else "red",x,y,112f,61f)
        badge("cream",x+(if(enabled) 57f else 7f),y+7f,48f,47f)
    }

    private fun fitted(value:String,baseline:Float,font:BitmapFont,size:Float,
                       available:Float,centerX:Float=W/2f,tint:Color=Color.WHITE) {
        val savedScale=font.data.scaleX
        val savedColor=Color(font.color)
        font.data.setScale(size)
        tmp.setText(font,value)
        if(tmp.width>available) font.data.setScale(size*available/tmp.width)
        tmp.setText(font,value)
        font.color=tint
        font.draw(batch,value,centerX-tmp.width/2f,baseline)
        font.color=savedColor
        font.data.setScale(savedScale)
    }

    private fun drawRect(x:Float,y:Float,w:Float,h:Float,color:Color) {
        if (w<=0f || h<=0f) return
        batch.color=color
        batch.draw(solid,x,y,w,h)
        batch.color=Color.WHITE
    }

    private fun label(text:String,x:Float,y:Float,font:BitmapFont) {
        font.draw(batch,text,x,y)
    }

    private fun toast(value:String) {
        message=value
        messageTimer=2.3f
    }
    private fun levelTitle(i:Int)=arrayOf("Sky Start","Cloud Pass","Crystal Drop",
        "Wind Gate","Moon Glow","Sun Peak")[i.coerceIn(1,MAX_LEVEL)-1]
    private fun skinName(i:Int)=SkyPeepSkinCatalog.get(i).name
    private fun playSound(kind:String) {
        if (effectsEnabled) try { sounds[kind]?.play(.45f) } catch (_:Exception) { }
    }
    private fun save() {
        prefs.putInteger("best",best)
        prefs.putInteger("difficulty",difficulty.ordinal)
        for (mode in SkyDifficulty.entries) {
            prefs.putInteger("route_best_${mode.ordinal}",routeBests[mode.ordinal])
            for (i in 0 until MAX_LEVEL)
                prefs.putInteger("route_stars_${mode.ordinal}_${i+1}",
                    routeStars[mode.ordinal][i])
        }
        prefs.putInteger("unlocked",unlocked)
        prefs.putInteger("level",level)
        prefs.putInteger("coins",coins)
        prefs.putInteger("gems",gems)
        prefs.putInteger("skin",selectedSkin)
        prefs.putInteger("trail_selected",selectedTrail)
        for (i in 1..3) prefs.putBoolean("trail_owned_$i",ownedTrails[i])
        prefs.putBoolean("music",musicEnabled)
        prefs.putBoolean("effects",effectsEnabled)
        prefs.putBoolean("vibration",vibrationEnabled)
        for (i in 0..4) prefs.putBoolean("owned_$i",ownedSkins[i])
        for (i in 0 until MAX_LEVEL) prefs.putInteger("stars_${i+1}",stars[i])
        prefs.flush()
    }
    override fun pause() {
        // App backgrounding must never leave a live run progressing behind a system UI.
        if (screen == Screen.PLAY) screen = Screen.PAUSE
        save()
        music?.pause()
    }
    override fun resume() { if (musicEnabled) music?.play() }
    override fun dispose() {
        save()
        music?.dispose()
        sounds.values.forEach { it.dispose() }
        pictures.values.forEach { it.dispose() }
        if (::fontSmall.isInitialized)fontSmall.dispose()
        if (::fontLarge.isInitialized)fontLarge.dispose()
        if (::solid.isInitialized)solid.dispose()
        chromeTextures.values.forEach { it.dispose() }
        costumes.values.forEach { it.dispose() }
        victoryStar?.dispose()
        pillarStem?.dispose()
        trailTexture?.dispose()
        if (::batch.isInitialized)batch.dispose()
    }
}