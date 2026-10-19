package com.kiroyal.mydayassistant

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Cream = Color(0xFFFFF9F0)
private val Lavender = Color(0xFFEAE2F8)
private val Purple = Color(0xFF7861A8)
private val DeepPurple = Color(0xFF493765)
private val Muted = Color(0xFF786F82)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyDayAssistant(applicationContext) }
    }
}
private data class Entry(val id: Long, val type: String, val title: String, val body: String, val date: String)
private fun today() = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date())

@Composable
private fun MyDayAssistant(context: Context) {
    val prefs = remember { context.getSharedPreferences("myday_local", Context.MODE_PRIVATE) }
    var entries by remember { mutableStateOf(readEntries(prefs)) }
    var tab by remember { mutableStateOf("Home") }
    var mood by remember { mutableStateOf(prefs.getString("mood", "Calm") ?: "Calm") }
    var showAdd by remember { mutableStateOf(false) }
    var addType by remember { mutableStateOf("Plan") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var pinDialog by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pinSet by remember { mutableStateOf(prefs.getString("pin", null) != null) }
    var locked by remember { mutableStateOf(pinSet) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, secondary = DeepPurple, background = Cream, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Cream) {
            if (locked && pinSet) {
                Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MyDay Assistant", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DeepPurple)
                    Text("Your private space", color = Muted)
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(pinInput, { pinInput = it.filter(Char::isDigit).take(8) }, label = { Text("Enter privacy PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    if (pinError.isNotBlank()) Text(pinError, color = Color.Red)
                    Button(onClick = { if (pinInput == prefs.getString("pin", "")) { locked = false; pinInput = ""; pinError = "" } else { pinError = "Incorrect PIN"; pinInput = "" } }) { Text("Unlock") }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxWidth().background(Lavender).padding(22.dp)) {
                        Text("MyDay Assistant", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DeepPurple)
                        Text(today(), color = DeepPurple)
                        Text("A softer place to plan, reflect and grow.", color = DeepPurple)
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Home", "Planner", "Diary", "Mood", "Private").forEach { label ->
                            Text(label, modifier = Modifier.clickable { tab = label }.background(if (tab == label) Lavender else Color.Transparent, RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 10.dp), color = DeepPurple, fontSize = 12.sp, fontWeight = if (tab == label) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    when (tab) {
                        "Home" -> Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Welcome back ✨", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepPurple)
                            Text("Small steps count. Make today your own.", color = Muted)
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp)) {
                                Text("Today's mood", fontWeight = FontWeight.Bold, color = DeepPurple)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("😊 Great", "😌 Calm", "🙂 Okay", "😔 Low").forEach { option -> Text(option, modifier = Modifier.clickable { mood = option.substringAfter(" "); prefs.edit().putString("mood", mood).apply() }.background(if (mood == option.substringAfter(" ")) Lavender else Cream, RoundedCornerShape(10.dp)).padding(8.dp), fontSize = 12.sp) } }
                            } }
                            listOf("Plan" to "Daily planner", "Diary" to "Write in diary", "Note" to "Save a note", "Goal" to "Goals & dreams").forEach { (type, label) ->
                                Card(Modifier.fillMaxWidth().clickable { addType = type; showAdd = true }, colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FavoriteBorder, null, tint = Purple); Spacer(Modifier.width(12.dp)); Text(label, color = DeepPurple, fontWeight = FontWeight.Medium); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Purple) } }
                            }
                            Text("Saved entries: ${entries.size}", color = Muted)
                        }
                        "Planner", "Diary" -> EntryList(tab, entries.filter { if (tab == "Planner") it.type == "Plan" else it.type in listOf("Diary", "Note", "Goal") }, onAdd = { addType = if (tab == "Planner") "Plan" else "Diary"; showAdd = true }, onDelete = { id -> entries = entries.filterNot { it.id == id }; saveEntries(prefs, entries) })
                        "Mood" -> Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Mood check-in", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepPurple)
                            Text("There is no wrong way to feel.", color = Muted)
                            listOf("Great 😊", "Calm 😌", "Okay 🙂", "Low 😔", "Anxious 😟").forEach { option -> Text(option, modifier = Modifier.fillMaxWidth().clickable { mood = option.substringBefore(" "); prefs.edit().putString("mood", mood).apply() }.background(if (mood == option.substringBefore(" ")) Lavender else Color.White, RoundedCornerShape(14.dp)).padding(16.dp), color = DeepPurple) }
                            Button(onClick = { addType = "Mood"; showAdd = true }) { Text("Write a mood note") }
                        }
                        else -> Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("My Private Space", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepPurple)
                            Text("A place for the things you want to keep close.", color = Muted)
                            Button(onClick = { addType = "Note"; showAdd = true }) { Text("Write a private note") }
                            OutlinedButton(onClick = { pinDialog = true }) { Text(if (pinSet) "Change privacy PIN" else "Create privacy PIN") }
                            if (pinSet) OutlinedButton(onClick = { locked = true }) { Text("Lock app now") }
                            Text("Privacy note: entries are stored locally but are not encrypted in this first build. Do not store highly sensitive information.", fontSize = 12.sp, color = Muted)
                        }
                    }
                }
            }
            if (showAdd) AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Add ${addType.lowercase()}") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(body, { body = it }, label = { Text("Details / reflection") }, minLines = 3)
                    Text("Saved only on this device.", fontSize = 12.sp, color = Muted)
                }
            }, confirmButton = { TextButton(onClick = { if (title.isNotBlank()) { entries = listOf(Entry(System.currentTimeMillis(), addType, title.trim(), body.trim(), today())) + entries; saveEntries(prefs, entries); title = ""; body = ""; showAdd = false } }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } })
            if (pinDialog) AlertDialog(onDismissRequest = { pinDialog = false; pin = "" }, title = { Text("Set privacy PIN") }, text = { OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("4–8 digit PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)) }, confirmButton = { TextButton(onClick = { if (pin.length in 4..8) { prefs.edit().putString("pin", pin).apply(); pinSet = true; locked = false; pinDialog = false; pin = "" } }) { Text("Save PIN") } }, dismissButton = { TextButton(onClick = { pinDialog = false }) { Text("Cancel") } })
        }
    }
}

