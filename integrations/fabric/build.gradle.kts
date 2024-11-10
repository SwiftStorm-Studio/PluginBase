import cl.franciscosolis.sonatypecentralupload.SonatypeCentralUploadTask
import java.io.FileInputStream
import java.util.*

plugins {
    id ("fabric-loom") apply true
}

version = "1.1.1"

dependencies {
    minecraft("com.mojang:minecraft:1.21.3")
    mappings("net.fabricmc:yarn:1.21.3+build.2")
    modImplementation("net.fabricmc:fabric-loader:0.16.9")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.107.0+1.21.3")
    compileOnly(project(":integrations:swiftbase-core"))
}

val localProperties = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}

val fabricJar by tasks.registering(Jar::class) {
    archiveClassifier.set("fabric")
    from(sourceSets.main.get().output)
}

publishing {
    publications {
        // Fabric用のパッケージ
        create<MavenPublication>("fabric") {
            groupId = rootProject.group.toString()
            artifactId = "${rootProject.name}-fabric"
            version = rootProject.version.toString()

            // プラットフォームごとのJarをアタッチ
            artifact(tasks.named("fabricJar")) {
                classifier = "fabric"
            }

            // POM情報
            pom {
                name.set("SwiftBase Fabric")
                description.set("Base code by SwiftStormStudio for FabricMod.")
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
    dependsOn("clean", "jar", "sourcesJar", "javadocJar", "generatePomFileForFabricPublication")

    username = localProperties.getProperty("cu")
    password = localProperties.getProperty("cp")

    archives = files(
        tasks.named("jar"),
        tasks.named("sourcesJar"),
        tasks.named("javadocJar"),
    )

    pom = file(
        tasks.named("generatePomFileForFabricPublication").get().outputs.files.single()
    )

    signingKey = localProperties.getProperty("signing.key")
    signingKeyPassphrase = localProperties.getProperty("signing.passphrase")
}