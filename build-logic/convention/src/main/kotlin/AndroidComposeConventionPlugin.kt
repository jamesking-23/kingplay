import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            // Looked up by name: Gradle cannot find an extension by a type that has wildcards.
            (extensions.getByName("android") as CommonExtension<*, *, *, *, *, *>).apply {
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                val bom = libs.library("androidx-compose-bom")
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))
                add("implementation", libs.library("androidx-compose-ui"))
                add("implementation", libs.library("androidx-compose-ui-graphics"))
                add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
                add("implementation", libs.library("androidx-compose-material3"))
                add("implementation", libs.library("androidx-compose-material-icons-core"))
                add("implementation", libs.library("androidx-compose-material3-window-size"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
                add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
                add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
            }
        }
    }
}
