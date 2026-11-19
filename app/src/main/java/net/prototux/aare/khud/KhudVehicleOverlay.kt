package net.prototux.aare.khud

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * KHUD fork: vehicle info drawn over whatever the car screen shows (the
 * navigation apps' own Android Auto screens on the K20 Pro). Values come
 * read-only from the KHUD Carlink app (`VehicleSignalsProvider`, schema
 * khud.vehicle v1), which listens to the existing ESP32 M-CAN feed. Nothing is
 * drawn without that app or while its ESP32 link is down; unknown values stay
 * hidden. No navigation/TBT is drawn here: the navigation app shows its own.
 */
@Composable
fun KhudVehicleOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<KhudVehicle?>(null) }
    LaunchedEffect(Unit) {
        var misses = 0
        while (isActive) {
            val read = withContext(Dispatchers.IO) { KhudVehicleSource.read(context) }
            state = read
            misses = if (read == null) misses + 1 else 0
            // Without the KHUD app, back off instead of polling a missing provider.
            delay(if (misses > 10) 5_000L else 200L)
        }
    }
    val s = state ?: return
    if (!s.link) return
    Box(modifier.fillMaxSize().padding(10.dp)) {
        TopChip(s, Modifier.align(Alignment.TopCenter))
        Column(Modifier.align(Alignment.CenterEnd), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.hvacPopup && s.hvac != null) HvacCard(s.hvac)
            if (s.parking?.any == true) ParkingCard(s.parking)
        }
    }
}

private val CardColor = Color(0xD9101418)

@Composable
private fun TopChip(s: KhudVehicle, modifier: Modifier) {
    if (s.gear == null && s.blinkLeft == null && s.blinkRight == null && s.wiper == null) return
    Row(modifier.background(CardColor, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (s.blinkLeft != null) Text("◀", color = if (s.blinkLeft) Color(0xFF34D058) else Color(0x33FFFFFF), fontSize = 22.sp)
        if (s.gear != null) Text(s.gear, color = if (s.gear == "R") Color(0xFFFF5A5A) else Color.White,
            fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (s.wiper != null) Text("와이퍼 ${s.wiper}", color = Color(0xFF9BD1FF), fontSize = 16.sp)
        if (s.blinkRight != null) Text("▶", color = if (s.blinkRight) Color(0xFF34D058) else Color(0x33FFFFFF), fontSize = 22.sp)
    }
}

@Composable
private fun HvacCard(h: KhudVehicle.Hvac) {
    Column(Modifier.width(210.dp).background(CardColor, RoundedCornerShape(16.dp)).padding(12.dp)) {
        if (h.off) { Text("공조 OFF", color = Color.White, fontSize = 20.sp); return@Column }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.width(186.dp)) {
            Text(temp(h.driverTemp10), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(temp(h.passengerTemp10), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        val flags = listOfNotNull(h.fan?.let { "팬 $it" }, "AUTO".takeIf { h.auto }, "A/C".takeIf { h.ac },
            "내기".takeIf { h.recirc }, "제상".takeIf { h.defrost })
        Text(flags.joinToString(" · "), color = Color(0xFFB8C4CC), fontSize = 15.sp)
    }
}

@Composable
private fun ParkingCard(p: KhudVehicle.Parking) {
    Column(Modifier.width(210.dp).background(CardColor, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("주차 보조", color = Color(0xFFB8C4CC), fontSize = 14.sp)
        ZoneRow(p.front)
        Spacer(Modifier.height(18.dp))
        ZoneRow(p.rear)
    }
}

@Composable
private fun ZoneRow(levels: List<Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        levels.forEach { level ->
            Box(Modifier.size(width = 56.dp, height = 12.dp).background(zoneColor(level), RoundedCornerShape(6.dp)))
        }
    }
}

private fun zoneColor(level: Int) = when (level) {
    0 -> Color(0x33FFFFFF)
    1 -> Color(0xFFFFD43B)
    2 -> Color(0xFFFF922B)
    else -> Color(0xFFFF3B3B)
}

private fun temp(t10: Int?) = t10?.let { "${it / 10}.${it % 10}°" } ?: "--"

/** Reads the KHUD Carlink provider; null when it is not installed or refuses. */
internal object KhudVehicleSource {
    private val URI: Uri = Uri.parse("content://com.freezed23.khudcarlink.lab.vehicle")

    fun read(context: Context): KhudVehicle? = try {
        context.contentResolver.call(URI, "signals", null, null)?.getString("json")?.let(KhudVehicle::parse)
    } catch (_: Exception) {
        null
    }
}

/** khud.vehicle v1 as sent by the KHUD Carlink app; unknown values stay null. */
data class KhudVehicle(
    val link: Boolean,
    val gear: String?,
    val hvac: Hvac?,
    val hvacPopup: Boolean,
    val parking: Parking?,
    val blinkLeft: Boolean?,
    val blinkRight: Boolean?,
    val wiper: String?,
) {
    data class Hvac(val off: Boolean, val driverTemp10: Int?, val passengerTemp10: Int?, val auto: Boolean,
                    val ac: Boolean, val recirc: Boolean, val defrost: Boolean, val fan: Int?)
    data class Parking(val front: List<Int>, val rear: List<Int>) {
        val any: Boolean get() = front.any { it > 0 } || rear.any { it > 0 }
    }

    companion object {
        fun parse(json: String): KhudVehicle? = try {
            val o = JSONObject(json)
            if (o.optString("schema") != "khud.vehicle" || o.optInt("v") != 1) null else {
                fun JSONObject.intOrNull(k: String) = if (isNull(k) || !has(k)) null else optInt(k)
                fun JSONObject.boolOrNull(k: String) = if (isNull(k) || !has(k)) null else optBoolean(k)
                fun JSONObject.strOrNull(k: String) = if (isNull(k) || !has(k)) null else optString(k).takeIf { it.isNotBlank() }
                val h = o.optJSONObject("hvac")?.let {
                    Hvac(it.optBoolean("off"), it.intOrNull("driverTemp10"), it.intOrNull("passengerTemp10"),
                        it.optBoolean("auto"), it.optBoolean("ac"), it.optBoolean("recirc"), it.optBoolean("defrost"),
                        it.intOrNull("fan"))
                }
                val p = o.optJSONObject("parking")?.let { pk ->
                    fun list(k: String) = pk.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.optInt(it) } }.orEmpty()
                    Parking(list("front"), list("rear"))
                }
                KhudVehicle(o.optBoolean("link"), o.strOrNull("gear"), h, o.optBoolean("hvacPopup"), p,
                    o.boolOrNull("blinkLeft"), o.boolOrNull("blinkRight"), o.strOrNull("wiper"))
            }
        } catch (_: Exception) {
            null
        }
    }
}
