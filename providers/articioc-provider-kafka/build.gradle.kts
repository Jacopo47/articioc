plugins {
	id("java")

	id("jacoco")
}

group = "org.articioc"
version = rootProject.version

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":articioc-base"))

	implementation(libs.vavr)
	implementation(libs.kafka.client)
	implementation(libs.bundles.jackson)


	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.bundles.junit)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")


	testImplementation(testFixtures(project(":")))
	testImplementation(libs.testcontainers.kafka)
}

tasks.test {
	useJUnitPlatform()
}
