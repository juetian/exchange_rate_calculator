package com.example.fxcalc

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 汇率数据：从免费接口 open.er-api.com 获取（无需 API Key），
 * 并缓存到 SharedPreferences，离线时使用缓存或内置汇率。
 */
class RateRepository(context: Context) {

    private val prefs = context.getSharedPreferences("rates", Context.MODE_PRIVATE)

    /** 1 USD = rates[code] */
    var rates: Map<String, Double> = loadCached() ?: Currencies.FALLBACK_RATES
        private set

    /** 汇率更新时间（毫秒），0 表示使用内置汇率 */
    var updatedAt: Long = prefs.getLong(KEY_TIME, 0L)
        private set

    private fun loadCached(): Map<String, Double>? {
        val json = prefs.getString(KEY_RATES, null) ?: return null
        return runCatching { parse(JSONObject(json)) }.getOrNull()
    }

    private fun parse(obj: JSONObject): Map<String, Double> {
        val map = HashMap<String, Double>()
        for (key in obj.keys()) map[key] = obj.getDouble(key)
        // 缺失的货币用内置值补齐
        for ((k, v) in Currencies.FALLBACK_RATES) map.putIfAbsent(k, v)
        return map
    }

    /** 阻塞调用，请在后台线程执行。成功返回 true。 */
    fun refresh(): Boolean {
        return try {
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val root = JSONObject(body)
            if (root.optString("result") != "success") return false
            val ratesObj = root.getJSONObject("rates")
            val time = root.optLong("time_last_update_unix", System.currentTimeMillis() / 1000) * 1000
            rates = parse(ratesObj)
            updatedAt = time
            prefs.edit().putString(KEY_RATES, ratesObj.toString()).putLong(KEY_TIME, time).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 把 amount 个 from 货币换算成 to 货币 */
    fun convert(amount: Double, from: String, to: String): Double {
        val rFrom = rates[from] ?: return 0.0
        val rTo = rates[to] ?: return 0.0
        return amount / rFrom * rTo
    }

    companion object {
        private const val API_URL = "https://open.er-api.com/v6/latest/USD"
        private const val KEY_RATES = "rates_json"
        private const val KEY_TIME = "updated_at"
    }
}
