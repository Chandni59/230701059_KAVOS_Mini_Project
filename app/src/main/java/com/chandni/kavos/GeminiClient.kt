package com.chandni.kavos

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class ReplyClass { COMING_NOW, ACKNOWLEDGED, CANT_HELP, UNCLEAR }

data class ChatMessage(val text: String, val fromUser: Boolean)

/**
 * All Gemini calls are best-effort. Every public method returns null on
 * failure (no key, offline, timeout, malformed response). Callers must
 * always have a deterministic fallback path — AI never blocks safety
 * features.
 */
object GeminiClient {

    private const val MODEL = "gemini-2.5-flash"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private const val PREFS = "KAVOS_PREFS"
    private const val KEY_API = "gemini_api_key"
    private const val KEY_HOME_AI = "ai_home_enabled"

    fun apiKey(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_API, null)?.takeIf { it.isNotBlank() }

    fun setApiKey(context: Context, key: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_API, key?.trim()).apply()
    }

    fun homeAiEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_HOME_AI, false)

    fun setHomeAiEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HOME_AI, enabled).apply()
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * One-line SOS opener tuned to the user's current context. Returns null
     * if anything fails so the caller falls back to its static message.
     * Hard cap on latency: ~3s.
     */
    fun composeSosOpener(
        apiKey: String,
        firstName: String?,
        triggerMethod: String,
        hourOfDay: Int,
        batteryPct: Int,
        spokenPhrase: String?
    ): String? {
        val system = """You are KAVOS, a personal-safety app, writing the FIRST line of an emergency SMS that will be sent to the user's guardians right now.

Output rules — exactly:
- One sentence, max 22 words.
- Lead with the user's first name if given. Then state: it's an emergency, the situation snapshot, and what the guardian should do.
- Mention concrete details from the context: time of day if night, low battery if <25%, the trigger reason (shake/voice/manual/timer), and the spoken phrase if any.
- No greetings, no emojis, no quotes, no "I", no markdown. The message is from the app on the user's behalf.
- Tone: calm, urgent, decisive. Indian English."""

        val ctx = buildString {
            append("First name: ").append(firstName ?: "(unknown)").append('\n')
            append("Trigger: ").append(triggerMethod).append('\n')
            append("Local hour: ").append(hourOfDay).append('\n')
            append("Battery %: ").append(batteryPct).append('\n')
            append("Spoken phrase: ").append(spokenPhrase ?: "(none)")
        }

        return runGemini(
            apiKey = apiKey,
            system = system,
            user = ctx,
            temperature = 0.4,
            maxTokens = 80,
            connectTimeoutMs = 1500,
            readTimeoutMs = 2500
        )?.singleLine()
    }

    /**
     * Classify a guardian's SMS reply. Returns null on failure; caller falls
     * back to UNCLEAR + raw text. Hard cap: ~4s (this runs after SOS, so we
     * can afford slightly more latency).
     */
    fun classifyReply(apiKey: String, replyText: String): ReplyClass? {
        val system = """You classify SMS replies sent by a "guardian" who just received a personal-safety SOS alert.

Output ONE token, no punctuation, no explanation:
- COMING_NOW   — they say they are coming, calling, or are physically en route now
- ACKNOWLEDGED — they confirm they got it but don't say they're acting yet (e.g. "ok", "got it", "k", "received")
- CANT_HELP    — they say they cannot help, are far, busy, or asking someone else to handle it
- UNCLEAR      — none of the above, or off-topic, or empty

Reply with the single label only."""

        val raw = runGemini(
            apiKey = apiKey,
            system = system,
            user = "Reply text:\n$replyText",
            temperature = 0.0,
            maxTokens = 8,
            connectTimeoutMs = 2000,
            readTimeoutMs = 3000
        )?.uppercase()?.trim() ?: return null

        return when {
            raw.contains("COMING_NOW") -> ReplyClass.COMING_NOW
            raw.contains("ACKNOWLEDGED") -> ReplyClass.ACKNOWLEDGED
            raw.contains("CANT_HELP") -> ReplyClass.CANT_HELP
            raw.contains("UNCLEAR") -> ReplyClass.UNCLEAR
            else -> null
        }
    }

    /**
     * Multi-turn chat for the AI Advisor screen. Caller must run on a
     * background thread. Throws on failure so the UI can show a specific
     * error (unlike the silent best-effort calls above).
     */
    fun ask(apiKey: String, history: List<ChatMessage>, userInput: String): String {
        val system = """You are Saheli — KAVOS's safety companion. "Saheli" means female friend in Hindi/Urdu. You are calm, decisive, and warm, like a trusted older sister who happens to be a safety expert. Your user is most often a woman in India.

Rules:
- Reply in 4-6 short numbered steps. No paragraphs, no fluff.
- Lead with the single most urgent action.
- Be specific to India: helplines 112, 1091, 100, 108. Mention KAVOS features the user already has (SOS button, Voice SOS, shake-to-trigger, Share Location, Fake Call, Siren, Safe Zones, Disguise mode, Trip Shield, Check-in Timer).
- Never tell them they are over-reacting. Validate the gut feeling.
- If the situation sounds life-threatening, the FIRST step must be: call 112 / hit the SOS button.
- Do not lecture about generic safety. Answer the specific situation they describe.
- End with one short reassuring sentence — vary the wording every time, never repeat the same closing line.
- Vary your phrasing across replies. Don't reuse the same opener twice in a conversation.
- Never refer to yourself as "AI", "model", "assistant", or "KAVOS". You are Saheli. Use "I" sparingly."""

        val url = URL("$ENDPOINT?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 30000
            setRequestProperty("Content-Type", "application/json")
        }

        val contents = JSONArray()
        for (msg in history) {
            val role = if (msg.fromUser) "user" else "model"
            contents.put(
                JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userInput)))
        )

        val body = JSONObject()
            .put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system)))
            )
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.85)
                    .put("topP", 0.95)
                    .put("maxOutputTokens", 600)
            )

        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) {
            val errMsg = try {
                JSONObject(response).optJSONObject("error")?.optString("message") ?: response
            } catch (_: Exception) { response }
            throw RuntimeException("Gemini error ($code): $errMsg")
        }

        val parts = JSONObject(response)
            .optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw RuntimeException("Malformed response")
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.getJSONObject(i).optString("text", ""))
        }
        return sb.toString().trim()
    }

    /**
     * Generates a 4-step "what to do right now" action plan tailored to the
     * user's current context. Returns null on failure so the caller falls
     * back to a static plan. Hard cap on latency: ~5s.
     */
    fun safetyPlan(
        apiKey: String,
        firstName: String?,
        hourOfDay: Int,
        batteryPct: Int,
        guardianCount: Int,
        tripShieldOn: Boolean,
        recentSosWithinHour: Boolean
    ): String? {
        val system = """You are KAVOS generating a personalised "next 30 minutes" safety plan for the user.

Output rules — exactly:
- Exactly 4 numbered steps (1. 2. 3. 4.).
- Each step is ONE imperative sentence, max 18 words.
- Order by urgency: most important first.
- Tailor every step to the context (time of day, battery, guardian count, Trip Shield state, recent SOS).
- Reference specific KAVOS features by name where relevant (Trip Shield, Voice SOS, Check-in Timer, Safe Zones, Share Location, Fake Call, Siren, Disguise).
- If guardians = 0, step 1 MUST be to add at least one guardian.
- If battery < 25, one step MUST address charging or low-power mode.
- If hour is 21-05, lean toward Trip Shield + sharing location with a guardian.
- If a recent SOS fired in the last hour, step 1 MUST address checking on the situation / replying to guardians.
- No greetings, no closing line, no emojis, no markdown beyond the numbers."""

        val ctx = buildString {
            append("First name: ").append(firstName ?: "(unknown)").append('\n')
            append("Local hour: ").append(hourOfDay).append('\n')
            append("Battery %: ").append(batteryPct).append('\n')
            append("Guardians: ").append(guardianCount).append('\n')
            append("Trip Shield: ").append(if (tripShieldOn) "ON" else "OFF").append('\n')
            append("Recent SOS within last hour: ").append(if (recentSosWithinHour) "YES" else "NO")
        }

        return runGemini(
            apiKey = apiKey,
            system = system,
            user = ctx,
            temperature = 0.4,
            maxTokens = 240,
            connectTimeoutMs = 2500,
            readTimeoutMs = 5000
        )?.trim()
    }

    /**
     * One-line tailored tip for the home-screen banner. Refreshed at most
     * once per hour by the caller.
     */
    fun homeBanner(
        apiKey: String,
        firstName: String?,
        hourOfDay: Int,
        batteryPct: Int,
        guardianCount: Int,
        tripShieldOn: Boolean
    ): String? {
        val system = """You are KAVOS writing a single proactive safety nudge for the home screen.

Output rules — exactly:
- One sentence, max 18 words.
- Address the user by first name if given.
- Tailor to the current context: time of day, battery, guardian count, whether Trip Shield is on.
- Reference at most one specific KAVOS feature by name (Trip Shield, Voice SOS, Check-in Timer, Safe Zones, Share Location, Fake Call, Siren).
- If guardians = 0, the nudge MUST be to add at least one guardian.
- If battery < 25, the nudge MUST mention charging or low battery.
- If hour is 21-05 and Trip Shield is OFF, lean toward suggesting Trip Shield.
- No greetings, no emojis, no markdown, no quotes."""

        val ctx = buildString {
            append("First name: ").append(firstName ?: "(unknown)").append('\n')
            append("Local hour: ").append(hourOfDay).append('\n')
            append("Battery %: ").append(batteryPct).append('\n')
            append("Guardians: ").append(guardianCount).append('\n')
            append("Trip Shield: ").append(if (tripShieldOn) "ON" else "OFF")
        }

        return runGemini(
            apiKey = apiKey,
            system = system,
            user = ctx,
            temperature = 0.5,
            maxTokens = 60,
            connectTimeoutMs = 2000,
            readTimeoutMs = 4000
        )?.singleLine()
    }

    // ── internals ────────────────────────────────────────────────────────────

    private fun runGemini(
        apiKey: String,
        system: String,
        user: String,
        temperature: Double,
        maxTokens: Int,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): String? {
        return try {
            val url = URL("$ENDPOINT?key=$apiKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Content-Type", "application/json")
            }

            val body = JSONObject()
                .put(
                    "system_instruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", system))
                    )
                )
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", user))
                            )
                    )
                )
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("temperature", temperature)
                        .put("maxOutputTokens", maxTokens)
                )

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code !in 200..299) return null

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val parts = JSONObject(response)
                .optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")
                ?: return null

            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                sb.append(parts.getJSONObject(i).optString("text", ""))
            }
            sb.toString().trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun String.singleLine(): String =
        replace(Regex("\\s+"), " ").trim().trim('"', '\'', '*', '`')
}
