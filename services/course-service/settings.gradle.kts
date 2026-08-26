pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "course-service"
includeBuild("../../shared/event-contracts")
