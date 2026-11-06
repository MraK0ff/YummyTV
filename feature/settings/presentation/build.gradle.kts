plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.feature.settings.presentation"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:error:api"))
    implementation(project(":core:model"))
    api(project(":core:mvi"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:tv"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:api"))
    implementation(project(":feature:settings:api"))
    api(project(":feature:home:domain"))
    implementation(project(":feature:update:domain"))
    implementation(project(":feature:video-download:domain"))

    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.appcompat)

    testImplementation(project(":core:testing"))
}
