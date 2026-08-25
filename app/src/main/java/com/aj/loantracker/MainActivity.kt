package com.aj.loantracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Payment(
    val date: String,
    val amount: Double,
    val note: String = ""
)

data class Loan(
    val id: Long,
    val lender: String,
    val amount: Double,
    val takenDate: String,
    val interest: Double,
    val durationMonths: Int,
    val monthlyPayment: Double,
    val dueDate: String,
    val purpose: String,
    val notes: String,
    val payments: List<Payment>
) {
    val paid: Double get() = payments.sumOf { it.amount }
    val remaining: Double get() = (amount + (amount * interest / 100.0) - paid).coerceAtLeast(0.0)
    val status: String get() = if (remaining <= 0.01) "PAID" else "ACTIVE"
}

class LoanStore(context: Context) {
    private val prefs = context.getSharedPreferences("loans", Context.MODE_PRIVATE)

    fun load(): List<Loan> {
        val raw = prefs.getString("data", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val pa = o.optJSONArray("payments") ?: JSONArray()
            val payments = (0 until pa.length()).map { p ->
                val x = pa.getJSONObject(p)
                Payment(x.optString("date"), x.optDouble("amount"), x.optString("note"))
            }
            Loan(
                o.optLong("id"),
                o.optString("lender"),
                o.optDouble("amount"),
                o.optString("takenDate"),
                o.optDouble("interest"),
                o.optInt("durationMonths"),
                o.optDouble("monthlyPayment"),
                o.optString("dueDate"),
                o.optString("purpose"),
                o.optString("notes"),
                payments
            )
        }
    }

    fun save(loans: List<Loan>) {
        val arr = JSONArray()
        loans.forEach { l ->
            val o = JSONObject()
            o.put("id", l.id).put("lender", l.lender).put("amount", l.amount)
                .put("takenDate", l.takenDate).put("interest", l.interest)
                .put("durationMonths", l.durationMonths).put("monthlyPayment", l.monthlyPayment)
                .put("dueDate", l.dueDate).put("purpose", l.purpose).put("notes", l.notes)
            val pa = JSONArray()
            l.payments.forEach { p ->
                pa.put(JSONObject().put("date", p.date).put("amount", p.amount).put("note", p.note))
            }
            o.put("payments", pa)
            arr.put(o)
        }
        prefs.edit().putString("data", arr.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LoanStore(this)
        setContent { LoanTrackerApp(store) }
    }
}

@Composable
fun LoanTrackerApp(store: LoanStore) {
    var loans by remember { mutableStateOf(store.load()) }
    var selected by remember { mutableStateOf<Loan?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }

    fun persist(newLoans: List<Loan>) {
        loans = newLoans
        store.save(newLoans)
        selected = selected?.let { s -> newLoans.find { it.id == s.id } }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Loan Tracker", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { showAdd = true }) {
                            Icon(Icons.Default.Add, "Add loan")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selected == null) {
                    FloatingActionButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, "Add loan")
                    }
                }
            }
        ) { pad ->
            if (selected != null) {
                LoanDetails(
                    loan = selected!!,
                    modifier = Modifier.padding(pad),
                    onBack = { selected = null },
                    onAddPayment = { showPayment = true },
                    onDelete = {
                        persist(loans.filterNot { it.id == selected!!.id })
                        selected = null
                    }
                )
            } else {
                Dashboard(
                    loans = loans,
                    modifier = Modifier.padding(pad),
                    onSelect = { selected = it },
                    onAdd = { showAdd = true }
                )
            }
        }

        if (showAdd) {
            AddLoanDialog(
                onDismiss = { showAdd = false },
                onSave = { loan ->
                    persist(loans + loan)
                    showAdd = false
                }
            )
        }

        if (showPayment && selected != null) {
            AddPaymentDialog(
                onDismiss = { showPayment = false },
                onSave = { payment ->
                    val s = selected!!
                    persist(loans.map {
                        if (it.id == s.id) it.copy(payments = it.payments + payment) else it
                    })
                    showPayment = false
                }
            )
        }
    }
}

