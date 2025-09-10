package com.example.game2d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF


class TileMap(private val ctx: Context) {

    val worldWidth = 1600f
    val worldHeight = 720f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)


    private val groundTopY = worldHeight / 2f


    private val platforms = ArrayList<RectF>()

    data class Pickup(var x: Float, var y: Float, var collected: Boolean = false)
    private val pickups = ArrayList<Pickup>()

    init {

        platforms.add(RectF(380f, groundTopY - 160f, 520f, groundTopY - 140f))
        platforms.add(RectF(600f, groundTopY - 100f, 740f, groundTopY - 80f))
        platforms.add(RectF(820f, groundTopY - 160f, 960f, groundTopY - 140f))
        platforms.add(RectF(1060f, groundTopY - 60f, 1200f, groundTopY - 40f))


        pickups.add(Pickup(460f, groundTopY - 180f))
        pickups.add(Pickup(640f, groundTopY - 120f))
        pickups.add(Pickup(900f, groundTopY - 180f))
    }


    fun getGroundTopY(): Float = groundTopY


    fun draw(canvas: Canvas) {
        // sky (top half)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(120, 200, 255)
        canvas.drawRect(0f, 0f, worldWidth, groundTopY, paint)

        // ground base
        paint.color = Color.rgb(110, 70, 30)
        canvas.drawRect(0f, groundTopY, worldWidth, worldHeight, paint)

        // turf top strip
        paint.color = Color.rgb(75, 160, 60)
        canvas.drawRect(0f, groundTopY, worldWidth, groundTopY + 24f, paint)

        // platforms
        paint.color = Color.rgb(90, 140, 70)
        for (p in platforms) {
            canvas.drawRoundRect(p, 8f, 8f, paint)
            // shadow/soil under platform
            paint.color = Color.rgb(130, 85, 45)
            canvas.drawRect(p.left, p.bottom - 6f, p.right, p.bottom + 8f, paint)
            paint.color = Color.rgb(90, 140, 70)
        }

        // decorative frame / borders
        paint.color = Color.rgb(30, 24, 40)
        canvas.drawRect(-20f, -20f, worldWidth + 20f, 28f, paint)
        canvas.drawRect(-20f, worldHeight - 28f, worldWidth + 20f, worldHeight + 20f, paint)
        canvas.drawRect(-20f, -20f, 28f, worldHeight + 20f, paint)
        canvas.drawRect(worldWidth - 28f, -20f, worldWidth + 20f, worldHeight + 20f, paint)

        // pickups
        paint.color = Color.YELLOW
        for (pk in pickups) {
            if (!pk.collected) canvas.drawCircle(pk.x, pk.y, 10f, paint)
        }
    }


    fun resolvePlayerCollision(player: Player): Boolean {
        val rect = player.getRect()
        var collided = false

        if (rect.top < 0f) {
            player.y = 0f
            if (player.vy < 0f) player.vy = 0f
            collided = true
        }

        // clamp horizontal inside world
        if (player.x < 0f) { player.x = 0f; player.vx = 0f }
        if (player.x + player.width > worldWidth) { player.x = worldWidth - player.width; player.vx = 0f }

        // ground top collision: if player's bottom is below groundTopY, land on ground top
        if (rect.bottom > groundTopY) {
            if (player.vy >= 0f) {
                player.y = groundTopY - player.height
                player.vy = 0f
                collided = true
            }
        }

        // platform collisions (top only, simple)
        for (p in platforms) {
            if (rect.right > p.left && rect.left < p.right) {
                if (player.vy > 0f && rect.bottom > p.top && rect.top < p.top) {
                    player.y = p.top - player.height
                    player.vy = 0f
                    collided = true
                }
            }
        }

        // world bottom clamp
        if (rect.bottom > worldHeight) {
            player.y = worldHeight - player.height
            player.vy = 0f
            collided = true
        }

        // pickups
        val it = pickups.iterator()
        while (it.hasNext()) {
            val pk = it.next()
            if (!pk.collected) {
                val dx = (player.x + player.width / 2f) - pk.x
                val dy = (player.y + player.height / 2f) - pk.y
                if (dx * dx + dy * dy < 30f * 30f) pk.collected = true
            }
        }

        return collided
    }
}