@Composable
private fun EntryList(tab: String, entries: List<Entry>, onAdd: () -> Unit, onDelete: (Long) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(if (tab == "Planner") "Daily planner" else "Your diary", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = DeepPurple, modifier = Modifier.weight(1f)); Button(onClick = onAdd) { Icon(Icons.Default.Add, null); Text("Add") } }
        Spacer(Modifier.height(12.dp))
        if (entries.isEmpty()) Text("Nothing here yet. Tap Add to begin.", color = Muted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(entries, key = { it.id }) { entry -> Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(entry.title, fontWeight = FontWeight.Bold, color = DeepPurple); Text("${entry.type} • ${entry.date}", fontSize = 11.sp, color = Muted) }; IconButton(onClick = { onDelete(entry.id) }) { Icon(Icons.Default.DeleteOutline, null) } }
                if (entry.body.isNotBlank()) Text(entry.body, color = Color(0xFF2F2938))
            } } }
        }
    }
}
private fun saveEntries(prefs: android.content.SharedPreferences, entries: List<Entry>) {
    val array = JSONArray()
    entries.forEach { array.put(JSONObject().put("id", it.id).put("type", it.type).put("title", it.title).put("body", it.body).put("date", it.date)) }
    prefs.edit().putString("entries", array.toString()).apply()
}
private fun readEntries(prefs: android.content.SharedPreferences): List<Entry> = try {
    val a = JSONArray(prefs.getString("entries", "[]") ?: "[]")
    (0 until a.length()).map { val o = a.getJSONObject(it); Entry(o.optLong("id"), o.optString("type"), o.optString("title"), o.optString("body"), o.optString("date")) }.sortedByDescending { it.id }
} catch (_: Exception) { emptyList() }
