import java.util.Properties

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

    // * Firma de release: android/key.properties (no se commitea) apunta al keystore, que vive fuera del repositorio.
    // * Sin ese archivo (CI, otro dev) el release se firma con la clave de debug: compila igual, pero ese APK no
    // * sirve para publicar ni para actualizar uno firmado con la clave real.
    val propiedadesFirma = Properties()
    val archivoFirma = rootProject.file("key.properties")
    if (archivoFirma.exists()) archivoFirma.inputStream().use { propiedadesFirma.load(it) }

    signingConfigs {
        if (archivoFirma.exists()) {
            create("release") {
                storeFile = file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
