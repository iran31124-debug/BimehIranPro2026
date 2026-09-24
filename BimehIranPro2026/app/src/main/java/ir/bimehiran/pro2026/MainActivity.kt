package ir.bimehiran.pro2026

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class Policy(
    val id: Long,
    var number: String,
    var customer: String,
    var plate: String,
    var total: Long,
    var cash: Long,
    var installments: Int,
    var startDate: String
) {
    var paid: Long = cash
    val remainingAmount: Long get() = (total - paid).coerceAtLeast(0)
    val openInstallments: Int
        get() = if (installments == 0) 0 else
            ((remainingAmount + installmentAmount - 1) / installmentAmount).toInt().coerceAtMost(installments)
    val installmentAmount: Long
        get() = if (installments <= 0) 0 else ((total - cash).coerceAtLeast(0) / installments)
}

class MainActivity : AppCompatActivity() {

    private val policies = mutableListOf<Policy>()
    private lateinit var content: LinearLayout
    private lateinit var summary: TextView

    private val nf = NumberFormat.getNumberInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "حسابداری بیمه ایران"
        buildUi()
        seedDemo()
        showDashboard()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF4F7FB.toInt())
        }

        val header = TextView(this).apply {
            text = "حسابداری بیمه ایران  •  نسخه 2026"
            textSize = 20f
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(28, 20, 28, 20)
            setBackgroundColor(0xFF163A5F.toInt())
        }
        root.addView(header, LinearLayout.LayoutParams(-1, 72))

        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        nav.addView(navButton("داشبورد") { showDashboard() })
        nav.addView(navButton("ثبت بیمه‌نامه") { showForm(null) })
        nav.addView(navButton("بیمه‌نامه‌ها") { showPolicies("") })
        nav.addView(navButton("گزارش‌ها") { showReports() })
        root.addView(nav, LinearLayout.LayoutParams(-1, 58))

        summary = TextView(this).apply {
            textSize = 14f
            setPadding(20, 10, 20, 10)
            setTextColor(0xFF37474F.toInt())
        }
        root.addView(summary, LinearLayout.LayoutParams(-1, 50))

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 4, 16, 24)
        }
        val scroll = ScrollView(this).apply { addView(content) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
    }

    private fun navButton(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 11f
            setOnClickListener { action() }
            isAllCaps = false
        }

    private fun showDashboard() {
        content.removeAllViews()
        val total = policies.sumOf { it.total }
        val paid = policies.sumOf { it.paid }
        val remaining = policies.sumOf { it.remainingAmount }
        summary.text = "تعداد: ${policies.size}   |   کل: ${money(total)}   |   وصولی: ${money(paid)}   |   مانده: ${money(remaining)}"

        card("نمای کلی", "مدیریت کامل بیمه‌نامه، اقساط و دریافت‌ها") {}
        card("بیمه‌نامه‌های فعال", "${policies.size} بیمه‌نامه ثبت شده") { showPolicies("") }
        card("مانده مطالبات", money(remaining)) { showPolicies("مانده") }
        card("سررسید و اقساط", "برای هر بیمه‌نامه پرداخت اقساط را ثبت کنید") { showPolicies("") }

        val add = Button(this).apply {
            text = "＋ ثبت بیمه‌نامه جدید"
            textSize = 16f
            isAllCaps = false
            setOnClickListener { showForm(null) }
        }
        content.addView(add, LinearLayout.LayoutParams(-1, 56))
    }

    private fun card(title: String, value: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = "$title\n$value"
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textSize = 16f
            isAllCaps = false
            setPadding(24, 10, 24, 10)
            setOnClickListener { action() }
        }
        val lp = LinearLayout.LayoutParams(-1, 88)
        lp.setMargins(0, 6, 0, 6)
        content.addView(b, lp)
    }

    private fun showPolicies(filter: String) {
        content.removeAllViews()
        val search = EditText(this).apply {
            hint = "جست‌وجو: شماره بیمه‌نامه، بیمه‌گذار یا پلاک"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        content.addView(search, LinearLayout.LayoutParams(-1, 56))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)

        fun refresh() {
            list.removeAllViews()
            val q = search.text.toString().trim()
            policies.filter {
                (q.isEmpty() || it.number.contains(q, true) || it.customer.contains(q, true) || it.plate.contains(q, true)) &&
                (filter != "مانده" || it.remainingAmount > 0)
            }.forEach { p ->
                val row = Button(this).apply {
                    text = "${p.customer}  •  ${p.number}\nمانده: ${money(p.remainingAmount)}   اقساط باز: ${p.openInstallments}"
                    gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                    isAllCaps = false
                    setOnClickListener { showPolicyDetail(p) }
                }
                list.addView(row, LinearLayout.LayoutParams(-1, 82))
            }
            if (list.childCount == 0) {
                list.addView(TextView(this).apply {
                    text = "موردی پیدا نشد."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(8, 40, 8, 40)
                })
            }
        }
        search.addTextChangedListenerSimple { refresh() }
        refresh()
    }

    private fun showPolicyDetail(p: Policy) {
        content.removeAllViews()
        content.addView(TextView(this).apply {
            text = "جزئیات بیمه‌نامه"
            textSize = 22f
            setPadding(8, 10, 8, 18)
        })
        detail("شماره بیمه‌نامه", p.number)
        detail("بیمه‌گذار", p.customer)
        detail("پلاک", p.plate)
        detail("مبلغ کل", money(p.total))
        detail("پرداختی", money(p.paid))
        detail("مانده", money(p.remainingAmount))
        detail("تعداد اقساط", p.installments.toString())
        detail("قسط تقریبی", money(p.installmentAmount))
        detail("اقساط باز", p.openInstallments)

        val pay = Button(this).apply {
            text = "ثبت پرداخت قسط"
            isAllCaps = false
            setOnClickListener { paymentDialog(p) }
        }
        content.addView(pay)
        val edit = Button(this).apply {
            text = "ویرایش بیمه‌نامه"
            isAllCaps = false
            setOnClickListener { showForm(p) }
        }
        content.addView(edit)
        val del = Button(this).apply {
            text = "حذف بیمه‌نامه"
            isAllCaps = false
            setOnClickListener {
                policies.remove(p)
                showPolicies("")
            }
        }
        content.addView(del)
    }

    private fun detail(k: String, v: Any) {
        content.addView(TextView(this).apply {
            text = "$k: $v"
            textSize = 16f
            setPadding(10, 7, 10, 7)
        })
    }

    private fun showForm(existing: Policy?) {
        content.removeAllViews()
        val title = if (existing == null) "ثبت بیمه‌نامه جدید" else "ویرایش بیمه‌نامه"
        content.addView(TextView(this).apply {
            text = title
            textSize = 22f
            setPadding(8, 10, 8, 18)
        })

        val number = field("شماره بیمه‌نامه", existing?.number ?: "")
        val customer = field("نام بیمه‌گذار", existing?.customer ?: "")
        val plate = field("شماره پلاک", existing?.plate ?: "")
        val total = field("مبلغ کل (ریال)", existing?.total?.toString() ?: "", true)
        val cash = field("پرداخت نقدی (ریال)", existing?.cash?.toString() ?: "", true)
        val installments = field("تعداد اقساط", existing?.installments?.toString() ?: "1", true)
        val date = field("تاریخ شروع", existing?.startDate ?: "انتخاب تاریخ")
        date.setOnClickListener { chooseDate(date) }

        listOf(number, customer, plate, total, cash, installments, date).forEach { content.addView(it) }

        val save = Button(this).apply {
            text = if (existing == null) "ذخیره بیمه‌نامه" else "ذخیره تغییرات"
            isAllCaps = false
            setOnClickListener {
                val t = total.text.toString().toLongOrNull() ?: 0
                val c = cash.text.toString().toLongOrNull() ?: 0
                val ins = installments.text.toString().toIntOrNull() ?: 0
                if (number.text.isBlank() || customer.text.isBlank() || t <= 0 || ins < 0) {
                    Toast.makeText(this@MainActivity, "شماره، بیمه‌گذار، مبلغ کل و تعداد اقساط را کامل کنید.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (existing == null) {
                    policies.add(Policy(
                        System.currentTimeMillis(), number.text.toString(), customer.text.toString(),
                        plate.text.toString(), t, c.coerceAtMost(t), ins, date.text.toString()
                    ))
                } else {
                    existing.number = number.text.toString()
                    existing.customer = customer.text.toString()
                    existing.plate = plate.text.toString()
                    existing.total = t
                    existing.cash = c.coerceAtMost(t)
                    existing.installments = ins
                    existing.startDate = date.text.toString()
                    if (existing.paid < existing.cash) existing.paid = existing.cash
                }
                Toast.makeText(this@MainActivity, "ذخیره شد.", Toast.LENGTH_SHORT).show()
                showPolicies("")
            }
        }
        content.addView(save, LinearLayout.LayoutParams(-1, 58))
    }

    private fun field(hint: String, value: String, number: Boolean = false): EditText =
        EditText(this).apply {
            this.hint = hint
            setText(value)
            textSize = 16f
            setSingleLine()
            setPadding(14, 4, 14, 4)
            if (number) inputType = InputType.TYPE_CLASS_NUMBER
        }

    private fun chooseDate(target: EditText) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            target.setText("%04d/%02d/%02d".format(y, m + 1, d))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun paymentDialog(p: Policy) {
        val e = EditText(this).apply {
            hint = "مبلغ پرداختی"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialogBuilder(this, "ثبت پرداخت", e) {
            val amount = e.text.toString().toLongOrNull() ?: 0
            if (amount > 0) {
                p.paid = (p.paid + amount).coerceAtMost(p.total)
                showPolicyDetail(p)
            }
        }
    }

    private fun showReports() {
        content.removeAllViews()
        val total = policies.sumOf { it.total }
        val paid = policies.sumOf { it.paid }
        val remain = policies.sumOf { it.remainingAmount }
        val open = policies.sumOf { it.openInstallments }
        content.addView(TextView(this).apply {
            text = "گزارش مالی\n\nتعداد بیمه‌نامه: ${policies.size}\nکل حق‌بیمه: ${money(total)}\nکل وصولی: ${money(paid)}\nکل مانده: ${money(remain)}\nتعداد اقساط باز: $open"
            textSize = 18f
            setPadding(10, 20, 10, 20)
        })
        val export = Button(this).apply {
            text = "نمایش فهرست مطالبات"
            isAllCaps = false
            setOnClickListener { showPolicies("مانده") }
        }
        content.addView(export)
    }

    private fun seedDemo() {
        if (policies.isNotEmpty()) return
        policies.add(Policy(1, "IR-1405-0001", "نمونه آزمایشی", "12الف34567", 120_000_000, 40_000_000, 4, "1405/07/01").apply {
            paid = 40_000_000
        })
    }

    private fun money(v: Long): String = nf.format(v) + " ریال"

    private fun EditText.addTextChangedListenerSimple(block: () -> Unit) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { block() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun AlertDialogBuilder(
        activity: MainActivity, title: String, view: EditText, ok: () -> Unit
    ) {
        android.app.AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ثبت") { _, _ -> ok() }
            .show()
    }
}
