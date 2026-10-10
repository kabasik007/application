package com.kabasik007.wrongulator.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tiny procedural game SFX rendered to WAV in cache and played through SoundPool.
 * No licensed samples, no internet, no runtime permissions, bounded by 7 clips.
 *
 * A ToneGenerator fallback covers the first moment while SoundPool loads.
 */
internal class SnakeAudioBank(context: Context) : AutoCloseable {
    private val cache = context.applicationContext.cacheDir
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        ).build()
    private val fallback = SnakeSounds()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    private val sampleIds = mutableMapOf<SnakeSounds.Cue, Int>()
    @Volatile private var closed = false

    init {
        pool.setOnLoadCompleteListener { _, sample, status ->
            if (!closed && status == 0) loaded.add(sample)
        }
    }

    suspend fun prepare() {
        val files = withContext(Dispatchers.IO) {
            SnakeSounds.Cue.entries.associateWith { cue ->
                val file = File(cache, "snake_generated_v2_${cue.name}.wav")
                if (!file.exists() || file.length() < 100) {
                    file.writeBytes(synth(cue))
                }
                file
            }
        }
        if (!closed) for ((cue, file) in files) {
            sampleIds[cue] = pool.load(file.absolutePath, 1)
        }
    }

    fun play(cue: SnakeSounds.Cue, enabled: Boolean) {
        if (closed || !enabled) return
        val id = sampleIds[cue]
        if (id != null && id in loaded) {
            pool.play(id, .66f, .66f, 1, 0, 1f)
        } else {
            fallback.play(cue, true)
        }
    }

    override fun close() {
        closed = true
        loaded.clear()
        runCatching { pool.release() }
        fallback.close()
    }

    private fun synth(cue: SnakeSounds.Cue): ByteArray {
        val (ms, from, to) = when (cue) {
            SnakeSounds.Cue.START -> Triple(140, 420.0, 660.0)
            SnakeSounds.Cue.TURN -> Triple(35, 350.0, 390.0)
            SnakeSounds.Cue.EAT -> Triple(115, 560.0, 960.0)
            SnakeSounds.Cue.LOSE -> Triple(245, 460.0, 140.0)
            SnakeSounds.Cue.PAUSE -> Triple(95, 500.0, 380.0)
            SnakeSounds.Cue.BLAST -> Triple(250, 200.0, 54.0)
            SnakeSounds.Cue.EMPTY -> Triple(72, 220.0, 180.0)
        }
        val sampleRate = 22050
        val sampleCount = ms * sampleRate / 1000
        val bytes = ByteBuffer.allocate(44 + sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        fun ascii(value: String) { bytes.put(value.toByteArray(Charsets.US_ASCII)) }
        ascii("RIFF")
        bytes.putInt(36 + sampleCount * 2)
        ascii("WAVE")
        ascii("fmt ")
        bytes.putInt(16)
        bytes.putShort(1) // PCM
        bytes.putShort(1) // mono
        bytes.putInt(sampleRate)
        bytes.putInt(sampleRate * 2)
        bytes.putShort(2)
        bytes.putShort(16)
        ascii("data")
        bytes.putInt(sampleCount * 2)
        val noise = Random(cue.ordinal + 39)
        var phase = 0.0
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleCount
            val frequency = from + (to - from) * t
            phase += 2.0 * PI * frequency / sampleRate
            val attack = min(1.0, t * 60.0)
            val envelope = attack * (1.0 - t).pow(1.7)
            val signal = sin(phase) * .75 + sin(phase * 2.0) * .22
            val grit = if (cue == SnakeSounds.Cue.BLAST) {
                (noise.nextDouble() * 2 - 1) * .45 * (1 - t)
            } else 0.0
            val value = ((signal + grit) * envelope * 22000.0)
                .toInt().coerceIn(-32768, 32767)
            bytes.putShort(value.toShort())
        }
        return bytes.array()
    }
}
