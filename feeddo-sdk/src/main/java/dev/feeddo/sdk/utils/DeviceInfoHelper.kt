package dev.feeddo.sdk.utils

import android.content.Context
import android.os.Build
import java.util.Locale

data class DeviceInfo(
    val platform: String = "android",
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String,
    val locale: String,
    val country: String
)

object DeviceInfoHelper {
    fun getDeviceInfo(context: Context): DeviceInfo {
        val appContext = context.applicationContext

        val manufacturer = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val model = Build.MODEL
        val deviceModel = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }

        val osVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        var appVersion = "1.0.0"
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
            appVersion = packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            // Fallback to default
        }

        val defaultLocale = Locale.getDefault()
        val localeTag = defaultLocale.toLanguageTag()
        val country = defaultLocale.country.ifEmpty { "US" }

        return DeviceInfo(
            platform = "android",
            deviceModel = deviceModel,
            osVersion = osVersion,
            appVersion = appVersion,
            locale = localeTag,
            country = country
        )
    }
}

