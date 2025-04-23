package ru.unicorecms.unicoreprovider

import pro.gravit.launcher.base.modules.LauncherInitContext
import pro.gravit.launcher.base.modules.LauncherModule
import pro.gravit.launcher.base.modules.LauncherModuleInfo
import pro.gravit.launcher.base.modules.events.PreConfigPhase
import pro.gravit.launchserver.auth.core.AuthCoreProvider
import pro.gravit.utils.Version
import ru.unicorecms.unicoreprovider.core.UnicoreAuthProvider

class UnicoreProviderModule : LauncherModule(LauncherModuleInfo("UnicoreProvider", version, arrayOf("LaunchServerCore"))) {
    companion object {
        val version = Version(1, 0, 1, 0, Version.Type.STABLE)
        private var registered = false
    }

    fun preInit(preConfigPhase: PreConfigPhase?) {
        if (!registered) {
            AuthCoreProvider.providers.register("unicore", UnicoreAuthProvider::class.java)
            registered = true
        }
    }

    override fun init(initContext: LauncherInitContext?) {
        registerEvent(
            { preConfigPhase: PreConfigPhase? -> this.preInit(preConfigPhase) },
            PreConfigPhase::class.java
        )
    }
}

