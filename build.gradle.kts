plugins {
	id("java")
	id("java-library")
	id("java-test-fixtures")
	id("maven-publish")

	id("jacoco-report-aggregation")

	id("com.diffplug.spotless") version "8.1.0"
}

group = "org.articioc"
version = "1.0.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":articioc-base"))

	implementation(libs.vavr)
	implementation(libs.bundles.logback)

	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.bundles.junit)
	testImplementation(libs.awaitility)


	testFixturesImplementation(project(":articioc-base"))
	testFixturesImplementation(libs.vavr)
	testFixturesImplementation(platform(libs.junit.bom))
	testFixturesImplementation(libs.bundles.junit)



	jacocoAggregation(project(":articioc-base"))
	jacocoAggregation(project(":providers:articioc-provider-kafka"))

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

tasks.jacocoTestCoverageVerification {
	violationRules {
		rule {
			executionData.setFrom(
				fileTree(project.rootDir.absolutePath) {
					include("**/build/jacoco/*.exec")
				}
			)
			element = "BUNDLE"
			limit {
				counter = "INSTRUCTION"
				minimum = "0.80".toBigDecimal()
			}
		}
	}
	dependsOn(tasks.testCodeCoverageReport)
	dependsOn(tasks.test)
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification)
}

apply(plugin = "com.diffplug.spotless")
allprojects {
	repositories {
		mavenCentral()
	}

	pluginManager.withPlugin("com.diffplug.spotless") {
		configure<com.diffplug.gradle.spotless.SpotlessExtension> {
			format("misc") {
				// define the files to apply `misc` to
				target("*.gradle.kts", ".gitattributes", ".gitignore")

				// define the steps to apply to those files
				trimTrailingWhitespace()
				leadingSpacesToTabs() // or leadingTabsToSpaces. Takes an integer argument if you don't like 4
				endWithNewline()
			}
			java {
				target("src/*/java/**/*.java")
				removeUnusedImports()
				trimTrailingWhitespace()
				endWithNewline()
			}
		}
	}
}


subprojects {
	apply(plugin = "com.diffplug.spotless")
}
