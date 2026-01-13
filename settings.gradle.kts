pluginManagement {
    repositories {
        // 阿里云 Google 镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // 阿里云中央仓库镜像
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 阿里云公共仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 阿里云 Gradle 插件镜像
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 官方源作为备用
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云 Google 镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云中央仓库镜像
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 阿里云公共仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 腾讯云镜像 (备用)
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 官方源作为备用
        google()
        mavenCentral()
    }
}

rootProject.name = "PhotoZen"
include(":app")
