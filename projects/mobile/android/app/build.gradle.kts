plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
    // * Firebase (push del móvil): lee google-services.json, que tiene que ser del mismo applicationId
    id("com.google.gms.google-services")
}

android {
    // * El namespace es el paquete de las clases (MainActivity); NO tiene que ser igual al applicationId, que es la
    // * identidad de la app en el teléfono y en Firebase
    namespace = "com.example.mobile"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // * La identidad de la app. Tiene que coincidir con el `package_name` de google-services.json (Firebase) y no se
        // * cambia a la ligera: Android la trata como otra app distinta (no hereda los datos de la anterior).
        applicationId = "com.drivehub.app"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
