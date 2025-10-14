import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun property(key: String) = providers.gradleProperty(key).get()
fun env(key: String) = providers.environmentVariable(key).get()

group = property("pluginGroup")
version = property("pluginVersion")

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.10.0"
    id("org.jetbrains.changelog") version "2.4.0"
    id("com.github.ben-manes.versions") version "0.53.0"
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        )

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }

    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.yaml:snakeyaml:2.5")

    testImplementation(kotlin("test"))

    testRuntimeOnly("junit:junit:4.13.2") // see: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
}

kotlin {
    jvmToolchain(property("kotlinJvmTarget").toInt())
}

intellijPlatform {
    pluginConfiguration {
        name = property("pluginName")
        version = property("pluginVersion")
        description = property("pluginDescription")
        changeNotes = provider {
            changelog.renderItem(
                changelog.getLatest(),
                Changelog.OutputType.HTML
            )
        }

        ideaVersion {
            sinceBuild = property("pluginSinceBuild")

            if (providers.gradleProperty("pluginUntilBuild").isPresent) {
                untilBuild = property("pluginUntilBuild")
            }
        }
    }

    signing {
        if (listOf(
                "JB_PLUGIN_SIGN_CERTIFICATE_CHAIN",
                "JB_PLUGIN_SIGN_PRIVATE_KEY",
                "JB_PLUGIN_SIGN_PRIVATE_KEY_PASSWORD").all { System.getenv(it) != null }) {
            certificateChainFile = file(env("JB_PLUGIN_SIGN_CERTIFICATE_CHAIN"))
            privateKeyFile = file(env("JB_PLUGIN_SIGN_PRIVATE_KEY"))
            password = file(env("JB_PLUGIN_SIGN_PRIVATE_KEY_PASSWORD")).readText()
        }
    }

    publishing {
        if (System.getenv("JB_PLUGIN_PUBLISH_TOKEN") != null) {
            token = file(env("JB_PLUGIN_PUBLISH_TOKEN")).readText().trim()
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

changelog {
    version = property("pluginVersion")
}

tasks {
    dependencyUpdates {
        rejectVersionIf {
            (
                listOf("RELEASE", "FINAL", "GA").any { candidate.version.uppercase().contains(it) }
                ||
                "^[0-9,.v-]+(-r)?$".toRegex().matches(candidate.version)
            ).not()
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
