package com.chandni.kavos

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class AIAdvisorActivity : AppCompatActivity() {

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val mainHandler = Handler(Looper.getMainLooper())

    private val daySuggestions = listOf(
        "I think someone is following me",
        "I'm at a party and feel uncomfortable",
        "Someone won't take no for an answer",
        "Is this address safe to visit alone?",
        "How do I plan a safer commute?"
    )

    private val nightSuggestions = listOf(
        "I need to walk home alone right now",
        "My cab is taking a different route",
        "I think someone is following me",
        "Should I take this auto at this hour?",
        "Best route home if streetlights are out?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_advisor)
        supportActionBar?.hide()

        val hero = findViewById<View>(R.id.heroHeader)
        hero.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(getColor(R.color.grad_indigo_a), getColor(R.color.grad_indigo_b))
        )

        adapter = ChatAdapter(messages)
        val rv = findViewById<RecyclerView>(R.id.rvChat)
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rv.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnNewChat).setOnClickListener {
            messages.clear()
            adapter.notifyDataSetChanged()
            startConversation()
        }

        val input = findViewById<EditText>(R.id.etInput)
        val send = findViewById<ImageButton>(R.id.btnSend)
        send.setOnClickListener { sendCurrent(input) }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendCurrent(input); true } else false
        }

        buildSuggestions(input)
        startConversation()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun startConversation() {
        addAiMessage(staticWelcome())
        if (GeminiClient.apiKey(this) == null) {
            addAiMessage(
                "Heads up: AI Advisor needs a free Gemini key. Open Profile → AI Settings to enable it. " +
                    "Until then, every other safety feature still works."
            )
        }
    }

    private fun staticWelcome(): String {
        val name = getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE)
            .getString("user_name", null)?.split(" ")?.firstOrNull()
        val greeting = if (name != null) "Hi $name, I'm Saheli." else "Hi, I'm Saheli."
        return "$greeting I'm here to help you think clearly when something feels off.\n\n" +
            "Tell me what's happening — I'll give you 4-6 quick steps. Tap a suggestion below or describe your own situation."
    }

    private fun buildSuggestions(input: EditText) {
        val row = findViewById<LinearLayout>(R.id.suggestionsRow)
        val density = resources.displayMetrics.density
        val mEnd = (8 * density).toInt()
        val padH = (14 * density).toInt()
        val padV = (8 * density).toInt()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val suggestions = if (hour in 21..23 || hour in 0..5) nightSuggestions else daySuggestions
        for (s in suggestions) {
            val tv = TextView(this).apply {
                text = s
                textSize = 12f
                setTextColor(getColor(R.color.purple_text))
                setPadding(padH, padV, padH, padV)
                background = getDrawable(R.drawable.chip_suggestion_bg)
                setOnClickListener {
                    input.setText(s)
                    input.setSelection(s.length)
                    sendCurrent(input)
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = mEnd
            tv.layoutParams = lp
            row.addView(tv)
        }
    }

    private fun sendCurrent(input: EditText) {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        val key = GeminiClient.apiKey(this)
        if (key == null) {
            promptOpenProfileForKey()
            return
        }
        if (!GeminiClient.isOnline(this)) {
            Toast.makeText(this, "AI Advisor needs internet. Try again when online.", Toast.LENGTH_LONG).show()
            return
        }

        addUserMessage(text)
        input.setText("")
        addAiMessage("…Saheli is typing", isPlaceholder = true)

        val historyForApi = messages
            .filterIndexed { idx, m -> idx != messages.lastIndex && !m.text.startsWith("…") }
            .toList()

        Thread {
            try {
                val reply = GeminiClient.ask(key, historyForApi, text)
                mainHandler.post { replacePlaceholderWith(reply) }
            } catch (e: Exception) {
                mainHandler.post {
                    val msg = e.message ?: "Could not reach AI"
                    val friendly = when {
                        msg.contains("API key", ignoreCase = true) ||
                            msg.contains("API_KEY", ignoreCase = true) ||
                            msg.contains("403") ->
                            "API key was rejected. Open Profile → AI Settings and paste a fresh one from aistudio.google.com."
                        msg.contains("Unable to resolve host", ignoreCase = true) ->
                            "Lost connection. Check internet and try again."
                        else -> "AI couldn't respond: $msg"
                    }
                    replacePlaceholderWith(friendly)
                }
            }
        }.start()
    }

    private fun promptOpenProfileForKey() {
        AlertDialog.Builder(this)
            .setTitle("Enable AI Advisor")
            .setMessage(
                "AI Advisor needs a free Gemini API key. Get one at aistudio.google.com → Get API key, " +
                    "then paste it in Profile → AI Settings. The key is stored only on this phone."
            )
            .setPositiveButton("Open Profile") { _, _ ->
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun replacePlaceholderWith(text: String) {
        val idx = messages.indexOfLast { it.text.startsWith("…") && !it.fromUser }
        if (idx >= 0) {
            messages[idx] = ChatMessage(text, fromUser = false)
            adapter.notifyItemChanged(idx)
        } else {
            addAiMessage(text)
        }
        scrollToEnd()
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text, fromUser = true))
        adapter.notifyItemInserted(messages.size - 1)
        scrollToEnd()
    }

    private fun addAiMessage(text: String, isPlaceholder: Boolean = false) {
        val finalText = if (isPlaceholder) "…Saheli is typing" else text
        messages.add(ChatMessage(finalText, fromUser = false))
        adapter.notifyItemInserted(messages.size - 1)
        scrollToEnd()
    }

    private fun scrollToEnd() {
        findViewById<RecyclerView>(R.id.rvChat).post {
            findViewById<RecyclerView>(R.id.rvChat).smoothScrollToPosition(messages.size - 1)
        }
    }

    private class ChatAdapter(val items: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val avatar: FrameLayout = view.findViewById(R.id.avatarContainer)
            val name: TextView = view.findViewById(R.id.tvName)
            val bubble: TextView = view.findViewById(R.id.tvBubble)
            val row: LinearLayout = view as LinearLayout
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val msg = items[position]
            holder.bubble.text = msg.text
            val ctx = holder.itemView.context
            if (msg.fromUser) {
                holder.avatar.visibility = View.GONE
                holder.name.visibility = View.GONE
                holder.bubble.setBackgroundResource(R.drawable.bubble_user)
                holder.bubble.setTextColor(Color.WHITE)
                holder.row.gravity = Gravity.END
            } else {
                holder.avatar.visibility = View.VISIBLE
                holder.name.visibility = View.VISIBLE
                holder.name.setTextColor(ctx.getColor(R.color.purple_text))
                holder.bubble.setBackgroundResource(R.drawable.bubble_ai)
                holder.bubble.setTextColor(ctx.getColor(R.color.purple_text))
                holder.row.gravity = Gravity.START
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
