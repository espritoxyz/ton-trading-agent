import java.net.URI
import org.gradle.api.artifacts.ExternalModuleDependency

allprojects {
    repositories {
        mavenCentral()
        maven {
            url = URI("https://maven.pkg.github.com/explyt/ai-client")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_PAT")
            }
        }
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            val dep = add("implementation", "com.explyt.ai.router:ai-router:v1.45.0")
            (dep as? ExternalModuleDependency)?.exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-stdlib"))
        }
    }
}
