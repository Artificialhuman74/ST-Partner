package com.sanathanatattva.partner.data.orders

import com.sanathanatattva.partner.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** A road route from OSRM (handoff §10): the line drawn on in-app maps. Navigation itself is Google Maps. */
data class Route(val points: List<Pair<Double, Double>>, val metres: Double, val seconds: Double)

object Routing {
    /**
     * Our backend's `GET /api/route`, which asks the self-hosted OSRM server on Railway
     * (sanathanatattava/osrm). One place to change the routing server, for app and website alike.
     */
    private val ROUTE = "${BuildConfig.BASE_URL.trimEnd('/')}/api/route"

    private val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, Route>()

    /** lat/lng pairs; rounded to ~10 m so a jittery GPS doesn't refetch every few seconds. */
    suspend fun route(from: Pair<Double, Double>, to: Pair<Double, Double>): Route? {
        fun r(v: Double) = "%.4f".format(java.util.Locale.ROOT, v)
        val key = "${r(from.first)},${r(from.second)}|${r(to.first)},${r(to.second)}"
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "$ROUTE?from=${r(from.first)},${r(from.second)}&to=${r(to.first)},${r(to.second)}"
                client.newCall(Request.Builder().url(url).build()).execute().use { res ->
                    if (!res.isSuccessful) return@use null
                    val body = json.parseToJsonElement(res.body?.string() ?: return@use null).jsonObject
                    val points = body["points"]!!.jsonArray.map { p ->
                        val ll = p.jsonArray
                        ll[0].jsonPrimitive.double to ll[1].jsonPrimitive.double
                    }
                    Route(points, body["metres"]!!.jsonPrimitive.double, body["seconds"]!!.jsonPrimitive.double)
                }
            }.getOrNull()?.also { cache[key] = it }
        }
    }
}
