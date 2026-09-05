plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "ai.eigent.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.eigent.mobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
        jniLibs.useLegacyPackaging = true
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    sourceSets["main"].assets.srcDir("src/main/assets")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

chaquopy {
    defaultConfig {
        version = "3.11"
        buildPython("python3")
        pip {
            install("fastapi==0.115.12")
            install("uvicorn==0.34.2")
            install("httpx==0.28.1")
            install("pydantic==2.11.4")
            install("python-dotenv==1.1.0")
            install("fastapi-babel==1.0.0")
            install("pydantic-i18n==0.4.5")
            install("pydash==8.0.5")
            install("inflection==0.5.1")
            install("aiofiles==24.1.0")
            install("openai==1.99.3")
            install("numpy==1.26.4")
            install("qdrant-client==1.16.2")
            install("pyyaml==6.0.3")
            install("jsonschema==4.26.0")
            install("opentelemetry-api==1.34.1")
            install("opentelemetry-sdk==1.34.1")
            install("opentelemetry-exporter-otlp-proto-http==1.34.1")
            install("truststore==0.10.0")
            install("camel-ai==0.2.91a5")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.12.1")
}
