plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "org.the3deer.android.engine"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    // required by gltf parser
    implementation(libs.core.jackson.databind)
}

// Javadoc configuration
tasks.register<Javadoc>("javadoc") {
    // Ensure the R class and other generated sources are compiled first
    dependsOn("compileDebugJavaWithJavac")

    val mainSourceSet = android.sourceSets.getByName("main")
    
    // Set the source directories
    setSource(mainSourceSet.java.srcDirs)
    
    // Exclude the mock javax.imageio package from Javadoc generation
    // This prevents the "package exists in another module: java.desktop" error
    exclude("javax/imageio/**")
    exclude("de/javagl/jgltf/**")
    exclude("**/BuildConfig.java")
    exclude("**/R.java")
    exclude("org/the3deer/engine/util/mapbox/**")
    exclude("org/poly2tri/**")

    // Initialize classpath with Android SDK
    classpath = project.files(android.bootClasspath)

    options {
        val options = this as StandardJavadocDocletOptions
        options.links("https://developer.android.com/reference")
        options.links("https://docs.oracle.com/javase/8/docs/api/")
        options.addStringOption("Xdoclint:none", "-quiet")
        options.encoding = "UTF-8"
        options.charSet = "UTF-8"
        options.docTitle = "Android 3D Engine API"
        options.windowTitle = "Android 3D Engine API"
    }

    doFirst {
        // Use the debug variant's classpath and output
        val variant = android.libraryVariants.find { it.name == "debug" }
        if (variant != null) {
            val compileTask = variant.javaCompileProvider.get()
            // Add all project dependencies
            classpath += compileTask.classpath
            // Add the compiled classes (including the generated R class and mock javax.imageio)
            // This allows other classes that use javax.imageio to resolve symbols
            classpath += project.files(compileTask.destinationDirectory)
        }
    }
}
