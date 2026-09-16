package com.naminfo

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder
import com.google.android.material.color.DynamicColors
import com.naminfo.compatibility.Compatibility
import com.naminfo.core.CoreContext
import com.naminfo.core.CorePreferences
import org.linphone.core.Factory
import org.linphone.core.LogCollectionState
import org.linphone.core.LogLevel
import com.naminfo.core.ManagedConfigurationReceiver
import com.naminfo.core.VFS
import org.linphone.core.tools.Log

@MainThread
class DiyaOneApplication : Application(), SingletonImageLoader.Factory {
    companion object {
        private const val TAG = "[Linphone Application]"

        @SuppressLint("StaticFieldLeak")
        lateinit var corePreferences: CorePreferences

        @SuppressLint("StaticFieldLeak")
        lateinit var coreContext: CoreContext
    }

    override fun onCreate() {
        super.onCreate()
        val context = applicationContext

        val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Linphone:AppCreation"
        )
        wakeLock.acquire(20000L) // 20 seconds

        Factory.instance().setLogCollectionPath(context.filesDir.absolutePath)
        Factory.instance().enableLogCollection(LogCollectionState.Enabled)
        // For VFS
        Factory.instance().setCacheDir(context.cacheDir.absolutePath)

        corePreferences = CorePreferences(context)
        corePreferences.copyAssetsFromPackage()

        val config = Factory.instance().createConfigWithFactory(
            corePreferences.configPath,
            corePreferences.factoryConfigPath
        )
        corePreferences.config = config

        if (!VFS.isEnabled(context)) {
            android.util.Log.i(TAG, "$TAG Enabling AES-256 encrypted storage")
            VFS.enable(context)
        }
        if (VFS.isEnabled(context)) {
            VFS.setup(context)
        }

        val appName = context.getString(R.string.app_name)
        Factory.instance().setLoggerDomain(appName)
        Factory.instance().loggingService.setLogLevel(LogLevel.Message)
        Factory.instance().enableLogcatLogs(corePreferences.printLogsInLogcat)

        Log.i("$TAG Report Core preferences initialized")
        Compatibility.setupAppStartupListener(context)

        coreContext = CoreContext(context)
        coreContext.start()

        ContextCompat.registerReceiver(
            this,
            ManagedConfigurationReceiver(),
            IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        DynamicColors.applyToActivitiesIfAvailable(this)
        wakeLock.release()
    }

    override fun onTrimMemory(level: Int) {
        Log.w("$TAG onTrimMemory called with level [${trimLevelToString(level)}]($level) !")
        when (level) {
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                Log.i("$TAG Memory trim required, clearing imageLoader memory cache")
                imageLoader.memoryCache?.clear()
            }
            else -> {}
        }
        super.onTrimMemory(level)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        // When VFS is enabled, prevent Coil from keeping plain version of files on disk
        val diskCachePolicy = if (VFS.isEnabled(applicationContext)) {
            CachePolicy.DISABLED
        } else {
            CachePolicy.ENABLED
        }

        return ImageLoader.Builder(this)
            .crossfade(false)
            .components {
                add(VideoFrameDecoder.Factory())
                // add(GifDecoder.Factory) // Do not add it, GIFs are properly rendered without it and adding it breaks resizing...
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                val cache = cacheDir.resolve("image_cache")
                DiskCache.Builder()
                    .directory(cache)
                    .maxSizePercent(0.02)
                    .build()
            }
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(diskCachePolicy)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    private fun trimLevelToString(level: Int): String {
        return when (level) {
            TRIM_MEMORY_UI_HIDDEN -> "Hidden UI"
            TRIM_MEMORY_RUNNING_MODERATE -> "Moderate (Running)"
            TRIM_MEMORY_RUNNING_LOW -> "Low"
            TRIM_MEMORY_RUNNING_CRITICAL -> "Critical"
            TRIM_MEMORY_BACKGROUND -> "Background"
            TRIM_MEMORY_MODERATE -> "Moderate"
            TRIM_MEMORY_COMPLETE -> "Complete"
            else -> level.toString()
        }
    }
}
