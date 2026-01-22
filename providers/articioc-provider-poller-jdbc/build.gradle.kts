plugins {
	id("java")
	id("java-test-fixtures")
}

group = "org.articioc"
version = rootProject.version

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":articioc-base"))
	implementation(project(":providers:articioc-provider-poller"))

	implementation(libs.vavr)
	implementation(libs.bundles.logback)
	implementation(libs.bundles.jdbi)
	implementation(libs.bundles.jackson)


	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.bundles.junit)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation(testFixtures(project(":")))
	testImplementation(libs.mockito)

	testFixturesImplementation(testFixtures(project(":")))
	testFixturesImplementation(project(":articioc-base"))
	testFixturesImplementation(project(":providers:articioc-provider-poller"))

	testFixturesImplementation(platform(libs.junit.bom))
	testFixturesImplementation(libs.bundles.junit)
	testFixturesImplementation(libs.vavr)
	testFixturesImplementation(libs.bundles.jdbi)

}

tasks.test {
	useJUnitPlatform()
}
