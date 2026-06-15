import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.0"
}
version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()
repositories {
	maven { url = uri("https://maven.terraformersmc.com/releases/") }
	maven { url = uri("https://api.modrinth.com/maven") }
	maven { url = uri("https://mvn.devos.one/snapshots/") }
	maven { url = uri("https://mvn.devos.one/releases/") }
	mavenCentral()
}
loom {
	splitEnvironmentSourceSets()
	mods {
		register("friendsradio") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}
dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	mappings("net.fabricmc:yarn:${providers.gradleProperty("yarn_mappings").get()}:v2")
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")
	modImplementation("com.terraformersmc:modmenu:7.+")
	modCompileOnly("maven.modrinth:create-fabric:0.5.1-j-build.1631+mc1.20.1") {
		isTransitive = false
	}
	modCompileOnly("io.github.fabricators_of_create.Porting-Lib:entity:2.3.0+1.20.1")
	modApi(include("com.tterrag.registrate_fabric:Registrate:1.3.79-MC1.20.1")!!)

	// Audio
	include(implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")!!)
	include(implementation("com.googlecode.soundlibs:jlayer:1.0.1.4")!!)
	include(implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")!!)

//	// Image I/O
//	include(implementation("com.twelvemonkeys.imageio:imageio-ico:3.0.2")!!)
//
//	// SVG
	include(implementation("org.apache.xmlgraphics:batik-transcoder:1.17")!!)
	include(implementation("org.apache.xmlgraphics:batik-codec:1.17")!!)
}
tasks.processResources {
	val version = version
	inputs.property("version", version)
	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}
tasks.withType<JavaCompile>().configureEach {
	options.release = 17
}
kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
	}
}
java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}
tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)
	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}
}