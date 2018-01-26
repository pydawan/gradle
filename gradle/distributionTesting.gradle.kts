/*
 * Copyright 2016 the original author or authors.
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

import org.gradle.cleanup.CleanUpCaches
import org.gradle.cleanup.CleanUpDaemons
import org.gradle.process.KillLeakingJavaProcesses
import org.gradle.testing.DistributionTest

tasks.withType<DistributionTest> {

    val distributionTest = this

    dependsOn(":toolingApi:toolingApiShadedJar")
    dependsOn(":cleanUpCaches")
    finalizedBy(":cleanUpDaemons")
    shouldRunAfter("test")

    jvmArgs("-Xmx512m", "-XX:+HeapDumpOnOutOfMemoryError")
    if (!javaVersion.isJava8Compatible) {
        jvmArgs("-XX:MaxPermSize=768m")
    }

    reports.junitXml.destination = File(the<JavaPluginConvention>().testResultsDir, name)

    // use -PtestVersions=all or -PtestVersions=1.2,1.3…
    val integTestVersionsSysProp = "org.gradle.integtest.versions"
    if (project.hasProperty("testVersions")) {
        systemProperties[integTestVersionsSysProp] = project.property("testVersions")
    }
    if (integTestVersionsSysProp !in systemProperties) {
        systemProperties[integTestVersionsSysProp] = "latest"
    }

    fun ifProperty(name: String, then: String): String? =
        then.takeIf { hasProperty(name) && property(name).toString().isNotEmpty() }

    systemProperties["org.gradle.integtest.cpp.toolChains"] =
        ifProperty("testAllPlatforms", "all") ?: "default"

    systemProperties["org.gradle.integtest.multiversion"] =
        ifProperty("testAllVersions", "all") ?: "default"

    val mirrorUrls = collectMirrorUrls()
    val mirrors = listOf("mavencentral", "jcenter", "lightbendmaven", "ligthbendivy", "google")
    mirrors.forEach { mirror ->
        systemProperties["org.gradle.integtest.mirrors.$mirror"] = mirrorUrls[mirror] ?: ""
    }

    dependsOn(
        task("configure${distributionTest.name.capitalize()}") {
            doLast {
                distributionTest.apply {

                    reports.html.destination = file("${the<ReportingExtension>().baseDir}/$name")

                    val intTestImage: Sync by tasks
                    gradleHomeDir = intTestImage.destinationDir

                    gradleUserHomeDir = rootProject.file("intTestHomeDir")

                    val toolingApiShadedJar: Zip by rootProject.project(":toolingApi").tasks
                    toolingApiShadedJarDir = toolingApiShadedJar.destinationDir

                    if (requiresLibsRepo) {
                        libsRepo = rootProject.file("build/repo")
                    }

                    if (requiresDists) {
                        distsDir = rootProject.the<BasePluginConvention>().distsDir
                        systemProperties["integTest.distZipVersion"] = version
                    }

                    if (requiresBinZip) {
                        val binZip: Zip by project(":distributions").tasks
                        this.binZip = binZip.archivePath
                    }

                    daemonRegistry = file("${rootProject.buildDir}/daemon")
                }
            }
        })

    lateinit var daemonListener: Any

    doFirst {
        val cleanUpDaemons: CleanUpDaemons by rootProject.tasks
        daemonListener = cleanUpDaemons.newDaemonListener()
        gradle.addListener(daemonListener)
    }

    doLast {
        gradle.removeListener(daemonListener)
    }
}

project(":") {

    if (tasks.findByName("cleanUpCaches") != null) {
        return@project
    }

    tasks {

        "cleanUpCaches"(CleanUpCaches::class) {
            dependsOn(":createBuildReceipt")
        }

        "cleanUpDaemons"(CleanUpDaemons::class)

        val killExistingProcessesStartedByGradle by creating(KillLeakingJavaProcesses::class)

        val isCiServer: Boolean by rootProject.extra
        if (isCiServer) {
            "clean" {
                dependsOn(killExistingProcessesStartedByGradle)
            }
            subprojects {
                tasks.all {
                    mustRunAfter(killExistingProcessesStartedByGradle)
                }
            }
        }
    }
}

fun collectMirrorUrls(): Map<String, String> =
    // expected env var format: repo1_id:repo1_url,repo2_id:repo2_url,...
    System.getenv("REPO_MIRROR_URLS")?.split(',')?.associate { nameToUrl ->
            val (name, url) = nameToUrl.split(':', limit = 2)
            name to url
        } ?: emptyMap()
