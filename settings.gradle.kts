pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/pioneo/lib-android-pioneo-chatui-compose")
            credentials {
                username = extra["GITHUB_USERNAME"] as String?
                password = extra["GITHUB_TOKEN"] as String?
            }
        }
    }
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Eatelligent"
include(":app")
 