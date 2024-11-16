import cl.franciscosolis.sonatypecentralupload.SonatypeCentralUploadTask
import java.io.FileInputStream
import java.util.*

version = "2.0.0"

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")
    compileOnly(project(":integrations:swiftbase-core"))
}

val localProperties = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}

publishing {
    publications {
        // Paper用のパッケージ
        create<MavenPublication>("paper") {
            groupId = project.group.toString()
            artifactId = "${rootProject.name}-paper"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("SwiftBase Paper")
                description.set("The Paper base of SwiftBase plugin")
                url.set("https://github.com/SwiftStorm-Studio/SwiftBase")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/license/mit")
                    }
                }
                developers {
                    developer {
                        id.set("lars")
                        name.set("Lars")
                        email.set("main@rk4z.net")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/SwiftStorm-Studio/SwiftBase.git")
                    developerConnection.set("scm:git:ssh://github.com/SwiftStorm-Studio/SwiftBase.git")
                    url.set("https://github.com/SwiftStorm-Studio/SwiftBase")
                }
            }
        }
    }
}

tasks.named<SonatypeCentralUploadTask>("sonatypeCentralUpload") {
    dependsOn("clean", "jar", "sourcesJar", "javadocJar", "generatePomFileForPaperPublication")

    username = localProperties.getProperty("cu")
    password = localProperties.getProperty("cp")

    archives = files(
        tasks.named("jar"),
        tasks.named("sourcesJar"),
        tasks.named("javadocJar"),
    )

    pom = file(
        tasks.named("generatePomFileForPaperPublication").get().outputs.files.single()
    )

    signingKey = localProperties.getProperty("signing.key")
    signingKeyPassphrase = localProperties.getProperty("signing.passphrase")
}