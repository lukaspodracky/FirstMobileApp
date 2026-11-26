package com.firstmobileapp.lightswitch

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Tiny synthesized sound effects, so the app needs no audio files. */
class SoundFx {
    var enabled = true

    private val rate = 22050
    private val tracks = HashMap<String, AudioTrack>()

    init {
        try {
            tracks["on"] = track(tones(listOf(880.0), 0.07))
            tracks["off"] = track(tones(listOf(587.0), 0.07))
            tracks["click"] = track(tones(listOf(660.0), 0.04, 0.25))
            tracks["hint"] = track(tones(listOf(988.0, 1319.0), 0.07, 0.25))
            tracks["win"] = track(tones(listOf(523.0, 659.0, 784.0, 1047.0, 1319.0), 0.11))
        } catch (_: Exception) {
            // No audio available: play silently.
        }
    }

    private fun tones(freqs: List<Double>, noteSec: Double, volume: Double = 0.35): ShortArray {
        val per = (rate * noteSec).toInt()
        val tail = (rate * 0.12).toInt()
        val out = ShortArray(per * freqs.size + tail)
        freqs.forEachIndexed { idx, f ->
            val len = per + tail
            for (s in 0 until len) {
                val pos = idx * per + s
                if (pos >= out.size) break
                val t = s.toDouble() / rate
                val attack = minOf(1.0, s / (rate * 0.004))
                val env = attack * exp(-t * 18.0)
                val v = sin(2 * PI * f * t) * 0.8 + sin(4 * PI * f * t) * 0.2
                val mixed = out[pos] + (v * env * volume * Short.MAX_VALUE).toInt()
                out[pos] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return out
    }

    private fun track(data: ShortArray): AudioTrack {
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(data, 0, data.size)
        return t
    }

    fun play(name: String) {
        if (!enabled) return
        val t = tracks[name] ?: return
        try {
            t.stop()
            t.reloadStaticData()
            t.play()
        } catch (_: Exception) {
        }
    }

    fun release() {
        tracks.values.forEach { it.release() }
        tracks.clear()
    }
}
