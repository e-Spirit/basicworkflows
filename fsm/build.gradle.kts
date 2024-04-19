import java.net.URI

val fsRuntimeVersion: String by project

val fsModuleName: String by project
val fsDisplayName: String by project
val fsDescription: String by project
val fsVendor: String by project

val releaseRepository: String by project
val snapshotRepository: String by project

buildscript {
    repositories {
        maven(url = "https://artifactory.e-spirit.hosting/artifactory/repo") {
            credentials {
                username = extra.properties["artifactory_hosting_username"] as String
                password = extra.properties["artifactory_hosting_password"] as String
            }
        }
    }
    dependencies {
        classpath("jakarta.activation:jakarta.activation-api:2.1.3")
    }
}

plugins {
    id("de.espirit.firstspirit") version "2.0.0"
    id("de.espirit.firstspirit-module") version "6.1.0"
}

dependencies {
    compileOnly("de.espirit.firstspirit:fs-isolated-runtime:$fsRuntimeVersion")
}

firstSpiritModule {
    moduleName = fsModuleName
    displayName = fsDisplayName
    vendor = fsVendor
    description = fsDescription
}

publishing {
    repositories {
        maven {
            credentials {
                username = extra.properties["artifactory_hosting_username"] as String
                password = extra.properties["artifactory_hosting_password"] as String
            }
            val publishingVersion = if ((version as String).endsWith("SNAPSHOT")) snapshotRepository else releaseRepository
            url = URI("https://artifactory.e-spirit.hosting/artifactory/$publishingVersion")
        }
    }
    publications {
        create<MavenPublication>("fsm") {
        groupId = "to.be.renamed"
        artifactId = "my-workflow-fsm"
        artifact(file("build/fsm/my-workflow-fsm-${version}.fsm"))
        }
    }
}

tasks.findByName("publish")?.dependsOn("build")
