plugins { java }

group = "kr.murim"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.lumine:Mythic-Dist:5.6.1")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}
tasks.processResources { filteringCharset = "UTF-8" }
