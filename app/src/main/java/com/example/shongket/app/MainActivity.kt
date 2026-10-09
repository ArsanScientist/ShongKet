package com.example.shongket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shongket.core.Triage
import com.example.shongket.data.RegionAssets
import com.example.shongket.data.SosEntry
import com.example.shongket.data.download.HfDownloadRepository
import com.example.shongket.ui.i18n.LocalBangla
import com.example.shongket.ui.community.CommunityScreen
import com.example.shongket.ui.community.CommunityViewModel
import com.example.shongket.ui.emergency.EmergencyScreen
import com.example.shongket.ui.family.FamilyScreen
import com.example.shongket.ui.family.FamilyViewModel
import com.example.shongket.ui.firstaid.FirstAidScreen
import com.example.shongket.ui.firstaid.FirstAidViewModel
import com.example.shongket.ui.gis.GisScreen
import com.example.shongket.ui.gis.GisViewModel
import com.example.shongket.ui.demo.Actions
import com.example.shongket.ui.guide.GuideScreen
import com.example.shongket.ui.mesh.MeshScreen
import com.example.shongket.ui.mesh.MeshViewModel
import com.example.shongket.ui.onboarding.OnboardingScreen
import com.example.shongket.ui.onboarding.OnboardingViewModel
import com.example.shongket.ui.settings.SettingsScreen
import com.example.shongket.ui.settings.SettingsViewModel
import com.example.shongket.ui.splash.BrandSplashScreen
import com.example.shongket.ui.summary.SummaryScreen
import com.example.shongket.ui.summary.SummaryViewModel
import com.example.shongket.ui.theme.ShongketTheme
import com.example.shongket.ui.translate.TranslateScreen
import com.example.shongket.ui.translate.TranslateViewModel
import com.example.shongket.ui.triage.TriageScreen
import com.example.shongket.ui.triage.TriageViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.shongket.ui.shell.ShongketShell
import com.example.shongket.ui.shell.Tab
import com.example.shongket.ui.shell.HomeTab
import com.example.shongket.ui.shell.VolunteerTab
import com.example.shongket.ui.shell.HomeUpdate
import com.example.shongket.data.CommunityKinds
import androidx.compose.runtime.remember
import compose.icons.FeatherIcons
import compose.icons.feathericons.Radio
import com.example.shongket.ui.community.kindStyle
import com.example.shongket.ui.chat.ChatScreen
import com.example.shongket.ui.chat.ChatViewModel

private object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TRIAGE = "triage"
    const val FIRSTAID = "firstaid"
    const val GIS = "gis"
    const val SUMMARY = "summary"
    const val MESH = "mesh"
    const val GUIDE = "guide"
    const val SETTINGS = "settings"
    const val EMERGENCY = "emergency"
    const val COMMUNITY = "community"
    const val FAMILY = "family"
    const val TRANSLATE = "translate"
    const val CHAT = "chat"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShongketTheme {
                ShongketNavHost()
            }
        }
    }
}

