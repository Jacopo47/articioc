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

	testImplementation("org.testcontainers:mariadb:1.21.4")
	implementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")


	testImplementation(testFixtures(project(":")))
	testImplementation(testFixtures(project(":providers:articioc-provider-poller-jdbc")))
}

tasks.test {
	useJUnitPlatform()
}
