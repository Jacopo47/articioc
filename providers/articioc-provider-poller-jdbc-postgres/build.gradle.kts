plugins {
	id("java")
}

group = "org.articioc"
version = "1.0.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":articioc-base"))
	implementation(project(":providers:articioc-provider-poller"))
	implementation(project(":providers:articioc-provider-poller-jdbc"))

	implementation(libs.vavr)
	implementation(libs.bundles.logback)
	implementation(libs.bundles.jdbi)
	implementation(libs.bundles.jackson)

	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.bundles.junit)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation(libs.mockito)

	testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")
	testImplementation("org.postgresql:postgresql:42.7.9")

	testImplementation(testFixtures(project(":")))
	testImplementation(testFixtures(project(":providers:articioc-provider-poller-jdbc")))


}

tasks.test {
	useJUnitPlatform()
}
