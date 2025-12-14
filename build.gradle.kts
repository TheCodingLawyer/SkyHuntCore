plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.skyraid"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    
    // WorldEdit & FastAsyncWorldEdit (optional, for schematic loading)
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.17")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.9.2")
    
    // Database - SQLite for default, MySQL optional
    implementation("org.xerial:sqlite-jdbc:3.44.0.0")
    
    // JSON Configuration
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Utilities
    implementation("org.jetbrains:annotations:24.1.0")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    // Don't filter YAML files with version replacement - just copy them as-is
    // Binary files will be copied without modification
    filesMatching(listOf("**/*.yml", "**/*.yaml")) {
        filter { line ->
            line.replace("\${project.version}", project.version.toString())
        }
    }
    
    // Binary files are copied as-is
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Created-By" to "SkyHunt Team"
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${project.version}.jar")
    
    // Exclude unnecessary files
    exclude("META-INF/maven/**")
    exclude("META-INF/versions/**")
    
    // Exclude all markdown documentation files
    exclude("**/*.md")
    exclude("**/*.MD")
}