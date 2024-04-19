import org.apache.tools.ant.DirectoryScanner

pluginManagement {
    repositories {
        maven {
            url = java.net.URI("https://artifactory.e-spirit.hosting/artifactory/repo")
            credentials {
                username = extra.properties["artifactory_hosting_username"] as String
                password = extra.properties["artifactory_hosting_password"] as String
            }
        }
        gradlePluginPortal()
    }
}

rootProject.name = "my-workflow-fsm-parent"
include(":my-workflow-fsm")

project(":my-workflow-fsm").projectDir = File(rootDir, "fsm")
project(":my-workflow-fsm").name = "my-workflow-fsm"

DirectoryScanner.removeDefaultExclude("**/.gitignore")