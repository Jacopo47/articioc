plugins {
	id("java")
}

group = "org.articioc"
version = rootProject.version

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":"))
	implementation(project(":articioc-base"))

	implementation("io.vavr:vavr:0.11.0")
	implementation("com.konghq:unirest-java:3.14.5")

	testImplementation(platform("org.junit:junit-bom:5.10.0"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
	useJUnitPlatform()
}


tasks.withType<PublishToMavenRepository>().configureEach {
    enabled = false
}

tasks.withType<PublishToMavenLocal>().configureEach {
    enabled = false
}