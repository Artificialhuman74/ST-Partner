package com.sanathanatattva.partner.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Reads screen text aloud (intro narration now, order announcements later).
 * Kannada voice data is missing on some phones (handoff §6.1); then it falls back to
 * Indian English, and if that is missing too it stays silent rather than failing.
 */
class Narrator(context: Context, private val languageTag: String) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        ready = pickVoice(Locale.forLanguageTag(languageTag)) || pickVoice(Locale.forLanguageTag("en-IN")) || pickVoice(Locale.ENGLISH)
        tts.setSpeechRate(0.92f)
        pending?.let { speak(it) }
        pending = null
    }

    private fun pickVoice(locale: Locale): Boolean =
        tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE &&
            tts.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE

    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "st-narration")
    }

    fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