@Composable
fun Dashboard(
    loans: List<Loan>,
    modifier: Modifier,
    onSelect: (Loan) -> Unit,
    onAdd: () -> Unit
) {
    val total = loans.sumOf { it.amount }
    val paid = loans.sumOf { it.paid }
    val remaining = loans.sumOf { it.remaining }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Borrowed", "৳${"%,.2f".format(total)}", Modifier.weight(1f))
            SummaryCard("Paid", "৳${"%,.2f".format(paid)}", Modifier.weight(1f))
            SummaryCard("Left", "৳${"%,.2f".format(remaining)}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        if (loans.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No loans yet", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onAdd) { Text("Add your first loan") }
                }
            }
        } else {
            Text("Your Loans", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(loans, key = { it.id }) { loan ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onSelect(loan) }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(loan.lender, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Taken: ${loan.takenDate}")
                                Text("Paid: ৳${"%,.2f".format(loan.paid)}  •  Left: ৳${"%,.2f".format(loan.remaining)}")
                            }
                            AssistChip(onClick = {}, label = { Text(loan.status) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoanDetails(
    loan: Loan,
    modifier: Modifier,
    onBack: () -> Unit,
    onAddPayment: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(loan.lender, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Detail("Loan amount", "৳${"%,.2f".format(loan.amount)}")
                Detail("Taken", loan.takenDate)
                Detail("Interest", "${loan.interest}%")
                Detail("Duration", "${loan.durationMonths} months")
                Detail("Monthly payment", "৳${"%,.2f".format(loan.monthlyPayment)}")
                Detail("Due date", loan.dueDate)
                Detail("Purpose", loan.purpose.ifBlank { "—" })
                Detail("Status", loan.status)
                HorizontalDivider()
                Detail("Total paid", "৳${"%,.2f".format(loan.paid)}")
                Detail("Remaining", "৳${"%,.2f".format(loan.remaining)}")
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddPayment, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Payments, null)
                Spacer(Modifier.width(6.dp))
                Text("Record Payment")
            }
            OutlinedButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        }

        Spacer(Modifier.height(12.dp))
        Text("Payment History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))

        if (loan.payments.isEmpty()) {
            Text("No payments recorded yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(loan.payments) { p ->
                    ListItem(
                        headlineContent = { Text("৳${"%,.2f".format(p.amount)}") },
                        supportingContent = { Text("${p.date}${if (p.note.isNotBlank()) " • ${p.note}" else ""}") }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AddLoanDialog(onDismiss: () -> Unit, onSave: (Loan) -> Unit) {
    var lender by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var taken by remember { mutableStateOf(today()) }
    var interest by remember { mutableStateOf("0") }
    var duration by remember { mutableStateOf("0") }
    var monthly by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Lender / Bank", lender) { lender = it }
                Field("Loan amount", amount) { amount = it }
                Field("Taken date (DD-MM-YYYY)", taken) { taken = it }
                Field("Interest %", interest) { interest = it }
                Field("Duration (months)", duration) { duration = it }
                Field("Monthly payment", monthly) { monthly = it }
                Field("Final due date", due) { due = it }
                Field("Purpose", purpose) { purpose = it }
                Field("Notes", notes) { notes = it }
            }
        },
        confirmButton = {
            Button(
                enabled = lender.isNotBlank() && amount.toDoubleOrNull() != null,
                onClick = {
                    onSave(
                        Loan(
                            id = System.currentTimeMillis(),
                            lender = lender.trim(),
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            takenDate = taken,
                            interest = interest.toDoubleOrNull() ?: 0.0,
                            durationMonths = duration.toIntOrNull() ?: 0,
                            monthlyPayment = monthly.toDoubleOrNull() ?: 0.0,
                            dueDate = due,
                            purpose = purpose,
                            notes = notes,
                            payments = emptyList()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddPaymentDialog(onDismiss: () -> Unit, onSave: (Payment) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Amount paid", amount) { amount = it }
                Field("Payment date", date) { date = it }
                Field("Note", note) { note = it }
            }
        },
        confirmButton = {
            Button(
                enabled = amount.toDoubleOrNull() != null && amount.toDouble() > 0,
                onClick = { onSave(Payment(date, amount.toDouble(), note)) }
            ) { Text("Save Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

fun today(): String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
