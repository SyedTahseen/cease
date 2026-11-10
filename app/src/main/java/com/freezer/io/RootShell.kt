package com.freezer.io

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
        run(*packages.map { "pm enable $it" }.toTypedArray())

    fun freezeAndStop(toFreeze: Collection<String>, toStop: Collection<String>): Result =
        run(
            *(toFreeze.map { "pm disable-user --user 0 $it" } + toStop.map { "am force-stop $it" }).toTypedArray(),
        )
}
