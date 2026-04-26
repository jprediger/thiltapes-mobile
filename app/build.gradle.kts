import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "br.univates.mobile.thiltapes"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "br.univates.mobile.thiltapes"
        minSdk = 32
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val propsLocais = Properties()
        val arquivoLocalProps = rootProject.file("local.properties")
        if (arquivoLocalProps.exists()) {
            arquivoLocalProps.inputStream().use { propsLocais.load(it) }
        }
        val urlDeclarada = propsLocais.getProperty("THILTAPES_API_BASE_URL")?.trim().orEmpty()
        val urlEfetiva = if (urlDeclarada.isNotEmpty()) {
            urlDeclarada.trimEnd('/')
        } else {
            "https://skirts-programmes-wage-deposits.trycloudflare.com"
        }
        buildConfigField("String", "API_BASE_URL", "\"$urlEfetiva\"")
        buildConfigField(
            "boolean",
            "API_BASE_URL_DEFINIDA_EM_LOCAL_PROPERTIES",
            "${urlDeclarada.isNotEmpty()}"
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.coordinatorlayout)
    implementation(libs.recyclerview)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.photoview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.maplibre.android)
    implementation(libs.volley)
    implementation(libs.gson)
}