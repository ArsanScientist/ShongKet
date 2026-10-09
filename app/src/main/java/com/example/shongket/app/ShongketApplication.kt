package com.example.shongket

import android.app.Application
import com.example.shongket.core.SosSession
import com.example.shongket.data.AppPrefs
import com.example.shongket.data.ChatRepository
import com.example.shongket.data.CommunityRepository
import com.example.shongket.data.FamilyRepository
import com.example.shongket.data.SosRepository
import com.example.shongket.data.download.HfDownloadRepository
import com.example.shongket.inference.EngineHolder
import com.example.shongket.location.LocationProvider
import com.example.shongket.mesh.MeshHub
import java.io.File

class ShongketApplication : Application() {

    lateinit var chatRepository: ChatRepository
        private set
    lateinit var downloadRepository: HfDownloadRepository
        private set
    lateinit var engineHolder: EngineHolder
        private set
    lateinit var prefs: AppPrefs
        private set
    lateinit var sosRepository: SosRepository
        private set
    lateinit var communityRepository: CommunityRepository
        private set
    lateinit var familyRepository: FamilyRepository
        private set
    lateinit var locationProvider: LocationProvider
        private set
    lateinit var meshHub: MeshHub
        private set
    lateinit var sosSession: SosSession
        private set

    /**
     * A handover note composed on the translation screen, waiting to be sent as an SOS.
     *
     * Held in memory rather than passed as a navigation argument because the note is
     * multi-line free text and round-tripping it through a route string would mangle it.
     * Consumed exactly once by the mesh screen, then cleared.
     */
    var pendingSosDraft: String? = null

    /**
     * Whether this phone is currently advertising itself as a volunteer.
     *
     * Application-scoped for the same reason the SOS session is: a volunteer who switched
     * this on and put the phone in their pocket is still a volunteer, and the switch must
     * survive leaving the screen. It is off by default and never turns itself on.
     */
    val volunteerSharing = kotlinx.coroutines.flow.MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        chatRepository = ChatRepository(this)
        downloadRepository = HfDownloadRepository()
        engineHolder = EngineHolder(this)
        prefs = AppPrefs(this)
        sosRepository = SosRepository(File(filesDir, "sos_store.json"))
        communityRepository = CommunityRepository(File(filesDir, "community_store.json"))
        familyRepository = FamilyRepository(File(filesDir, "family_store.json"))
        locationProvider = LocationProvider(this)
        meshHub = MeshHub(this, sosRepository, communityRepository, familyRepository,
            locationProvider, prefs)
        sosSession = SosSession(this)
    }
}











