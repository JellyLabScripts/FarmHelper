plugins {
    id("cc.polyfrost.loom") version "0.10.0.5"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.freefair.lombok") version "8.1.0"
    id("net.kyori.blossom") version "1.3.1"
}

version = "4.5.23-pre"

repositories {
    maven("https://jitpack.io")
    maven("https://repo.polyfrost.cc/releases")
    maven("https://repo.spongepowered.org/repository/maven-public")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

val embed: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(embed)

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    compileOnly("cc.polyfrost:oneconfig-1.8.9-forge:0.2.0-alpha+")
    embed("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+")

    compileOnly("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT:processor")

    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.2")

    embed("com.github.ronmamo:reflections:0.10.2")
    embed("org.java-websocket:Java-WebSocket:1.5.3")
    embed("com.dorkbox:Notify:3.7")

    // required for intellij debug mode to work for some reason
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
}

blossom {
    replaceToken("%%VERSION%%", version)
}

loom {
    runConfigs {
        named("client") {
            ideConfigGenerated(true)
        }
    }

    launchConfigs {
        getByName("client") {
            arg("--tweakClass", "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")
            /**
             * uncomment the following if you want to authenticate into you minecraft account
             * while in the dev env. https://github.com/DJtheRedstoner/DevAuth read this
             */
            //property("devauth.enabled", "true")
        }
    }

    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.farmhelper.json")
    }
}

tasks {
    jar {
        manifest.attributes(
                mapOf(
                        "ModSide" to "CLIENT",
                        "TweakOrder" to "0",
                        "ForceLoadAsMod" to true,
                        "TweakClass" to "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker",
                        "MixinConfigs" to "mixins.farmhelper.json"
                )
        )
        dependsOn(shadowJar)
    }

    remapJar {
        input.set(shadowJar.get().archiveFile)
        archiveClassifier.set("remap")
    }

    shadowJar {
        configurations = listOf(embed)
    }

    processResources {
        inputs.property("version", version)
        filesMatching(listOf("mcmod.info")) {
            expand(mapOf("version" to version))
        }
    }

    build {
        doLast {
            project.file("build/libs/${project.name}-${project.version}.jar").delete()
            project.file("build/libs/${project.name}-${project.version}-all.jar").delete()
            project.file("build/libs/${project.name}-${project.version}-remap.jar").renameTo(file("build/libs/${project.name}-${project.version}.jar"))
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

java.toolchain.languageVersion = JavaLanguageVersion.of(8)