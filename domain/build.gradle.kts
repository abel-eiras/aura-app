plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

// Los contratos de datos viven en specs/*/contracts (fuente única). Se copian
// como recursos para que el código y los tests lean exactamente el mismo fichero.
val syncContracts by tasks.registering(Copy::class) {
    from(rootProject.file("specs/005-tipos-de-nota/contracts/categorias.default.json"))
    from(rootProject.file("specs/005-tipos-de-nota/contracts/categorias.schema.json"))
    from(rootProject.file("specs/003-procesado-ia/contracts/transcripcion.schema.json"))
    into(layout.buildDirectory.dir("generated/contracts/contracts"))
}
sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated/contracts"))
}
tasks.processResources { dependsOn(syncContracts) }

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
