import java.net.URI

buildscript {
    repositories {
        maven(url = "https://artifactory.e-spirit.hosting/artifactory/repo") {
            credentials {
                username = extra.properties["artifactory_hosting_username"] as String
                password = extra.properties["artifactory_hosting_password"] as String
            }
        }
    }
}

plugins {
    id("java-library")
    id("maven-publish")
}

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    group = "to.be.renamed"
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    repositories {
        maven(url = "https://artifactory.e-spirit.hosting/artifactory/repo") {
            credentials {
                username = extra.properties["artifactory_hosting_username"] as String
                password = extra.properties["artifactory_hosting_password"] as String
            }
        }
    }
}

tasks.findByName("publish")?.dependsOn("my-workflow-fsm:publish")
