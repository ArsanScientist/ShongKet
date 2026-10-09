package com.example.shongket.data

import android.content.Context
import com.example.shongket.core.PhrasebookData

/** Loads the bundled phrasebook once and keeps it for the life of the process. */
object PhrasebookRepository {

    @Volatile
    private var cached: PhrasebookData? = null

    fun get(context: Context): PhrasebookData =
        cached ?: synchronized(this) {
            cached ?: Assets.readObject<PhrasebookData>(context, "phrasebook.json").also {
                cached = it
            }
        }
}











