plugins {
	id("java")
	id("java-library")
	id("maven-publish")

	id("jacoco")
}

group = "org.articioc"
version = rootProject.version

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.vavr)
	implementation(libs.bundles.jackson)

	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.bundles.junit)
	testImplementation(libs.vavr)
}

tasks.test {
	useJUnitPlatform()
}

publishing {
	publications {
		create<MavenPublication>(rootProject.name) {
			from(components["java"])
		}
	}

	repositories {
		mavenLocal()
	}
}
