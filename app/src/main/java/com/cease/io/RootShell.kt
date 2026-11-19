package com.cease.io

private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)*")

fun isSafePackageName(name: String): Boolean = PACKAGE_NAME.matches(name)

object RootShell {
    data class Result(val success: Boolean, val output: String)

    fun isRootAvailable(): Boolean = run("id").let { it.success && it.output.contains("uid=0") }

    fun run(vararg commands: String): Result = try {
        val process = ProcessBuilder("su").redirectErrorStream(true).start()
        process.outputStream.bufferedWriter().use { writer ->
            commands.forEach { writer.write(it); writer.newLine() }
            writer.write("exit")
            writer.newLine()
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        Result(process.waitFor() == 0, output.trim())
    } catch (e: Exception) {
        Result(false, e.message ?: "su not available")
    }

    fun freeze(packages: Collection<String>): Result = freezeAndStop(packages, emptyList())

    fun forceStop(packages: Collection<String>): Result = freezeAndStop(emptyList(), packages)

    fun unfreeze(packages: Collection<String>): Result =
        run(*packages.filter(::isSafePackageName).map { "pm enable $it" }.toTypedArray())

    fun freezeAndStop(toFreeze: Collection<String>, toStop: Collection<String>): Result =
        run(
            *(
                toFreeze.filter(::isSafePackageName).map { "pm disable-user --user 0 $it" } +
                    toStop.filter(::isSafePackageName).map { "am force-stop $it" }
                ).toTypedArray(),
        )
}
