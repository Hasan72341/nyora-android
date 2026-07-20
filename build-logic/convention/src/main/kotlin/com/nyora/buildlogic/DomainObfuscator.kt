package com.nyora.buildlogic

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File

/**
 * Replaces every string constant that EXACTLY matches a known source domain (from `domains.txt`,
 * one per line = index) with a call to `DomainVault.d(index)`, so the baked domains are not
 * extractable from the APK by `strings`/apktool. Exact-match only ⇒ no heuristic overrun.
 */
abstract class DomainObfuscatorFactory :
    AsmClassVisitorFactory<DomainObfuscatorFactory.Params> {

    interface Params : InstrumentationParameters {
        @get:Input
        val domainsFilePath: Property<String>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor,
    ): ClassVisitor = DomainClassVisitor(
        instrumentationContext.apiVersion.get(),
        nextClassVisitor,
        indexOf(parameters.get().domainsFilePath.get()),
    )

    override fun isInstrumentable(classData: ClassData): Boolean = true

    private companion object {
        private val cache = HashMap<String, Map<String, Int>>()

        @Synchronized
        fun indexOf(path: String): Map<String, Int> = cache.getOrPut(path) {
            File(path).readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .withIndex()
                .associate { (i, d) -> d to i }
        }
    }
}

private class DomainClassVisitor(
    apiVersion: Int,
    next: ClassVisitor,
    private val domainIndex: Map<String, Int>,
) : ClassVisitor(apiVersion, next) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return object : MethodVisitor(api, mv) {
            override fun visitLdcInsn(value: Any?) {
                val idx = (value as? String)?.let { domainIndex[it] }
                if (idx != null) {
                    when {
                        idx <= Byte.MAX_VALUE -> super.visitIntInsn(Opcodes.BIPUSH, idx)
                        idx <= Short.MAX_VALUE -> super.visitIntInsn(Opcodes.SIPUSH, idx)
                        else -> super.visitLdcInsn(idx)
                    }
                    super.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/nyora/hasan72341/core/vault/DomainVault",
                        "d",
                        "(I)Ljava/lang/String;",
                        false,
                    )
                } else {
                    super.visitLdcInsn(value)
                }
            }
        }
    }
}
