package com.example.fxcalc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fxcalc.Calculator.DIVIDE
import com.example.fxcalc.Calculator.MINUS
import com.example.fxcalc.Calculator.OPERATORS
import com.example.fxcalc.Calculator.PLUS
import com.example.fxcalc.Calculator.TIMES
import com.example.fxcalc.databinding.ActivityMainBinding
import com.example.fxcalc.databinding.ItemCurrencyRowBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: RateRepository

    private val rows = mutableListOf<ItemCurrencyRowBinding>()
    private val selected = mutableListOf<String>()

    /** 当前作为输入源的行 */
    private var sourceIndex = 0

    /** 输入源行中的算式，如 "100×3+20" */
    private var expr = "100"

    /** 为 true 时，下一次输入数字会替换当前内容（按 = 之后、或刚切换输入行时） */
    private var replaceOnDigit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = RateRepository(this)
        loadState()

        val inflater = LayoutInflater.from(this)
        for (i in 0 until ROW_COUNT) {
            val rb = ItemCurrencyRowBinding.inflate(inflater, binding.rowsContainer, false)
            // 插到提示文字之前
            binding.rowsContainer.addView(rb.root, binding.rowsContainer.childCount - 1)
            rows += rb
            rb.currencyPicker.setOnClickListener { showPicker(i) }
            rb.card.setOnClickListener { selectSource(i) }
            rb.card.setOnLongClickListener { copyRow(i); true }
        }

        setupKeypad()
        binding.btnRefresh.setOnClickListener { refreshRates(showToast = true) }

        render()
        updateStatus()
        if (autoRefreshOnStart) refreshRates(showToast = false)
    }

    // ---------------- 键盘 ----------------

    private fun setupKeypad() {
        val k = binding.keypad
        val digits = mapOf(
            k.key0 to "0", k.key1 to "1", k.key2 to "2", k.key3 to "3", k.key4 to "4",
            k.key5 to "5", k.key6 to "6", k.key7 to "7", k.key8 to "8", k.key9 to "9",
            k.key00 to "00",
        )
        digits.forEach { (btn, d) -> btn.setOnClickListener { onKey(it) { inputDigits(d) } } }
        k.keyDot.setOnClickListener { onKey(it) { inputDot() } }
        k.keyPlus.setOnClickListener { onKey(it) { inputOperator(PLUS) } }
        k.keyMinus.setOnClickListener { onKey(it) { inputOperator(MINUS) } }
        k.keyTimes.setOnClickListener { onKey(it) { inputOperator(TIMES) } }
        k.keyDivide.setOnClickListener { onKey(it) { inputOperator(DIVIDE) } }
        k.keyPercent.setOnClickListener { onKey(it) { inputPercent() } }
        k.keyBack.setOnClickListener { onKey(it) { backspace() } }
        k.keyBack.setOnLongClickListener { onKey(it) { clear() }; true }
        k.keyClear.setOnClickListener { onKey(it) { clear() } }
        k.keyEquals.setOnClickListener { onKey(it) { equals() } }
    }

    private inline fun onKey(v: View, action: () -> Unit) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        action()
        render()
    }

    /** 当前正在输入的数字（最后一个运算符之后的部分） */
    private fun currentToken(): String {
        val i = expr.indexOfLast { it in OPERATORS }
        return expr.substring(i + 1)
    }

    private fun inputDigits(d: String) {
        if (replaceOnDigit) {
            expr = ""
            replaceOnDigit = false
        }
        if (expr.endsWith("%")) return
        val token = currentToken()
        if (token.count { it.isDigit() } >= MAX_DIGITS || expr.length >= MAX_LENGTH) return
        when {
            token.isEmpty() && d == "00" -> expr += "0"
            token == "0" && d == "00" -> Unit
            token == "0" -> expr = expr.dropLast(1) + d
            else -> expr += d
        }
    }

    private fun inputDot() {
        if (replaceOnDigit) {
            expr = ""
            replaceOnDigit = false
        }
        if (expr.endsWith("%")) return
        val token = currentToken()
        when {
            token.contains('.') -> Unit
            token.isEmpty() -> expr += "0."
            else -> expr += "."
        }
    }

    private fun inputOperator(op: Char) {
        replaceOnDigit = false
        if (expr.length >= MAX_LENGTH) return
        val last = expr.lastOrNull()
        when {
            last == null -> if (op == MINUS) expr = MINUS.toString()
            last in OPERATORS -> {
                expr = expr.dropLast(1)
                // 只剩一个负号时，换成其他运算符没有意义
                expr = if (expr.isEmpty()) (if (op == MINUS) MINUS.toString() else "") else expr + op
            }
            last == '.' -> expr = expr.dropLast(1) + op
            else -> expr += op
        }
    }

    private fun inputPercent() {
        replaceOnDigit = false
        val last = expr.lastOrNull() ?: return
        if (last.isDigit() || last == '%') expr += "%"
    }

    private fun backspace() {
        replaceOnDigit = false
        expr = expr.dropLast(1)
    }

    private fun clear() {
        replaceOnDigit = false
        expr = ""
    }

    private fun equals() {
        val result = Calculator.evaluate(expr)
        if (result == null) {
            Toast.makeText(this, R.string.calc_error, Toast.LENGTH_SHORT).show()
            return
        }
        expr = if (result.signum() == 0) "" else plain(result)
        replaceOnDigit = true
    }

    // ---------------- 行操作 ----------------

    /** 点击某一行：把它设为输入源，以它当前显示的金额为起点继续计算 */
    private fun selectSource(index: Int) {
        if (index == sourceIndex) return
        val shown = rows[index].tvAmount.text.toString().replace(",", "")
        expr = shown
        sourceIndex = index
        replaceOnDigit = true
        rows[index].card.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        render()
    }

    private fun copyRow(index: Int) {
        val text = if (index == sourceIndex) {
            Calculator.evaluate(expr)?.let { plain(it) } ?: return
        } else {
            rows[index].tvAmount.text.toString().replace(",", "")
        }
        if (text.isEmpty()) return
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(selected[index], text))
        Toast.makeText(this, getString(R.string.copied, "$text ${selected[index]}"), Toast.LENGTH_SHORT).show()
    }

    // ---------------- 显示 ----------------

    private fun render() {
        val result = Calculator.evaluate(expr)
        val value = result?.toDouble() ?: 0.0
        val from = selected[sourceIndex]
        val active = ContextCompat.getColor(this, R.color.active_stroke)

        rows.forEachIndexed { i, row ->
            val code = selected[i]
            val c = Currencies.get(code)
            row.tvFlag.text = c.flag
            row.tvCode.text = c.code
            row.card.strokeColor = if (i == sourceIndex) active else 0

            if (i == sourceIndex) {
                row.tvName.text = c.name
                row.tvAmount.text = prettyExpr(expr)
                if (Calculator.hasOperator(expr)) {
                    row.tvSub.visibility = View.VISIBLE
                    row.tvSub.text = if (result == null) getString(R.string.calc_error)
                    else "= ${format(value)}"
                } else {
                    row.tvSub.visibility = View.GONE
                }
            } else {
                row.tvName.text = "${c.name} · 1 $from = ${format(repo.convert(1.0, from, code))}"
                row.tvAmount.text = if (result == null || value == 0.0) "" else
                    format(repo.convert(value, from, code))
                row.tvSub.visibility = View.GONE
            }
        }
    }

    /** 给算式中的每个数字加上千分位，如 "1234.5×3" → "1,234.5×3" */
    private fun prettyExpr(s: String): String =
        NUMBER.replace(s) { m ->
            val t = m.value
            val dot = t.indexOf('.')
            val intPart = if (dot >= 0) t.substring(0, dot) else t
            val grouped = if (intPart.isEmpty()) "" else
                DecimalFormat("#,##0", US).format(BigDecimal(intPart))
            if (dot >= 0) grouped + t.substring(dot) else grouped
        }

    /** 显示用格式：带千分位，小数位数随数值大小调整 */
    private fun format(v: Double): String {
        val a = abs(v)
        val pattern = when {
            a == 0.0 -> "0"
            a >= 1 -> "#,##0.##"
            a >= 0.01 -> "#,##0.####"
            else -> "#,##0.######"
        }
        return DecimalFormat(pattern, US).format(v)
    }

    /** 算式中使用的纯数字格式，负数用 − 号 */
    private fun plain(v: BigDecimal): String =
        v.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            .replace('-', MINUS)

    // ---------------- 货币选择 ----------------

    private fun showPicker(rowIndex: Int) {
        val view = layoutInflater.inflate(R.layout.dialog_currency_picker, null)
        val search = view.findViewById<EditText>(R.id.etSearch)
        val list = view.findViewById<ListView>(R.id.listView)

        var filtered = Currencies.ALL
        fun labels() = filtered.map { "${it.flag}  ${it.code}  ${it.name}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels().toMutableList())
        list.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_currency)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim().orEmpty()
                filtered = if (q.isEmpty()) Currencies.ALL else Currencies.ALL.filter {
                    it.code.contains(q, ignoreCase = true) || it.name.contains(q)
                }
                adapter.clear()
                adapter.addAll(labels())
            }
        })

        list.setOnItemClickListener { _, _, pos, _ ->
            val code = filtered[pos].code
            // 若所选货币已在其他行，则两行交换
            val existing = selected.indexOf(code)
            if (existing >= 0 && existing != rowIndex) {
                selected[existing] = selected[rowIndex]
            }
            selected[rowIndex] = code
            saveSelection()
            render()
            hideKeyboard(search)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun hideKeyboard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    // ---------------- 汇率 ----------------

    private fun refreshRates(showToast: Boolean) {
        binding.tvStatus.setText(R.string.status_loading)
        binding.btnRefresh.isEnabled = false
        thread {
            val ok = repo.refresh()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                binding.btnRefresh.isEnabled = true
                updateStatus()
                if (ok) render()
                if (showToast) {
                    Toast.makeText(
                        this,
                        if (ok) R.string.toast_ok else R.string.toast_fail,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateStatus() {
        if (repo.updatedAt == 0L) {
            binding.tvStatus.setText(R.string.status_builtin)
        } else {
            val time = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(repo.updatedAt))
            binding.tvStatus.text = getString(R.string.status_updated, time)
        }
    }

    // ---------------- 保存状态 ----------------

    private fun loadState() {
        val prefs = getSharedPreferences("ui", MODE_PRIVATE)
        val saved = prefs.getString("selected", null)?.split(",")
            ?.filter { code -> Currencies.ALL.any { it.code == code } }
        selected.clear()
        selected += if (saved != null && saved.size == ROW_COUNT) saved else Currencies.DEFAULT_SELECTION
        expr = prefs.getString("expr", "100") ?: "100"
        sourceIndex = prefs.getInt("source", 0).coerceIn(0, ROW_COUNT - 1)
        replaceOnDigit = true
    }

    private fun saveSelection() {
        getSharedPreferences("ui", MODE_PRIVATE).edit()
            .putString("selected", selected.joinToString(","))
            .apply()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences("ui", MODE_PRIVATE).edit()
            .putString("expr", expr)
            .putInt("source", sourceIndex)
            .apply()
    }

    companion object {
        /** 测试时关闭启动联网，保证结果可重复 */
        @VisibleForTesting
        var autoRefreshOnStart = true

        private const val ROW_COUNT = 5
        private const val MAX_DIGITS = 15
        private const val MAX_LENGTH = 40
        private val US = DecimalFormatSymbols(Locale.US)
        private val NUMBER = Regex("""\d*\.?\d+|\d+\.""")
    }
}
