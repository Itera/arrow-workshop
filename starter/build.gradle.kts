// Your workspace. Deliberately no lint plugins: run `./gradlew :starter:test` while you work.
plugins {
    alias(libs.plugins.kotlin)
}

kotlin { jvmToolchain(25) }

dependencies {
    implementation(platform(libs.arrow.stack))
    implementation(libs.bundles.arrow)

    testImplementation(libs.bundles.test)
}

tasks.test { useJUnitPlatform() }
