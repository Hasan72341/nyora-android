package com.nyora.buildlogic

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Applies the domain-string obfuscation ASM instrumentation to the shippable build types.
 * (`debug` is included for verification; drop it from [OBFUSCATED_BUILD_TYPES] once validated.)
 */
class DomainObfuscationPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val androidComponents =
            target.extensions.getByType(AndroidComponentsExtension::class.java)
        val domainsFile = File(target.projectDir, "domains.txt").absolutePath

        androidComponents.onVariants { variant ->
            if (variant.buildType in OBFUSCATED_BUILD_TYPES) {
                variant.instrumentation.transformClassesWith(
                    DomainObfuscatorFactory::class.java,
                    InstrumentationScope.ALL,
                ) { params ->
                    params.domainsFilePath.set(domainsFile)
                }
                variant.instrumentation.setAsmFramesComputationMode(
                    FramesComputationMode.COPY_FRAMES,
                )
            }
        }
    }

    private companion object {
        // Ship builds only — debug stays plain for fast, inspectable dev builds. Verified working
        // on a debug build (1029/1030 domains removed from the DEX; DomainVault decrypts at runtime).
        val OBFUSCATED_BUILD_TYPES = setOf("release", "nightly")
    }
}
