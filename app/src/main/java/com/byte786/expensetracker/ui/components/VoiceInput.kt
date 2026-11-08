package com.byte786.expensetracker.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.byte786.expensetracker.data.Account
import com.byte786.expensetracker.data.Category
import com.byte786.expensetracker.data.VoiceParser

/** Uses the phone's own speech recognition (English), then [VoiceParser] to fill a record. */
object VoiceInput {
    private fun intent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        .putExtra(RecognizerIntent.EXTRA_PROMPT, "e.g. \"500 bike mobil from cash\"")

    fun launch(ctx: Context, launcher: ActivityResultLauncher<Intent>) {
        try {
            launcher.launch(intent())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "No speech recognition on this phone. Install or enable the Google app.", Toast.LENGTH_LONG).show()
        }
    }

    fun parse(text: String, cats: List<Category>, accs: List<Account>): VoiceParser.Draft = VoiceParser.parse(
        text,
        cats.map { VoiceParser.Cat(it.id, it.name, it.type, it.parentId) },
        accs.map { VoiceParser.Acc(it.id, it.name) },
    )
}
