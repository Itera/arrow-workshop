// Every stage is its own subproject. Plugins are declared once here so all stages share one classpath.
plugins {
    base
    alias(libs.plugins.arrow.optics) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlinter) apply false
}

// The root has no code of its own. Its `clean`, `check`, and `test` cover every stage, so
// `:check` and the root node in the IDE's Gradle tool window behave like `./gradlew check`.
tasks.clean {
    dependsOn(subprojects.map { "${it.path}:clean" })
}

tasks.check {
    dependsOn(subprojects.map { "${it.path}:check" })
}

tasks.register("test") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the tests of every stage."
    dependsOn(subprojects.map { "${it.path}:test" })
}
