plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.detekt)
}

kotlin { jvmToolchain(25) }

dependencies {
    implementation(platform(libs.arrow.stack))
    implementation(libs.bundles.arrow)

    testImplementation(libs.bundles.test)
}

tasks.test { useJUnitPlatform() }

tasks.check { dependsOn(tasks.detektMain, tasks.detektTest) }
