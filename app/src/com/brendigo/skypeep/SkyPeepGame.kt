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
import com.badlogic.gdx.utils.viewport.FitViewport
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** All locations refer to the original 941x1672 portrait artwork. No scrolling. */
class SkyPeepGame(private val shareText: ((String) -> Unit)? = null) : ApplicationAdapter() {
    companion object {
        private const val W = 941f
        private const val H = 1672f
        private const val BIRD_X = 172f
        private const val BIRD_W = 245f
        private const val BIRD_H = 215f
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
    private lateinit var viewport: FitViewport
    private lateinit var prefs: Preferences
    private val pictures = mutableMapOf<String, Texture>()
    private var loadedScreenArt: String? = null
    private var loadedLevelArt = 0
    private lateinit var fontSmall: BitmapFont
    private lateinit var fontLarge: BitmapFont
    private lateinit var solid: Texture
    private val chrome = mutableMapOf<String, Texture>()
    private var music: Music? = null
    private val sounds = mutableMapOf<String, Sound>()
    private val tmp = GlyphLayout()
    private val touch = Vector2()

    private var screen = Screen.SPLASH
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
    private var musicEnabled = true
    private var effectsEnabled = true
    private var vibrationEnabled = false
    private var birdY = 815f
    private var verticalSpeed = 0f
    private var flapTimer = 0f
    private var readyForFirstTap = false
    private var animationTime = 0f
    private var elapsed = 0f
    private var nextGate = 1.5f
    private val gates = mutableListOf<Gate>()
    private var message = ""
    private var messageTimer = 0f

    override fun create() {
        batch = SpriteBatch()
        camera = OrthographicCamera()
        viewport = FitViewport(W, H, camera)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        batch.projectionMatrix = camera.combined
        prefs = Gdx.app.getPreferences("SkyPeepSettingsAndProgress")
        best = prefs.getInteger("best", 0)
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
        loadedLevelArt = newLevel
    }

    private fun levelArtNames(world: Int): List<String> =
        listOf("bg_$world", "pillar_${world}_top", "pillar_${world}_bottom") +
            listOf("left", "score", "right", "pause").map { "hud_${it}_$world" }

    private fun syncScreenArt() {
        // Reuse the current illustration while in the same screen, and dispose
        // the previous full-screen JPG before switching to another UI page.
        val required = SkyPeepAssets.screenFor(screen.name)
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
        viewport.update(width, height, true)
        camera.update()
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime.coerceIn(0f, .05f)
        animationTime += delta
        if (messageTimer > 0f) messageTimer -= delta
        handleInput()
        if (screen == Screen.PLAY) updateGame(delta)
        syncScreenArt()
        Gdx.gl.glClearColor(.16f,.67f,.96f,1f)
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        when (screen) {
            Screen.SPLASH -> image("screen_splash", 0f, 0f, W, H)
            Screen.MENU -> { image("screen_menu", 0f, 0f, W, H); drawMenuWallet() }
            Screen.MAP -> { image("screen_map", 0f, 0f, W, H); drawMapProgress() }
            Screen.PLAY -> drawGameplay()
            Screen.PAUSE -> { image("screen_pause", 0f, 0f, W, H); drawPauseToggles() }
            Screen.RESULT -> { image("screen_over", 0f, 0f, W, H); drawResult() }
            Screen.SKINS, Screen.SHOP -> { image("screen_skins", 0f, 0f, W, H); drawSkins() }
            Screen.SETTINGS -> { image("screen_menu", 0f, 0f, W, H); drawSettings() }
        }
        if (messageTimer > 0 && screen != Screen.PLAY) {
            badge("dark",110f,1375f,W-220f,103f)
            fitted(message,1437f,fontSmall,1.4f,W-290f)
        }
        batch.end()
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
        if (touch.x < 0 || touch.x > W || touch.y < 0 || touch.y > H) return
        val x = touch.x
        val yTop = H - touch.y
        when (screen) {
            Screen.SPLASH -> { screen = Screen.MENU; playSound("click") }
            Screen.MENU -> clickMenu(x, yTop)
            Screen.MAP -> clickMap(x, yTop)
            Screen.PLAY -> {
                if (hit(x,yTop,806f,0f,135f,155f)) { screen = Screen.PAUSE; playSound("click") }
                else flap()
            }
            Screen.PAUSE -> clickPause(x,yTop)
            Screen.RESULT -> clickResult(x,yTop)
            Screen.SKINS, Screen.SHOP -> clickSkins(x,yTop)
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
            hit(x,y,5f,1435f,200f,237f) -> screen = Screen.MENU
            hit(x,y,580f,1450f,360f,220f) -> begin(level)
            else -> {
                val pos = arrayOf(
                    floatArrayOf(260f,1260f,650f,1490f),
                    floatArrayOf(540f,1000f,910f,1270f),
                    floatArrayOf(120f,795f,500f,1080f),
                    floatArrayOf(490f,585f,880f,864f),
                    floatArrayOf(135f,315f,530f,632f),
                    floatArrayOf(545f,195f,915f,498f)
                )
                for (i in pos.indices) {
                    val p=pos[i]
                    if (hit(x,y,p[0],p[1],p[2]-p[0],p[3]-p[1])) {
                        if (i+1 <= unlocked) { level=i+1; save(); toast("SELECTED: ${levelTitle(level)}") }
                        else toast("COMPLETE PREVIOUS LEVEL")
                        return
                    }
                }
            }
        }
    }

    private fun clickPause(x: Float, y: Float) {
        playSound("click")
        when {
            hit(x,y,220f,640f,500f,163f) -> screen = Screen.PLAY
            hit(x,y,220f,798f,500f,158f) -> begin(level)
            hit(x,y,220f,951f,500f,158f) -> { screen = Screen.MENU; save() }
            hit(x,y,200f,1120f,545f,126f) -> setMusic(!musicEnabled)
            hit(x,y,200f,1240f,545f,126f) -> { effectsEnabled = !effectsEnabled; save() }
        }
    }

    private fun clickResult(x: Float, y: Float) {
        playSound("click")
        if (y in 1195f..1505f) {
            when {
                x < 305f -> { screen = Screen.MENU; save() }
                x < 628f -> {
                    val nextLevel = if (resultWon && level < MAX_LEVEL) level + 1 else level
                    begin(nextLevel)
                }
                else -> {
                    val msg = "SkyPeep | ${levelTitle(level)} | Score $score | Best $best"
                    if (shareText != null) shareText.invoke(msg)
                    else Gdx.app.clipboard.contents = msg
                    toast("SHARE RESULT")
                }
            }
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
        if (hit(x,y,0f,1445f,210f,227f)) { screen = Screen.MENU; return }
        if (hit(x,y,285f,810f,398f,120f)) { selectedSkin = 0; save(); toast("DEFAULT EQUIPPED"); return }
        if (y !in 1010f..1480f) return
        val skin = when {
            x < 233f -> 1
            x < 470f -> 2
            x < 708f -> 3
            else -> 4
        }
        if (ownedSkins[skin]) {
            selectedSkin = skin
            save()
            toast("${skinName(skin)} EQUIPPED")
            return
        }
        val costs = intArrayOf(0,500,800,120,1000)
        val useGem = skin == 3
        val price=costs[skin]
        if ((if (useGem) gems else coins) < price) {
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

    private fun clickSettings(x: Float, y: Float) {
        playSound("click")
        when {
            hit(x,y,160f,1145f,630f,165f) -> { screen=Screen.MENU; save() }
            hit(x,y,120f,555f,710f,118f) -> setMusic(!musicEnabled)
            hit(x,y,120f,698f,710f,117f) -> { effectsEnabled=!effectsEnabled; save() }
            hit(x,y,120f,840f,710f,120f) -> { vibrationEnabled=!vibrationEnabled; save() }
            hit(x,y,120f,980f,710f,127f) -> { toast("LANGUAGE: ENGLISH") }
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
        elapsed=0f
        nextGate=2.0f
        gates.clear()
        verticalSpeed=0f
        birdY=840f
        flapTimer=0f
        animationTime=0f
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
        if (readyForFirstTap) return
        elapsed += dt
        flapTimer = max(0f,flapTimer-dt)
        verticalSpeed = max(-900f, verticalSpeed - 1720f*dt)
        birdY += verticalSpeed *dt
        if (birdY < 38f || birdY + BIRD_H > H - 205f) { finish(false); return }
        val gap = FlightRules.gap(level)
        val moveSpeed = 235f + (level-1)*22f
        nextGate -= dt
        if (nextGate <= 0f) {
            nextGate= max(1.38f, 2.25f-(level-1)*.11f)
            val center = MathUtils.random(570f,1110f)
            gates.add(Gate(W+80f,center,gem=(score>0 && MathUtils.random(0,9)==0)))
        }
        val iterator=gates.iterator()
        while (iterator.hasNext()) {
            val gate=iterator.next()
            gate.x -= moveSpeed * dt
            val gateWidth=GATE_WIDTH
            val topLimit=gate.centerY+gap*.5f
            val bottomLimit=gate.centerY-gap*.5f
            val hitLeft = BIRD_X + 48f
            val hitRight = BIRD_X + BIRD_W - 53f
            val hitBottom=birdY+52f
            val hitTop=birdY+BIRD_H-55f
            if (FlightRules.intersectsObstacle(
                    gateX = gate.x, gateWidth = gateWidth,
                    gapBottom = bottomLimit, gapTop = topLimit,
                    birdLeft = hitLeft, birdRight = hitRight,
                    birdBottom = hitBottom, birdTop = hitTop
                )) {
                finish(false)
                return
            }
            if (!gate.coinTaken && gate.x+gateWidth*.5f<BIRD_X+BIRD_W &&
                gate.x+gateWidth*.5f>BIRD_X &&
                kotlin.math.abs((birdY+BIRD_H*.5f)-gate.centerY)<125f) {
                gate.coinTaken=true
                coins+=5
                earnedCoins+=5
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
                gems++
                playSound("coin")
            }
            if (!gate.scored && gate.x+gateWidth < BIRD_X+40f) {
                gate.scored=true
                score++
                best=max(best,score)
                if (score >= targetScore()) { finish(true); return }
            }
            if (gate.x < -400f) iterator.remove()
        }
    }

    private fun targetScore() = FlightRules.targetScore(level)
    private fun finish(won: Boolean) {
        resultWon = won
        screen = Screen.RESULT
        best=max(best,score)
        if (won) {
            coins += 100 + level * 15
            earnedCoins += 100 + level * 15
            stars[level-1]=3
            unlocked=max(unlocked,min(MAX_LEVEL,level+1))
            playSound("win")
        } else {
            playSound("fail")
            if (vibrationEnabled) try { Gdx.input.vibrate(40) } catch (_:Exception) { }
        }
        save()
    }

    private fun drawGameplay() {
        image("bg_$level",0f,0f,W,H)
        val gap = FlightRules.gap(level)
        for (gate in gates) {
            val top = pictures["pillar_${level}_top"]!!
            val bottom = pictures["pillar_${level}_bottom"]!!
            val topY = gate.centerY+gap*.5f
            val bottomY = gate.centerY-gap*.5f
            val pillarWidth=GATE_WIDTH
            // The pole stems are rendered behind the photographed caps as scalable 2D shapes.
            val tint = when(level) {
                3 -> Color(.14f,.27f,.43f,1f)
                5 -> Color(.25f,.16f,.48f,1f)
                6 -> Color(.90f,.76f,.47f,1f)
                else -> Color(.89f,.84f,.72f,1f)
            }
            drawRect(gate.x+39f, topY+top.height*.95f, pillarWidth-72f,
                     max(0f,H-(topY+top.height*.95f)),tint)
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
        drawBird()
        drawHUD()
        if (readyForFirstTap) {
            drawRect(195f, 345f, 555f, 96f, Color(.08f,.23f,.42f,.88f))
            centered("TAP TO FLY", 410f, fontLarge)
        }
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
        batch.draw(tx,BIRD_X,birdY,BIRD_W,BIRD_H)
        batch.color=Color.WHITE
    }

    private fun drawHUD() {
        // Exact cropped reference HUD layers. Dynamic figures replace reference mockup values.
        image("hud_left_$level", 14f, H-123f, 269f, 113f)
        image("hud_right_$level", 635f, H-124f, 191f, 115f)
        image("hud_pause_$level", 811f, H-129f, 125f, 122f)
        image("hud_score_$level", 286f, H-218f, 367f, 210f)
        // Cover only the numeric text within the reference plates.
        badge("dark",126f,H-100f,113f,63f)
        fitted(coins.toString(),H-55f,fontSmall,1.9f,103f,182f)
        badge("dark",739f,H-99f,60f,63f)
        fitted(gems.toString(),H-53f,fontSmall,1.9f,55f,767f)
        badge("score",387f,H-133f,160f,101f)
        fitted(score.toString(),H-59f,fontLarge,1.20f,153f)
    }

    private fun drawCoin(x:Float,y:Float,r:Float) {
        image("coin_asset",x-r,y-r,r*2f,r*2f)
    }

    private fun drawDiamond(x:Float,y:Float,r:Float) {
        image("gem_asset",x-r,y-r,r*2f,r*2f)
    }

    private fun drawMenuWallet() {
        // Cover only screenshot mockup's sample numeric labels, retaining the original visual buttons.
        badge("dark",119f,H-103f,136f,65f)
        fitted(coins.toString(),H-54f,fontSmall,1.9f,127f,186f)
        badge("dark",743f,H-103f,76f,65f)
        fitted(gems.toString(),H-54f,fontSmall,1.9f,69f,781f)
    }

    private fun drawMapProgress() {
        // Place unlocked tags on top of the fixed reference locks, without changing the map artwork.
        val badges=arrayOf(floatArrayOf(705f,1112f),floatArrayOf(345f,903f),
            floatArrayOf(670f,695f),floatArrayOf(360f,450f),floatArrayOf(733f,278f))
        for (i in 2..unlocked) {
            val p=badges[i-2]
            badge("green",p[0]-47f,H-p[1]-42f,94f,78f)
            fitted("OK",H-p[1]+22f,fontSmall,1.3f,75f,p[0])
        }
        // The selected level is indicated when the user taps its island. The background map
        // is deliberately unobstructed, and PLAY always uses the selected unlocked level.
    }

    private fun drawResult() {
        if (resultWon) {
            drawRect(150f,H-763f,643f,287f,Color(.99f,.89f,.72f,.99f))
            centered("LEVEL COMPLETE",H-605f,fontLarge)
            centered("3 STARS",H-708f,fontSmall)
        }
        // Static illustration labels remain; the actual run values replace sample values.
        val panel=Color(.97f,.90f,.78f,1f)
        drawRect(201f,H-947f,157f,95f,panel)
        drawRect(399f,H-947f,165f,95f,panel)
        drawRect(673f,H-947f,115f,95f,panel)
        label(score.toString(),208f,H-873f,fontLarge)
        label(best.toString(),410f,H-873f,fontLarge)
        label(earnedCoins.toString(),668f,H-873f,fontLarge)
        val progress = min(1f, score.toFloat() / targetScore())
        drawRect(300f,H-1100f,455f,53f,Color(.42f,.43f,.48f,1f))
        drawRect(302f,H-1097f,451f*progress,46f,Color(.12f,.58f,.93f,1f))
        centered("$score / ${targetScore()}",H-1059f,fontSmall,526f)
        if (resultWon) {
            drawRect(319f,H-1438f,310f,182f,Color(.12f,.75f,.22f,.55f))
            centered(if(level<MAX_LEVEL) "NEXT" else "REPLAY",H-1350f,fontSmall,475f)
        }
    }

    private fun drawPauseToggles() {
        toggle(565f,H-1218f,musicEnabled)
        toggle(565f,H-1324f,effectsEnabled)
    }

    private fun drawSkins() {
        badge("wood",120f,H-103f,164f,65f)
        fitted(coins.toString(),H-56f,fontSmall,1.8f,153f,199f)
        badge("wood",733f,H-103f,111f,65f)
        fitted(gems.toString(),H-56f,fontSmall,1.8f,104f,788f)
        if (selectedSkin!=0) {
            badge("cream",341f,H-821f,282f,68f)
            fitted(skinName(selectedSkin).uppercase(Locale.ROOT),H-775f,fontSmall,1.3f,265f,W/2f,Color.valueOf("603F2B"))
        }
        for (i in 1..4) {
            if (ownedSkins[i]) {
                val x=(i-1)*235f+25f
                badge("green",x,H-1456f,188f,94f)
                fitted(if(selectedSkin==i) "EQUIPPED" else "EQUIP",H-1398f,fontSmall,1.1f,171f,x+94f)
            }
        }
    }

    private fun drawSettings() {
        drawRect(0f,0f,W,H,Color(0f,.05f,.13f,.72f))
        drawRect(86f,390f,769f,1010f,Color(.95f,.89f,.75f,.98f))
        drawRect(106f,1240f,728f,142f,Color(.20f,.55f,.90f,1f))
        centered("SETTINGS",1315f,fontLarge)
        val names=arrayOf("MUSIC", "SOUND EFFECTS", "VIBRATION", "LANGUAGE")
        val value=arrayOf(if(musicEnabled) "ON" else "OFF",if(effectsEnabled) "ON" else "OFF",
            if(vibrationEnabled) "ON" else "OFF", "ENGLISH")
        for (i in names.indices) {
            val y=1003f-i*143f
            drawRect(135f,y,665f,110f,Color(.11f,.36f,.67f,.99f))
            label(names[i],160f,y+72f,fontSmall)
            label(value[i],if(i==3) 610f else 690f,y+72f,fontSmall)
        }
        drawRect(195f,355f,553f,140f,Color(.22f,.70f,.25f,1f))
        centered("BACK",446f,fontLarge)
    }

    /** Rounded, antialiased-like gradient chrome rendered once, reused on each screen. */
    private fun prepareChrome() {
        val palette=mapOf(
            "dark" to Color.valueOf("103963"),
            "score" to Color.valueOf("1B8FDC"),
            "green" to Color.valueOf("48C62D"),
            "cream" to Color.valueOf("F6E4C4"),
            "wood" to Color.valueOf("92552F"),
            "blue" to Color.valueOf("169CE8"),
            "red" to Color.valueOf("C83A40")
        )
        for ((name,base) in palette) {
            val pix=Pixmap(192,80,Pixmap.Format.RGBA8888)
            pix.setColor(0f,0f,0f,0f)
            pix.fill()
            for(y in 0 until 80) for(x in 0 until 192) {
                val dx=max(0f,kotlin.math.abs(x-95.5f)-64.5f)
                val dy=max(0f,kotlin.math.abs(y-39.5f)-9.5f)
                val d=kotlin.math.sqrt(dx*dx+dy*dy)
                if(d>30f) continue
                val c=if(d>26f) Color(.09f,.14f,.22f,1f) else {
                    val brightness=if(y<20) 1.12f else if(y>65) .88f else 1f
                    Color((base.r*brightness).coerceIn(0f,1f),
                          (base.g*brightness).coerceIn(0f,1f),
                          (base.b*brightness).coerceIn(0f,1f),1f)
                }
                pix.drawPixel(x,y,Color.rgba8888(c))
            }
            chrome[name]=Texture(pix).also {
                it.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear)
            }
            pix.dispose()
        }
    }

    private fun badge(name:String,x:Float,y:Float,width:Float,height:Float) {
        if(width<=0f || height<=0f) return
        chrome[name]?.let { batch.draw(it,x,y,width,height) }
    }

    private fun toggle(x:Float,y:Float,enabled:Boolean) {
        badge(if(enabled) "green" else "red",x,y,112f,61f)
        badge("cream",x+if(enabled) 57f else 7f,y+7f,48f,47f)
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

    private fun centered(text:String,y:Float,font:BitmapFont,centerX:Float=W/2f) {
        tmp.setText(font,text)
        font.draw(batch,text,centerX-tmp.width/2,y)
    }

    private fun toast(value:String) {
        message=value
        messageTimer=2.3f
    }
    private fun levelTitle(i:Int)=arrayOf("Sky Start","Cloud Pass","Crystal Drop",
        "Wind Gate","Moon Glow","Sun Peak")[i.coerceIn(1,MAX_LEVEL)-1]
    private fun skinName(i:Int)=arrayOf("Default","Captain","Forest","Cosmic","Royal")[i.coerceIn(0,4)]
    private fun playSound(kind:String) {
        if (effectsEnabled) try { sounds[kind]?.play(.45f) } catch (_:Exception) { }
    }
    private fun save() {
        prefs.putInteger("best",best)
        prefs.putInteger("unlocked",unlocked)
        prefs.putInteger("level",level)
        prefs.putInteger("coins",coins)
        prefs.putInteger("gems",gems)
        prefs.putInteger("skin",selectedSkin)
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
        chrome.values.forEach { it.dispose() }
        if (::batch.isInitialized)batch.dispose()
    }
}