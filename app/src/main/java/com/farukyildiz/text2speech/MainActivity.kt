package com.farukyildiz.text2speech

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.farukyildiz.text2speech.databinding.ActivityMainBinding
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {

    private val LOG_TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        initTextToSpeech()
        setContentView(binding.root)


        binding.buttonListen.setOnClickListener {

            GlobalScope.launch(Dispatchers.IO) {
                val message = binding.editText.text.toString()
                val langCode = detectMessageLanguage(message)
                Log.e(LOG_TAG, "langCode: $langCode")

                launch(Dispatchers.Main) {
                    listenMessage(language = langCode, message = message)
                }
            }

        }
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this) {
            if (it != TextToSpeech.SUCCESS) {
                Toast.makeText(this, R.string.text_to_speech_not_supported, Toast.LENGTH_LONG).show()
                return@TextToSpeech
            }

            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(p0: String?) {
                    runOnUiThread { onChangedTextToSpeechStatus(status = true) }
                }

                override fun onDone(p0: String?) {
                    runOnUiThread { onChangedTextToSpeechStatus(status = false) }
                }

                override fun onError(p0: String?) {
                    Log.e("textToSpeech", "onError")
                }
            })

        }
    }

    private fun listenMessage(language: String, message: String) {
        val locale = Locale.forLanguageTag(language)
        val result = textToSpeech.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            val displayLanguage = locale.displayLanguage
            Toast.makeText(this, getString(R.string.text_to_speech_lang_not_supported, displayLanguage), Toast.LENGTH_LONG).show()
        } else {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
        }
    }

    fun onChangedTextToSpeechStatus(status: Boolean) {
        if (status) {
            binding.textViewStatus.text = "Status: listening..."
        } else {
            binding.textViewStatus.text = "Status: listening done"
        }
    }

    private suspend fun detectMessageLanguage(message: String): String {
        return suspendCoroutine { continuation ->
            LanguageIdentification.getClient().identifyLanguage(message).addOnSuccessListener { result ->
                if (result != "und") {
                    continuation.resume(result)
                } else {
                    continuation.resume("en")
                }
            }.addOnFailureListener {
                continuation.resume("en")
            }
        }
    }

}