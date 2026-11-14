# Feeddo SDK Proguard rules
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class dev.feeddo.sdk.models.** { *; }

