package com.chandni.kavos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class SOSHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_history)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { goHome() }

        loadHistory()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private fun loadHistory() {
        val container = findViewById<LinearLayout>(R.id.historyContainer)
        val tvEmpty = findViewById<View>(R.id.tvEmpty)
        val db = AppDatabase.getDatabase(this)

        Thread {
            val logs = db.sosLogDao().getAllLogsForUser(UserScope.uid())
            runOnUiThread {
                if (logs.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    return@runOnUiThread
                }
                tvEmpty.visibility = View.GONE
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

                for (log in logs) {
                    val card = layoutInflater.inflate(R.layout.item_sos_log, container, false)
                    card.findViewById<TextView>(R.id.tvLogTime).text = sdf.format(Date(log.timestamp))
                    card.findViewById<TextView>(R.id.tvLogMethod).text = "Trigger: ${log.triggerMethod}"

                    val locText = card.findViewById<TextView>(R.id.tvLogLocation)
                    locText.text = "Location: https://maps.google.com/?q=${log.location}"

                    val ackRow = card.findViewById<View>(R.id.llAckRow)
                    val ackTitle = card.findViewById<TextView>(R.id.tvAckTitle)
                    val ackReply = card.findViewById<TextView>(R.id.tvAckReply)

                    val ackBy = log.acknowledgedBy
                    val ackAt = log.acknowledgedAt
                    if (!ackBy.isNullOrEmpty() && ackAt != null) {
                        ackRow.visibility = View.VISIBLE
                        val cls = runCatching { ReplyClass.valueOf(log.replyClassification.orEmpty()) }
                            .getOrNull()
                        val verdict = when (cls) {
                            ReplyClass.COMING_NOW -> "✓ $ackBy is on the way"
                            ReplyClass.ACKNOWLEDGED -> "✓ $ackBy got your alert"
                            ReplyClass.CANT_HELP -> "⚠ $ackBy can't help right now"
                            ReplyClass.UNCLEAR, null -> "✓ Acknowledged by $ackBy"
                        }
                        val deltaMin = ((ackAt - log.timestamp) / 60000L).coerceAtLeast(0L)
                        ackTitle.text = if (deltaMin == 0L) verdict
                        else "$verdict · ${deltaMin} min later"
                        ackReply.text = "\"${log.replyText.orEmpty()}\""
                    } else {
                        ackRow.visibility = View.GONE
                    }

                    container.addView(card)
                }
            }
        }.start()
    }
}
