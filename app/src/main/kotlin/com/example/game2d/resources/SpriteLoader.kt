package com.example.game2d.resources

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

object SpriteLoader {
    private val single = HashMap<String, Bitmap?>()
    private val multi  = HashMap<String, Array<Bitmap?>>()   // frames nullable

    fun get(name: String): Bitmap? = single[name]
    fun getFrames(name: String): Array<Bitmap?> = multi[name] ?: emptyArray()
    fun getFramesCount(name: String): Int = multi[name]?.size ?: 0

    fun preloadDefaults(ctx: Context) {
        // Obstacles
        load(ctx, "spike")
        load(ctx, "saw")
        load(ctx, "checkpoint_pole")
        loadFramesFromSheet(ctx, "checkpoint_flag_out", 26)
        loadFramesFromSheet(ctx, "checkpoint_flag_idle", 10)
        // --- Monster2 sprites ---
        loadFramesFromSheet(ctx, "monster2_run", 6)
        loadFramesFromSheet(ctx, "monster2_idle", 11)
        loadFramesFromSheet(ctx, "monster2_hitwall", 4)
        loadFramesFromSheet(ctx, "monster2_hit", 5)

        val fruits = listOf("strawberry","apple","cherry","orange","banana")
        fun tryLoadFramesFor(ctx: Context, name: String): Boolean {
            val tries = listOf(17)
            for (c in tries) {
                if (loadFramesFromSheet(ctx, name, c)) return true
            }
            return false
        }
        fruits.forEach { name ->
            if (!tryLoadFramesFor(ctx, name)) {
                load(ctx, name) // fallback single image if no matching sheet pattern
            }
        }

        // Enemy pig
        if (!loadFramesByFiles(ctx, "pig", 6)) {
            loadFramesFromSheet(ctx, "pig", 6)
        }

        // --- NEW: bullet sprites ---
        load(ctx, "bullet") // viên đạn đang bay
        // các frame "vỡ": bullet_hit_1, bullet_hit_2 ... (tự động bỏ qua nếu thiếu)
        if (!loadFramesByFiles(ctx, "bullet_hit", 4)) { // thử 2-4 khung
            // fallback thử tên khác mà bạn có thể đặt
            loadFramesByFiles(ctx, "bullet_break", 4)
            loadFramesByFiles(ctx, "bullet_burst", 4)
        }
    }

    fun load(ctx: Context, name: String) {
        loadBitmap(ctx, name)?.let { single[name] = it }
    }

    fun loadFramesByFiles(ctx: Context, base: String, count: Int): Boolean {
        val arr = Array<Bitmap?>(count) { null }
        var ok = false
        for (i in 0 until count) {
            val candidates = listOf("${base}_${i+1}", "${base}_${String.format("%02d", i+1)}", "${base}${i+1}")
            var bmp: Bitmap? = null
            for (nm in candidates) {
                bmp = loadBitmap(ctx, nm)
                if (bmp != null) break
            }
            arr[i] = bmp
            ok = ok || (bmp != null)
        }
        if (ok) multi[base] = arr
        return ok
    }

    fun loadFramesFromSheet(ctx: Context, name: String, count: Int): Boolean {
        val sheet = loadBitmap(ctx, name) ?: return false
        val fw = sheet.width / count
        if (fw <= 0 || fw * count != sheet.width) {
            single[name] = sheet
            return false
        }
        val arr = arrayOfNulls<Bitmap>(count)
        for (i in 0 until count) {
            arr[i] = Bitmap.createBitmap(sheet, i * fw, 0, fw, sheet.height)
        }
        multi[name] = arr
        return true
    }

    // ================= helpers =================

    private fun loadBitmap(ctx: Context, name: String): Bitmap? {
        val opts = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inDither = true
        }

        // 1) drawable
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id != 0) {
            try { BitmapFactory.decodeResource(ctx.resources, id, opts)?.let { return normalize(it) } }
            catch (_: Exception) {}
        }

        // 2) assets/<name>.png
        try {
            ctx.assets.open("$name.png").use { s ->
                BitmapFactory.decodeStream(s, null, opts)?.let { return normalize(it) }
            }
        } catch (_: Exception) {}

        // 3) variations
        for (v in listOf("${name}_1","${name}_01","${name}1","${name}_frame_1")) {
            try {
                ctx.assets.open("$v.png").use { s ->
                    BitmapFactory.decodeStream(s, null, opts)?.let { return normalize(it) }
                }
            } catch (_: Exception) {}
        }

        // 4) optional external
        try { BitmapFactory.decodeFile("/sdcard/$name.png", opts)?.let { return normalize(it) } }
        catch (_: Exception) {}

        Log.w("SpriteLoader", "Missing sprite: $name")
        return null
    }

    private fun normalize(src: Bitmap): Bitmap {
        val copy = src.copy(Bitmap.Config.ARGB_8888, false)
        copy.setHasAlpha(true)
        copy.prepareToDraw()
        return copy
    }
}
