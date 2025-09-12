package com.example.game2d
import android.content.Context
object AppCtx {
    lateinit var ctx: Context
    val res get() = ctx.resources
    val pkg get() = ctx.packageName
    val assets get() = ctx.assets
}
