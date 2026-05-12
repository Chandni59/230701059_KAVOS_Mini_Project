package com.chandni.kavos

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.sin

/**
 * Heavy emergency siren — not a morning alarm.
 *
 * Synthesizes PCM at runtime so it doesn't depend on any system ringtone:
 *  - a high wail that sweeps 600 Hz ↔ 1200 Hz over 0.55s (classic police siren),
 *  - a deep 90 Hz sub-bass mixed underneath for chest-thumping weight,
 *  - alarm-stream playback at max volume,
 *  - a heavy "thump-thump" vibration pattern.
 *
 * Why a synth instead of an MP3? The default RingtoneManager.TYPE_ALARM on
 * most phones is a soft xylophone tone that reads as a morning alarm, not
 * as danger. A wail + sub-bass reads as emergency in any culture.
 */
class SirenPlayer(private val context: Context) {

    private var track: AudioTrack? = null
    private var vibrator: Vibrator? = null
    private var renderThread: Thread? = null
    @Volatile private var running = false

    fun isRunning(): Boolean = running

    fun start() {
        if (running) return

        // Kick alarm volume to max so the siren actually sounds dangerous.
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(
            AudioManager.STREAM_ALARM,
            am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )

        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBuf * 2).coerceAtLeast(sampleRate / 4) // ~250ms

        val t = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track = t
        running = true
        t.play()

        renderThread = Thread {
            renderLoop(t, sampleRate, bufferSize)
        }.apply {
            priority = Thread.MAX_PRIORITY
            isDaemon = true
            start()
        }

        startHeavyVibration()
    }

    fun stop() {
        if (!running) return
        running = false
        try { track?.stop() } catch (_: Exception) {}
        try { track?.release() } catch (_: Exception) {}
        track = null
        try { renderThread?.interrupt() } catch (_: Exception) {}
        renderThread = null
        vibrator?.cancel()
    }

    private fun renderLoop(t: AudioTrack, sampleRate: Int, bufferSize: Int) {
        val chunk = ShortArray(bufferSize / 2)
        val sweepPeriodSec = 0.55                  // siren wail period
        val sweepPeriodSamples = sampleRate * sweepPeriodSec
        val twoPi = 2.0 * PI

        // Phase accumulators stay continuous across chunks so we don't get clicks.
        var phaseHigh = 0.0
        var phaseLow = 0.0
        var sweepPos = 0.0

        while (running) {
            for (i in chunk.indices) {
                // Triangle sweep between 0..1 used to drive the wail frequency.
                val t01 = (sweepPos / sweepPeriodSamples) % 1.0
                val sweep = if (t01 < 0.5) t01 * 2.0 else (1.0 - t01) * 2.0
                val highHz = 600.0 + 600.0 * sweep    // 600 → 1200 → 600 Hz

                val high = sin(phaseHigh)
                // Soft saturation on the wail makes it raspy/serious, not pure-tone-pretty.
                val raspedHigh = clip(high * 1.3)
                val sub = sin(phaseLow)               // 90 Hz sub-bass adds weight

                val mix = 0.7 * raspedHigh + 0.35 * sub
                val sample = (clip(mix) * 0.85 * Short.MAX_VALUE).toInt().toShort()
                chunk[i] = sample

                phaseHigh += twoPi * highHz / sampleRate
                phaseLow += twoPi * 90.0 / sampleRate
                if (phaseHigh > twoPi) phaseHigh -= twoPi
                if (phaseLow > twoPi) phaseLow -= twoPi
                sweepPos += 1.0
                if (sweepPos > sweepPeriodSamples) sweepPos -= sweepPeriodSamples
            }
            try {
                t.write(chunk, 0, chunk.size)
            } catch (_: Exception) {
                return
            }
        }
    }

    private fun clip(v: Double): Double =
        if (v > 1.0) 1.0 else if (v < -1.0) -1.0 else v

    private fun startHeavyVibration() {
        val v = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        vibrator = v
        // Heavy thump-thump pattern: long pulse, short rest, repeat.
        val pattern = longArrayOf(0, 500, 120, 500, 120, 700, 200)
        v.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }
}
