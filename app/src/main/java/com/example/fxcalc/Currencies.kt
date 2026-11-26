package com.example.fxcalc

data class Currency(val code: String, val name: String, val flag: String)

/** 全球主要货币。flag 使用 emoji 国旗。 */
object Currencies {
    val ALL: List<Currency> = listOf(
        Currency("CNY", "人民币", "🇨🇳"),
        Currency("USD", "美元", "🇺🇸"),
        Currency("EUR", "欧元", "🇪🇺"),
        Currency("JPY", "日元", "🇯🇵"),
        Currency("GBP", "英镑", "🇬🇧"),
        Currency("HKD", "港币", "🇭🇰"),
        Currency("TWD", "新台币", "🇹🇼"),
        Currency("MOP", "澳门元", "🇲🇴"),
        Currency("KRW", "韩元", "🇰🇷"),
        Currency("AUD", "澳大利亚元", "🇦🇺"),
        Currency("CAD", "加拿大元", "🇨🇦"),
        Currency("CHF", "瑞士法郎", "🇨🇭"),
        Currency("SGD", "新加坡元", "🇸🇬"),
        Currency("NZD", "新西兰元", "🇳🇿"),
        Currency("SEK", "瑞典克朗", "🇸🇪"),
        Currency("NOK", "挪威克朗", "🇳🇴"),
        Currency("DKK", "丹麦克朗", "🇩🇰"),
        Currency("PLN", "波兰兹罗提", "🇵🇱"),
        Currency("RUB", "俄罗斯卢布", "🇷🇺"),
        Currency("TRY", "土耳其里拉", "🇹🇷"),
        Currency("INR", "印度卢比", "🇮🇳"),
        Currency("THB", "泰铢", "🇹🇭"),
        Currency("MYR", "马来西亚林吉特", "🇲🇾"),
        Currency("IDR", "印尼盾", "🇮🇩"),
        Currency("PHP", "菲律宾比索", "🇵🇭"),
        Currency("VND", "越南盾", "🇻🇳"),
        Currency("AED", "阿联酋迪拉姆", "🇦🇪"),
        Currency("SAR", "沙特里亚尔", "🇸🇦"),
        Currency("ILS", "以色列新谢克尔", "🇮🇱"),
        Currency("BRL", "巴西雷亚尔", "🇧🇷"),
        Currency("MXN", "墨西哥比索", "🇲🇽"),
        Currency("ZAR", "南非兰特", "🇿🇦"),
    )

    private val byCode = ALL.associateBy { it.code }
    fun get(code: String): Currency = byCode[code] ?: ALL.first()

    /** 默认显示的五种货币 */
    val DEFAULT_SELECTION = listOf("CNY", "USD", "EUR", "JPY", "GBP")

    /**
     * 内置参考汇率（1 USD = x），用于首次启动或离线时。
     * 联网后会自动替换为最新汇率。
     */
    val FALLBACK_RATES: Map<String, Double> = mapOf(
        "USD" to 1.0, "CNY" to 6.678179, "EUR" to 0.91245, "JPY" to 159.238818,
        "GBP" to 0.763269, "HKD" to 7.841855, "TWD" to 32.918193, "MOP" to 8.077121,
        "KRW" to 1343.042517, "AUD" to 1.507265, "CAD" to 1.429121, "CHF" to 0.836724,
        "SGD" to 1.28993, "NZD" to 1.826291, "SEK" to 10.240617, "NOK" to 9.801644,
        "DKK" to 6.821344, "PLN" to 4.06987, "RUB" to 81.96636, "TRY" to 50.669245,
        "INR" to 92.925233, "THB" to 33.940118, "MYR" to 4.115504, "IDR" to 18227.123111,
        "PHP" to 63.769743, "VND" to 25765.93616, "AED" to 3.6725, "SAR" to 3.75,
        "ILS" to 3.339784, "BRL" to 5.894422, "MXN" to 19.375508, "ZAR" to 17.532655,
    )
}
