import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
	kotlin("jvm") version "2.1.0"
	`maven-publish`
}

group = "io.github.team-sneakymouse"

version =
	providers.exec {
		workingDir(rootDir)
		commandLine("git", "show", "-s", "--format=%ct:%h", "--abbrev=12", "HEAD")
	}.standardOutput.asText.map { commit ->
		val (timestamp, hash) = commit.trim().split(":", limit = 2)
		val date =
			DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC)
				.format(Instant.ofEpochSecond(timestamp.toLong()))
		"$date-$hash"
	}.get()

repositories {
	maven {
		url = uri("https://plugins.gradle.org/m2/")
	}
	maven {
		url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
	}
	maven {
		url = uri("https://repo.mikeprimm.com/")
	}
	mavenCentral()
	maven("https://repo.papermc.io/repository/maven-public/")
}

kotlin {
	jvmToolchain(21)
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
	compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
	// compileOnly("me.clip:placeholderapi:2.11.5")
	// compileOnly("us.dynmap:dynmap-api:3.4-beta-3")
	// compileOnly("us.dynmap:DynmapCoreAPI:3.4")
	// implementation("com.squareup.okhttp3:okhttp:4.12.0")
	// implementation("org.json:json:20240303")
}

java {
	withSourcesJar()
}

tasks.processResources {
	inputs.property("version", project.version.toString())
	filesMatching("paper-plugin.yml") {
		expand("version" to project.version.toString())
	}
}

tasks.jar {
	archiveBaseName.set(rootProject.name)
	manifest {
		attributes["Main-Class"] = "com.danidipp.sneakyentitytweaks.SneakyEntityTweaks"
	}

	from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

configure<JavaPluginExtension> {
	sourceSets {
		main {
			java.srcDir("src/main/kotlin")
			resources.srcDir(file("src/resources"))
		}
	}
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			artifactId = rootProject.name
			from(components["java"])
			pom {
				name.set("SneakyEntityTweaks")
				description.set("Adjustments for entity default behaviors in a roleplay environment.")
				url.set("https://github.com/Team-Sneakymouse/SneakyEntityTweaks")
				properties.put("sneakyrp.deployment.type", "paper-plugin")
				scm {
					url.set("https://github.com/Team-Sneakymouse/SneakyEntityTweaks")
					connection.set("scm:git:https://github.com/Team-Sneakymouse/SneakyEntityTweaks.git")
				}
			}
		}
	}
	repositories {
		maven {
			name = "sneakyrp"
			url = uri("https://maven.sneakyrp.com/releases")
			credentials(PasswordCredentials::class)
			authentication {
				create<org.gradle.authentication.http.BasicAuthentication>("basic")
			}
		}
	}
}

tasks.withType<PublishToMavenRepository>().configureEach {
	dependsOn(tasks.check)
}
