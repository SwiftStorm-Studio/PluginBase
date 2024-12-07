# SwiftBase

SwiftBase is a library that provides a base code of Minecraft Fabric Mod and Paper Plugin.

## Features
- i18n with YAML and Classes
- Update CHacker (With ModrinthAPI)
- Auto Config file generation (With user system language)
- Executor System

## How to use
### Fabric Mod
1. Add the following to your `build.gradle` or `build.gradle.kts`

Groovy DSL:
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'net.rk4z.s1:swiftbase-core:{version}'
    implementation 'net.rk4z.s1:swiftbase-fabric:{version}'
}
```

Kotlin DSL:
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("net.rk4z.s1:swiftbase-core:{version}")
    implementation("net.rk4z.s1:swiftbase-fabric:{version}")
}
```

2. Relocate the package name of the library to your package name and include it in your mod Jar.
   (This is an example of how to include the library in your mod Jar. You can change the package name as you like.)
You can relocate the package name like this

Groovy DSL:
```groovy
configurations {
    includeInJar
}

dependencies {
    minecraft "com.mojang:minecraft:{version}"
    mappings "net.fabricmc:yarn:{version}"
    modImplementation "net.fabricmc:fabric-loader:{version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:{version}"

    compileOnly "net.rk4z.s1:swiftbase-core:{version}"
    compileOnly "net.rk4z.s1:swiftbase-fabric:{version}"

    includeInJar "net.rk4z.s1:swiftbase-core:{version}"
    includeInJar "net.rk4z.s1:swiftbase-fabric:{version}"
}

tasks.withType(Jar) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from {
        configurations.includeInJar.collect {
            it.isDirectory() ? it : zipTree(it)
        }
    }
}

```

Kotlin DSL:
```kotlin
val includeInJar: Configuration by configurations.creating

dependencies {
	minecraft("com.mojang:minecraft:{version}")
	mappings("net.fabricmc:yarn:{version}")
	modImplementation("net.fabricmc:fabric-loader:{version}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:{version}")
	modImplementation("net.fabricmc:fabric-language-kotlin:{version}")

    compileOnly("net.rk4z.s1:swiftbase-core:{version}")
    compileOnly("net.rk4z.s1:swiftbase-fabric:{version}")
    
    includeInJar("net.rk4z.s1:swiftbase-core:{version}")
    includeInJar("net.rk4z.s1:swiftbase-fabric:{version}")
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        (configurations["includeInJar"])
            .filter { it.exists() && !it.name.startsWith("kotlin") }
            .map { if (it.isDirectory) it else project.zipTree(it) }
    })
}
```

### Paper Plugin
1. Add the following to your `build.gradle` or `build.gradle.kts`

Groovy DSL:
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'net.rk4z.s1:swiftbase-core:{version}'
    implementation 'net.rk4z.s1:swiftbase-paper:{version}'
}
```

Kotlin DSL:
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("net.rk4z.s1:swiftbase-core:{version}")
    implementation("net.rk4z.s1:swiftbase-paper:{version}")
}
```

2. Relocate the package name of the library to your package name and include it in your plugin Jar.
   (This is an example of how to include the library in your plugin Jar. You can change the package name as you like.)

Groovy DSL:
```groovy
tasks.named("jar", Jar) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.output)

    from {
        configurations.runtimeClasspath.collect { zipTree(it) }
    }
}

```

Kotlin DSL:
```kotlin
tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    from({
        configurations.runtimeClasspath.get()
            .filter { !it.name.contains("kotlin", ignoreCase = true) }
            .map { zipTree(it) }
    })
}
```