package com.brendigo.skypeep

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.math.MathUtils

/** Initial playable flight loop; full illustrated level assets and menus are being ported. */
class SkyPeepGame : ApplicationAdapter() {
    private lateinit var shapes: ShapeRenderer
    private val camera = OrthographicCamera()
    private var y = 480f
    private var velocity = 0f
    private var obstacleX = 650f
    private var gapY = 480f
    private var score = 0
    private var playing = false
    private val width = 540f
    private val height = 960f
    private val birdX = 145f
    private val radius = 22f
    private val obstacleWidth = 76f
    private val gapHeight = 235f
    private var counted = false
    override fun create() {
        shapes = ShapeRenderer()
        camera.setToOrtho(false, width, height)
        camera.update()
        reset()
    }
    private fun reset() {
        y = 480f; velocity = 0f; obstacleX = 650f; gapY = 480f
        score = 0; counted = false; playing = false
    }
    override fun render() {
        val delta = Gdx.graphics.deltaTime.coerceAtMost(1f / 30f)
        if (Gdx.input.justTouched()) {
            if (!playing && (y < radius || y > height - radius || collision())) reset()
            playing = true
            velocity = 340f
        }
        if (playing) {
            velocity -= 870f * delta
            y += velocity * delta
            obstacleX -= (190f + score.coerceAtMost(40) * 3f) * delta
            if (!counted && obstacleX + obstacleWidth < birdX) { score++; counted = true }
            if (obstacleX + obstacleWidth < 0f) {
                obstacleX = width + 50f
                gapY = MathUtils.random(300f, 660f)
                counted = false
            }
            if (y < radius || y > height - radius || collision()) playing = false
        }
        ScreenUtils.clear(0.24f, 0.69f, 0.96f, 1f)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.98f, 0.86f, 0.56f, 1f)
        shapes.rect(obstacleX, 0f, obstacleWidth, gapY - gapHeight / 2f)
        shapes.rect(obstacleX, gapY + gapHeight / 2f, obstacleWidth, height - gapY - gapHeight / 2f)
        shapes.setColor(0.04f, 0.73f, 0.87f, 1f)
        shapes.circle(birdX, y, radius, 32)
        shapes.setColor(1f, 0.94f, 0.81f, 1f)
        shapes.circle(birdX + 9f, y - 4f, 7f, 18)
        shapes.end()
    }
    private fun collision(): Boolean {
        val intersectsX = birdX + radius > obstacleX && birdX - radius < obstacleX + obstacleWidth
        return intersectsX && (y - radius < gapY - gapHeight / 2f || y + radius > gapY + gapHeight / 2f)
    }
    override fun resize(w: Int, h: Int) {
        camera.setToOrtho(false, width, height)
        camera.update()
    }
    override fun dispose() { shapes.dispose() }
}
