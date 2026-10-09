package com.kabasik007.wrongulator.ui

import android.media.AudioManager
import android.media.ToneGenerator

/** Self-contained retro sound cues: no sound assets, downloads or runtime permission. */
internal class SnakeSounds : AutoCloseable {
    private val tones = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 46)
    }.getOrNull()

    enum class Cue { START, TURN, EAT, LOSE, PAUSE, BLAST, EMPTY }

    fun play(cue: Cue, enabled: Boolean) {
        if (!enabled) return
        val (tone, duration) = when (cue) {
            Cue.START -> ToneGenerator.TONE_DTMF_5 to 110
            Cue.TURN -> ToneGenerator.TONE_PROP_BEEP to 25
            Cue.EAT -> ToneGenerator.TONE_DTMF_9 to 95
            Cue.LOSE -> ToneGenerator.TONE_PROP_NACK to 170
            Cue.PAUSE -> ToneGenerator.TONE_PROP_ACK to 75
            Cue.BLAST -> ToneGenerator.TONE_DTMF_D to 180
            Cue.EMPTY -> ToneGenerator.TONE_PROP_NACK to 55
        }
        runCatching { tones?.startTone(tone, duration) }
    }

    override fun close() {
        runCatching { tones?.release() }
    }
}
