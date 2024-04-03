plugins {
    java
    application
}

group = "org.tkann"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons:commons-math3:3.3")
}

application {
    mainClass = "Main"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "Main"
    }
    archiveFileName.set("cocot.jar")
    // incorporate the dependencies into the jar
    from({ configurations.runtimeClasspath.get().filter { it.exists() } }) {
        into("lib")
    }
}
