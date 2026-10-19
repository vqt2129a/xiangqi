// Adapter Firebase: thông báo đẩy (FCM), báo lỗi (Crashlytics), đo hiệu năng (Performance),
// sao lưu ván đấu lên cloud (Auth ẩn danh + Firestore).
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.quangthanh.xiangqi.infrastructure.firebase"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":application"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
}
