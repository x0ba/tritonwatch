pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "notification-service"
includeBuild("../../shared/event-contracts")
