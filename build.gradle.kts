plugins {
    application
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java {
            srcDir("src/java")
        }
        resources {
            srcDir("src/resources")
        }
    }
    test {
        java {
            srcDir("test/java")
        }
        resources {
            srcDir("test/resources")
        }
    }
}

dependencies {
	implementation("org.eclipse.collections:eclipse-collections-api:11.1.0")
	implementation("org.eclipse.collections:eclipse-collections:11.1.0")
	implementation("commons-cli:commons-cli:1.9.0")
    implementation("net.imagej:imagej:30.0.0")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.11.1")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "tucil2_13523045_13523052.Main"
}
tasks.jar {
    manifest {
        attributes["Main-Class"] = "tucil2_13523045_13523052.Main"
    }
	from(configurations.runtimeClasspath.get().map { zipTree(it) }) {
		exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
	}
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
