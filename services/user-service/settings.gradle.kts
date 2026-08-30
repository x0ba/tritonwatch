pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "user-service"
includeBuild("../../shared/event-contracts")
