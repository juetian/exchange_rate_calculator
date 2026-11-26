package com.example.fxcalc

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import kotlin.math.abs
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: RateRepository

    private val rows = mutableListOf<Row>()
    private val selected = mutableListOf<String>()

    /** 当前作为输入源的行及其金额 */
    private var sourceIndex = 0
    private var sourceAmount = 100.0

    /** 程序内部改写文本时置为 true，避免 TextWatcher 互相触发 */
    private var updating = false

    private inner class Row(val index: Int, val b: ItemCurrencyRowBinding)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = RateRepository(this)
        loadSelection()

        val inflater = LayoutInflater.from(this)
        for (i in 0 until ROW_COUNT) {
            val rb = ItemCurrencyRowBinding.inflate(inflater, binding.rowsContainer, false)
            // 插到提示文字之前
            binding.rowsContainer.addView(rb.root, binding.rowsContainer.childCount - 1)
            val row = Row(i, rb)
            rows += row
            setupRow(row)
        }

        binding.btnRefresh.setOnClickListener { refreshRates(showToast = true) }
        binding.btnClear.setOnClickListener {
            sourceAmount = 0.0
            updating = true
            rows.forEach { it.b.etAmount.setText("") }
            updating = false
            rows[sourceIndex].b.etAmount.requestFocus()
        }

        renderAll()
        updateStatus()
        refreshRates(showToast = false)
    }

    private fun setupRow(row: Row) {
        val b = row.b
        b.currencyPicker.setOnClickListener { showPicker(row.index) }
        b.etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                sourceIndex = row.index
                sourceAmount = parse(b.etAmount.text.toString())
                highlightSource()
            }
        }
        b.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                sourceIndex = row.index
                sourceAmount = parse(s?.toString().orEmpty())
                renderOthers()
                highlightSource()
            }
        })
    }

    private fun parse(text: String): Double =
        text.replace(",", "").trim().toDoubleOrNull() ?: 0.0

    /** 刷新所有行（含输入源本身的显示） */
    private fun renderAll() {
        updating = true
        rows.forEach { row ->
            val c = Currencies.get(selected[row.index])
            row.b.tvFlag.text = c.flag
            row.b.tvCode.text = c.code
            row.b.tvName.text = c.name
            if (row.index == sourceIndex) {
                val et = row.b.etAmount
                if (parse(et.text.toString()) != sourceAmount || et.text.isEmpty()) {
                    et.setText(if (sourceAmount == 0.0) "" else plain(sourceAmount))
                }
            }
        }
        updating = false
        renderOthers()
        highlightSource()
    }

    /** 根据输入源换算其他四行 */
    private fun renderOthers() {
        updating = true
        val from = selected[sourceIndex]
        rows.forEach { row ->
            if (row.index == sourceIndex) return@forEach
            val to = selected[row.index]
            val et = row.b.etAmount
            if (sourceAmount == 0.0) {
                et.setText("")
            } else {
                et.setText(format(repo.convert(sourceAmount, from, to)))
            }
        }
        updating = false
        // 显示每一行的 1 单位兑换关系
        rows.forEach { row ->
            val code = selected[row.index]
            val c = Currencies.get(code)
            if (row.index == sourceIndex) {
                row.b.tvName.text = c.name
            } else {
                val unit = repo.convert(1.0, from, code)
                row.b.tvName.text = "${c.name} · 1 $from = ${format(unit)}"
            }
        }
    }

    private fun highlightSource() {
        val active = ContextCompat.getColor(this, R.color.active_stroke)
        val normal = 0x00000000
        rows.forEach { row ->
            row.b.card.strokeColor = if (row.index == sourceIndex) active else normal
        }
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
        return DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(v)
    }

    /** 输入框中保留的纯数字格式 */
    private fun plain(v: Double): String =
        BigDecimal(v).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

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
            renderAll()
            hideKeyboard(search)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun hideKeyboard(v: android.view.View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun refreshRates(showToast: Boolean) {
        binding.tvStatus.setText(R.string.status_loading)
        binding.btnRefresh.isEnabled = false
        thread {
            val ok = repo.refresh()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                binding.btnRefresh.isEnabled = true
                updateStatus()
                if (ok) renderOthers()
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
        val tv: TextView = binding.tvStatus
        if (repo.updatedAt == 0L) {
            tv.setText(R.string.status_builtin)
        } else {
            val time = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(repo.updatedAt))
            tv.text = getString(R.string.status_updated, time)
        }
    }

    private fun loadSelection() {
        val prefs = getSharedPreferences("ui", MODE_PRIVATE)
        val saved = prefs.getString("selected", null)?.split(",")
            ?.filter { code -> Currencies.ALL.any { it.code == code } }
        selected.clear()
        selected += if (saved != null && saved.size == ROW_COUNT) saved else Currencies.DEFAULT_SELECTION
        sourceAmount = prefs.getFloat("amount", 100f).toDouble()
        sourceIndex = prefs.getInt("source", 0).coerceIn(0, ROW_COUNT - 1)
    }

    private fun saveSelection() {
        getSharedPreferences("ui", MODE_PRIVATE).edit()
            .putString("selected", selected.joinToString(","))
            .apply()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences("ui", MODE_PRIVATE).edit()
            .putFloat("amount", sourceAmount.toFloat())
            .putInt("source", sourceIndex)
            .apply()
    }

    companion object {
        private const val ROW_COUNT = 5
    }
}
