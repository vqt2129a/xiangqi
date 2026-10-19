import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Tầng Use case: điều phối domain, chỉ phụ thuộc vào :domain.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // Chia sẻ đồ giả lập (InMemoryGameRepository) cho test của các module khác.
    `java-test-fixtures`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}

dependencies {
    api(project(":domain"))
    // Flow cho trạng thái tài khoản (thư viện chuẩn của Kotlin, không phụ thuộc Android hay Firebase).
    api(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
