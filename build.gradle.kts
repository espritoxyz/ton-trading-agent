import java.net.URI

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
