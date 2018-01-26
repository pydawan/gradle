/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// To make it easier to access these functions from Groovy
@file:JvmName("Process")

package org.gradle.process

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.testing.LeakingProcessKillPattern
import java.io.ByteArrayOutputStream


fun Project.pkill(pid: String) {
    val killOutput = ByteArrayOutputStream()
    val result = exec {
        commandLine =
            if (isWindows) {
                listOf("taskkill.exe", "/F", "/T", "/PID", pid)
            } else {
                listOf("kill", pid)
            }
        standardOutput = killOutput
        errorOutput = killOutput
        isIgnoreExitValue = true
    }
    if (result.exitValue != 0) {
        val out = killOutput.toString()
        if (!out.contains("No such process")) {
            logger.warn(
                """Failed to kill daemon process $pid. Maybe already killed?
Output: $killOutput
""")
        }
    }
}


data class ProcessInfo(val pid: String, val process: String)


fun Project.forEachLeakingJavaProcess(action: Action<ProcessInfo>) {
    val output = ByteArrayOutputStream()
    val error = ByteArrayOutputStream()
    val (result, pidPattern) =
        if (isWindows) {
            exec {
                commandLine("wmic", "process", "get", "processid,commandline")
                standardOutput = output
                errorOutput = error
                isIgnoreExitValue = true
            } to "([0-9]+)\\s*$".toRegex()
        } else {
            exec {
                commandLine("ps", "x")
                standardOutput = output
                errorOutput = output
                isIgnoreExitValue = true
            } to "([0-9]+)".toRegex()
        }

    if (result.exitValue != 0) {
        val errorLog = file("${rootProject.buildDir}/errorLogs/process-list-${System.currentTimeMillis()}.log")
        mkdir(errorLog.parent)
        errorLog.writeText("[Output]\n$output\n[Error Output]\n$error")
        logger.quiet("Error obtaining process list, output log created at $errorLog")
        result.assertNormalExitValue()
    }

    val processPattern = generateLeakingProcessKillPattern()
    forEachLineIn(output.toString()) { line ->
        val processMatcher = processPattern.find(line)
        if (processMatcher != null) {
            val pidMatcher = pidPattern.find(line)
            if (pidMatcher != null) {
                val pid = pidMatcher.groupValues[1]
                val process = processMatcher.groupValues[1]
                if (!isMe(process)) {
                    action.execute(ProcessInfo(pid, process))
                }
            }
        }
    }
}


fun Project.generateLeakingProcessKillPattern() =
    LeakingProcessKillPattern.generate(rootProject.projectDir.absolutePath).toRegex()


inline
fun forEachLineIn(s: String, action: (String) -> Unit) =
    s.lineSequence().forEach(action)


fun Project.isMe(process: String) =
    process.contains(gradle.gradleHomeDir!!.path)


val isWindows get() = OperatingSystem.current().isWindows

