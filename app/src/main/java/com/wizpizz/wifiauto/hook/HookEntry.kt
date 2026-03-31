package com.wizpizz.wifiauto.hook

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {

    override fun onHook() = encase {
        configs {
            debugLog {
                tag = "WifiAutoDisable"
            }
        }

        loadApp(name = "com.android.systemui") {
            WifiAutoDisableHook.apply(this)
        }
    }
}
