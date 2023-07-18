plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.freefair.lombok") version "8.1.0"
    id("net.kyori.blossom") version "1.3.1"
}

version = "1.0.3"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
    maven("https://m2.chew.pro/releases")
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-alpha.12")
    implementation("com.github.Kaktushose:jda-commands:v3.0.0")
    implementation("io.javalin:javalin:4.6.3")
    implementation("net.jodah:typetools:0.6.3")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("org.slf4j:slf4j-simple:1.7.31")
}

blossom {
    replaceToken("%%VERSION%%", version)
}

tasks {
    shadowJar {
        isEnableRelocation = true
        relocationPrefix = "com.jellylab.farmhelperbot.relocate"
    }
    jar {
        manifest {
            attributes["Main-Class"] = "com.yyonezu.remotecontrol.Main"
        }
    }
    application {
        mainClass = "com.yyonezu.remotecontrol.Main"
    }
    // would have put this into the root build script but it complained that it couldn't find the task
    build {
        doLast {
            copy {
                from("${project.rootProject.rootDir}/${project.name}/build/libs/${project.name}-${project.version}-all.jar")
                into("${project.rootProject.rootDir}/build")
                rename("${project.name}-${project.version}-all.jar", "${project.name}-${project.version}.jar")
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

java.toolchain.languageVersion = JavaLanguageVersion.of(8)