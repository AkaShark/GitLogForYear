plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(project(":common:tool"))
        }

//        val desktopMain by getting {
//            dependencies {
//                implementation(libs.kotlinx.coroutinesSwing)
//            }
//        }

    }
}