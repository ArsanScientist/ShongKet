package com.example.shongket.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shongket.ShongketApplication
import com.example.shongket.data.AppPrefs
import com.example.shongket.data.download.HfDownloadRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShongketApplication
    val prefs: AppPrefs get() = app.prefs

    val language: StateFlow<String> = app.prefs.language
    val activeRegion: StateFlow<String> = app.prefs.activeRegion
    val installedRegions: StateFlow<Set<String>> = app.prefs.installedRegions

    fun setLanguage(lang: String) = app.prefs.setLanguage(lang)

    fun installRegion(id: String) = app.prefs.installRegion(id)

    fun activateRegion(id: String) = app.prefs.setActiveRegion(id)

    fun clearChatHistory() {
        viewModelScope.launch {
            app.chatRepository.clearAllChats()
        }
    }

    fun deleteModel() {
        val f = HfDownloadRepository.modelFile(getApplication())
        if (f.exists()) f.delete()
    }
}











