tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })

    manifest {
        attributes(
            "Main-Class" to "net.rk4z.s1.swiftbase.launcher.Main",
            "Class-Path" to "."
        )
    }
}
