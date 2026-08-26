pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "ingestion-service"
includeBuild("../../shared/event-contracts")
