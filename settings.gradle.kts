pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AndroidGames"

include(":app")
include(":game-api")
include(":games:gomoku")
include(":games:othello")
include(":games:xiangqi")
include(":games:chess")
