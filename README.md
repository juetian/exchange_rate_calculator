# 汇率计算器（Android）

一个简单的安卓汇率计算器，**五种货币同时比较**，内置 32 种全球主要货币。

## 功能

- 5 行货币同时显示：在任意一行输入金额，其余四行实时换算
- 点击货币（国旗/代码）可更换，支持按代码或中文名搜索；选中已存在的货币时两行自动交换
- 每行显示 `1 源货币 = x 本货币` 的单位汇率
- 启动时自动从 [open.er-api.com](https://open.er-api.com)（免费、无需 API Key）获取最新汇率，并缓存到本地
- 离线可用：无网络时使用上次缓存，首次离线则使用内置参考汇率
- 记住所选货币和上次输入的金额；支持深色模式

## 支持的货币

人民币 CNY、美元 USD、欧元 EUR、日元 JPY、英镑 GBP、港币 HKD、新台币 TWD、澳门元 MOP、
韩元 KRW、澳元 AUD、加元 CAD、瑞士法郎 CHF、新加坡元 SGD、新西兰元 NZD、瑞典克朗 SEK、
挪威克朗 NOK、丹麦克朗 DKK、波兰兹罗提 PLN、俄罗斯卢布 RUB、土耳其里拉 TRY、印度卢比 INR、
泰铢 THB、马来西亚林吉特 MYR、印尼盾 IDR、菲律宾比索 PHP、越南盾 VND、阿联酋迪拉姆 AED、
沙特里亚尔 SAR、以色列新谢克尔 ILS、巴西雷亚尔 BRL、墨西哥比索 MXN、南非兰特 ZAR

如需增加货币，编辑 `app/src/main/java/com/example/fxcalc/Currencies.kt` 即可。

## 构建

- 用 Android Studio 直接打开本项目运行；或
- 命令行：`./gradlew assembleRelease`，APK 位于 `app/build/outputs/apk/release/app-release.apk`
- 也可在 GitHub 的 Actions 页面下载自动构建的 APK

要求：JDK 17+，Android SDK 35。最低支持 Android 7.0（API 24）。

> release 包目前使用 debug 签名，便于直接安装测试；上架应用商店前请换成自己的签名。