@Composable
private fun ShongketNavHost() {
    val context = LocalContext.current
    val app = context.applicationContext as ShongketApplication
    val language by app.prefs.language.collectAsState()
    app.engineHolder.respondInBangla = language == "bn"
    // The intro is shown once. Whether the optional AI model was downloaded is irrelevant
    // here — Shongket's rescue tools do not depend on it, so declining must be a real choice
    // and not a prompt that returns on every launch.
    val introSeen by app.prefs.introSeen.collectAsState()
    val start = if (introSeen) Routes.HOME else Routes.ONBOARDING
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Seeds practice data for the drill: sample SOS reports plus family encounters around
    // wherever the user actually is. Both are labelled as practice and are purged the moment
    // a real report or a real encounter arrives.
    val seedDrill: () -> Unit = {
        if (app.sosRepository.entries.value.none { it.source == "drill" }) {
            RegionAssets.loadScenarios(context).forEach {
                app.sosRepository.add(SosEntry(it, Triage.fallbackTriage(it), source = "drill"))
            }
        }
        scope.launch {
            val here = runCatching { app.meshHub.myLocation() }.getOrNull()
            app.familyRepository.seedDrill(here)
        }
        Unit
    }

    fun toStartFromSplash() {
        navController.navigate(start) {
            popUpTo(Routes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun appFactory() =
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application,
        )

    CompositionLocalProvider(LocalBangla provides (language == "bn")) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            BrandSplashScreen()
            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1300)
                toStartFromSplash()
            }
        }
        composable(Routes.ONBOARDING) {
            val vm: OnboardingViewModel = viewModel(factory = appFactory())
            val toHome: () -> Unit = {
                app.prefs.markIntroSeen()
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                    launchSingleTop = true
                }
            }
            OnboardingScreen(viewModel = vm, onFinished = toHome, onSkip = toHome)
        }
        composable(Routes.HOME) {
            // Four resting places plus a permanently reachable SOS. Everything the app can
            // do is on Home or one row inside More; nothing is deeper than two taps.
            var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
            val sosActive by app.sosSession.active.collectAsState()
            val meshMsgs by app.meshHub.sosMessages.collectAsState()
            val boardReports by app.communityRepository.entries.collectAsState()
            val peers by app.meshHub.peers.collectAsState()
            val modelReady = HfDownloadRepository.modelFile(context).exists()

            // The feed is assembled from what actually arrived over the radio. There is no
            // placeholder activity: an empty feed means nothing has happened, and on this
            // screen that is genuinely the good news.
            val now = System.currentTimeMillis()
            val updates = remember(meshMsgs, boardReports) {
                val fromMesh = meshMsgs.takeLast(6).map {
                    HomeUpdate(
                        title = if (it.mine) "SOS sent" else "SOS received",
                        detail = it.text.take(70),
                        minutesAgo = 0,
                        urgent = true,
                        icon = FeatherIcons.Radio,
                    )
                }
                val fromBoard = boardReports.takeLast(6).map { r ->
                    HomeUpdate(
                        title = CommunityKinds.byId(r.kind).en,
                        detail = r.note.ifBlank { if (r.mine) "You reported this" else "From " + r.sender },
                        minutesAgo = ((now - r.ts) / 60000L).coerceAtLeast(0),
                        urgent = CommunityKinds.byId(r.kind).danger,
                        icon = kindStyle(r.kind).icon,
                    )
                }
                (fromMesh + fromBoard).sortedBy { it.minutesAgo }
            }

            ShongketShell(
                current = tab,
                onSelect = { tab = it },
                onSos = { navController.navigate(Routes.MESH) },
                sosActive = sosActive,
            ) {
                when (tab) {
                    Tab.HOME -> HomeTab(
                        modelReady = modelReady,
                        peers = peers,
                        updates = updates,
                        onTranslate = { navController.navigate(Routes.TRANSLATE) },
                        onFirstAid = { navController.navigate(Routes.FIRSTAID) },
                        onRadar = { navController.navigate(Routes.FAMILY) },
                        onAskAi = { navController.navigate(Routes.CHAT) },
                        onBoard = { navController.navigate(Routes.COMMUNITY) },
                        onEmergency = { navController.navigate(Routes.EMERGENCY) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onGuide = { navController.navigate(Routes.GUIDE) },
                    )
                    Tab.MAP -> {
                        val vm: GisViewModel = viewModel(factory = appFactory())
                        GisScreen(viewModel = vm, onBack = null)
                    }
                    Tab.ALERTS -> {
                        val vm: CommunityViewModel = viewModel(factory = appFactory())
                        CommunityScreen(viewModel = vm, onBack = null)
                    }
                    Tab.VOLUNTEER -> {
                        val sharing by app.volunteerSharing.collectAsState()
                        var busy by remember { mutableStateOf(false) }
                        VolunteerTab(
                            sharing = sharing,
                            sharingBusy = busy,
                            onShareLocation = {
                                // Posts "rescue available" with this phone's position onto the
                                // same signed mesh the board already uses, so it reaches a
                                // stranger's Radar without inventing a second transport.
                                busy = true
                                scope.launch {
                                    runCatching {
                                        app.meshHub.sendReport(
                                            "rescue_here",
                                            "Volunteer available here",
                                        )
                                    }
                                    app.volunteerSharing.value = true
                                    busy = false
                                }
                            },
                            onStopSharing = { app.volunteerSharing.value = false },
                            onTriage = { navController.navigate(Routes.TRIAGE) },
                            onSummary = { navController.navigate(Routes.SUMMARY) },
                            onEmergency = { navController.navigate(Routes.EMERGENCY) },
                            onGuide = { navController.navigate(Routes.GUIDE) },
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                        )
                    }
                }
            }
        }
        composable(Routes.TRANSLATE) {
            val vm: TranslateViewModel = viewModel(factory = appFactory())
            TranslateScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenGuide = { navController.navigate(Routes.GUIDE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onSendAsSos = { note ->
                    app.pendingSosDraft = note
                    navController.navigate(Routes.MESH)
                },
            )
        }
        composable(Routes.TRIAGE) {
            val vm: TriageViewModel = viewModel(factory = appFactory())
            TriageScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.FIRSTAID) {
            val vm: FirstAidViewModel = viewModel(factory = appFactory())
            FirstAidScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.GIS) {
            val vm: GisViewModel = viewModel(factory = appFactory())
            GisScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.SUMMARY) {
            val vm: SummaryViewModel = viewModel(factory = appFactory())
            SummaryScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.MESH) {
            val vm: MeshViewModel = viewModel(factory = appFactory())
            // A note composed on the translation screen arrives here prefilled, so the
            // volunteer never has to retype what the person just told them.
            val draft = app.pendingSosDraft
            app.pendingSosDraft = null
            MeshScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                prefill = draft.orEmpty(),
            )
        }
        composable(Routes.GUIDE) {
            GuideScreen(
                onBack = { navController.popBackStack() },
                onSeedDemo = seedDrill,
                actions = Actions(
                    triage = { navController.navigate(Routes.TRIAGE) },
                    firstAid = { navController.navigate(Routes.FIRSTAID) },
                    shelter = { navController.navigate(Routes.GIS) },
                    mesh = { navController.navigate(Routes.MESH) },
                    summary = { navController.navigate(Routes.SUMMARY) },
                    translate = { navController.navigate(Routes.TRANSLATE) },
                ),
            )
        }
        composable(Routes.EMERGENCY) {
            EmergencyScreen(
                onBack = { navController.popBackStack() },
                onMesh = { navController.navigate(Routes.MESH) },
            )
        }
        composable(Routes.COMMUNITY) {
            val vm: CommunityViewModel = viewModel(factory = appFactory())
            CommunityScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.FAMILY) {
            val vm: FamilyViewModel = viewModel(factory = appFactory())
            FamilyScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.CHAT) {
            val vm: ChatViewModel = viewModel(factory = appFactory())
            ChatScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onNeedModel = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.CHAT) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = appFactory())
            SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
    }
}











