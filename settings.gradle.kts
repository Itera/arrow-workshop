plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories { mavenCentral() }
}

rootProject.name = "arrow-workshop"

include(
    "starter",
    "01-domain",
    "02-validation",
    "03-typed-errors",
    "04-boundaries",
    "05-parallel-quotes",
    "06-live-updates",
    "07-resilience",
    "08-resource-safety",
    "09-extras",
)
