package com.sanathanatattva.partner.alerts

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * The ring while the order screen is open: the phone's ringtone on loop, paused while a
 * voice reads the order out. Kannada voice when the app is in Kannada and the phone has it,
 * otherwise Indian English (handoff §6.1).
 */
class AlertSound(context: Context, private val languageTag: String) : TextToSpeech.OnInitListener {
    private val app = context.applicationContext
    private val ringtone: Ringtone? = RingtoneManager.getRingtone(app, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))?.apply {
        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= 28) isLooping = true
    }
    private val tts = TextToSpeech(app, this)
    private var ttsReady = false
    private var pendingSpeech: String? = null
    private var stopped = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        ttsReady = pick(Locale.forLanguageTag(languageTag)) || pick(Locale.forLanguageTag("en-IN")) || pick(Locale.ENGLISH)
        tts.setSpeechRate(0.9f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { if (!stopped) ringtone?.play() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { if (!stopped) ringtone?.play() }
        })
        pendingSpeech?.let { announce(it) }
        pendingSpeech = null
    }

    private fun pick(locale: Locale) =
        tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE && tts.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE

    fun ring() {
        stopped = false
        ringtone?.play()
    }

    /** Pauses the ring, reads [text], then rings again. */
    fun announce(text: String) {
        if (!ttsReady) {
            pendingSpeech = text
            return
        }
        ringtone?.stop()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "order")
    }

    fun stop() {
        stopped = true
        pendingSpeech = null
        ringtone?.stop()
        if (ttsReady) tts.stop()
    }

    fun release() {
        stop()
        tts.shutdown()
    }
}

/** "New order. 2 times 5 litre groundnut oil. Refill only, don't pack new cans. …" */
fun announcementFor(offer: Offer): String = buildString {
    append(if (offer.forRider) "New delivery. " else "New order. ")
    offer.lines.forEach { line ->
        append("${line.qty} times ${line.litres} litre ${line.oil}. ")
        when (line.kind) {
            LineKind.Refill -> append(if (offer.forRider) "Refill only, don't take new cans. " else "Refill only, don't pack new cans. ")
            LineKind.New -> append(if (offer.forRider) "Take a new can. " else "Pack a new can. ")
            LineKind.Standard -> Unit
        }
    }
    if (offer.forRider) append("Pick up from ${offer.pickupName}. You earn ${offer.money.removePrefix("₹")} rupees. ")
    else append("For ${offer.customer}, ${offer.area}. ")
    if (offer.test) append("This is a test alert.")
}
