package mobi.nftblockchain.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.facebook.applinks.AppLinkData
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import mobi.nftblockchain.app.Constants.BANNER_KEY
import mobi.nftblockchain.app.Constants.BASE_URL
import mobi.nftblockchain.app.Constants.INTERSTITIAL_KEY
import mobi.nftblockchain.app.Constants.ISBANNERSHOW
import mobi.nftblockchain.app.Constants.ISINTERSHOW
import mobi.nftblockchain.app.Constants.ISOPENAPPSHOW
import mobi.nftblockchain.app.Constants.KEY_PREFERENCE
import mobi.nftblockchain.app.Constants.SDK_KEY
import mobi.nftblockchain.app.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow


class MainActivity : AppCompatActivity(), MaxAdViewAdListener {
    private lateinit var adView: MaxAdView
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private var appOpenManager: ExampleAppOpenManager? = null
    private var baseUrl: String = ""
    private var lastAdShownTime: Long = 0
    private val adShowInterval = TimeUnit.MINUTES.toMillis(55)

    private var uploadMessageAboveL: ValueCallback<Array<Uri?>?>? = null
    private val FILE_CHOOSER_RESULT_CODE = 150
    private lateinit var interstitialAd: MaxInterstitialAd
    private var retryAttempt = 0.0
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: Editor
    private var openApp: Boolean = false
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showPush()
        } else {
            showPermissionDeniedMessage()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Creating an extended library configuration.

        interstitialAd = MaxInterstitialAd(INTERSTITIAL_KEY, applicationContext)

        adView = MaxAdView(BANNER_KEY, this)
        val config =
            AppMetricaConfig.newConfigBuilder("166726e4-4415-47c0-92c7-6cdecfe7d1b4").build()
        // Initializing the AppMetrica SDK.
        AppMetrica.activate(this, config)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = this.getSharedPreferences(KEY_PREFERENCE, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        val initConfig = AppLovinSdkInitializationConfiguration.builder(SDK_KEY, this)
            .setMediationProvider(AppLovinMediationProvider.MAX)
            .build()
        AppLovinSdk.getInstance(this).mediationProvider = "max"
        AppLovinSdk.getInstance(this).initialize(initConfig) { configuration ->

        }


        with(binding.webView) {
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.allowContentAccess = true
            settings.userAgentString = "mobi.nftblockchain.app"
            settings.allowFileAccess = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            settings.databaseEnabled = true
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);

            setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                val request = DownloadManager.Request(
                    Uri.parse(url)
                )
                Log.e("URL", "IT is BASE URL-> $url")
                baseUrl = url
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                        url, contentDisposition, mimeType
                    )
                )

                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request);
            }

            webViewClient = MyWebViewClient()
            webChromeClient = MyWebChromeClient()

            loadUrl("https://nftblockchain.mobi/game")
        }

        onBackPressedDispatcher.addCallback(this) {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                moveTaskToBack(true)
            }
        }

        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.fullyInitialize()
        AppLinkData.fetchDeferredAppLinkData(
            this
        ) {
            it?.let { link ->
                try {
                    val host = link.targetUri!!.host!!
                    openDeeplink(host)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


        try {
            val firebaseRemoteConfig = Firebase.remoteConfig

            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }

            firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

            firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

            firebaseRemoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.d(TAG, "Updated keys: " + configUpdate.updatedKeys)
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Config update error with code: " + error.code, error)
                }
            })
            fetchRemoteConfig(firebaseRemoteConfig)
        } catch (e: Exception) {
            Log.e("ERROR", "Error remote config -> ${e.message}")
        }

        checkAndRequestNotificationPermission()
    }

    fun checkAndRequestNotificationPermission() {
        // Проверка версии Android и наличия разрешения
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showPush()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationale()
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            showPush()
        }
    }

    private fun showPermissionRationale() {
        Toast.makeText(this, "Нужно разрешение на отправку уведомлений", Toast.LENGTH_LONG).show()
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "Разрешение на отправку уведомлений отсутствует", Toast.LENGTH_LONG)
            .show()
    }

    fun createBannerAd() {

        adView.setListener(this)
        val adContainer: FrameLayout = findViewById(R.id.adView)
        adContainer.addView(adView)
        adView.loadAd()
    }

    fun createInterstitialAd() {

        interstitialAd.setListener(this)

        interstitialAd.loadAd()
    }

    private fun fetchRemoteConfig(firebaseRemoteConfig: FirebaseRemoteConfig) {
        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    editor.putBoolean(ISBANNERSHOW, firebaseRemoteConfig.getBoolean("isBannerShow"))
                    editor.putBoolean(
                        ISOPENAPPSHOW,
                        firebaseRemoteConfig.getBoolean("isOpeanAppShow")
                    )
                    editor.putBoolean(ISINTERSHOW, firebaseRemoteConfig.getBoolean("isInterShow"))
                    editor.apply()
                    Log.e(
                        "TAG",
                        "ADS-> ${firebaseRemoteConfig.getBoolean("isBannerShow")}\n${
                            firebaseRemoteConfig.getBoolean("isOpeanAppShow")
                        }\n${firebaseRemoteConfig.getBoolean("isInterShow")}"
                    )
                }
            }
    }

    private fun showAdIfReady() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastAdShownTime

        if (elapsedTime >= adShowInterval) {
            val logger = AppEventsLogger.newLogger(applicationContext)
            logger.logEvent(AppEventsConstants.EVENT_NAME_ACHIEVED_LEVEL)
            Log.e("TAG", "${elapsedTime.toMinutes()} -> ${lastAdShownTime.toMinutes()}")
            if (interstitialAd.isReady) {
                interstitialAd.showAd(INTERSTITIAL_KEY)

            } else {
//                Toast.makeText(this, "Interstitial is not ready", Toast.LENGTH_SHORT).show()
            }
            lastAdShownTime = currentTime
        }
    }

    private fun showPush() {
        val intervals = listOf(24, 48, 72, 168)

        for (i in intervals.indices) {
            val hours = intervals[i]
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra(Intent.EXTRA_ALARM_COUNT, i + 1)
            }
            val pendingIntent =
                PendingIntent.getBroadcast(
                    this,
                    i,
                    intent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                )

            val triggerTime = (hours * 60 * 60 * 1000).toLong()

            val alarmManager =
                getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + triggerTime,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun openDeeplink(link: String) {
        val s = link.split("_")
        binding.webView.loadUrl("https://nftblockchain.${s[0]}/${s[1]}?r=${s[2]}")
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        binding.webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webView.restoreState(savedInstanceState)
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            baseUrl = url.toString()
            editor.putString(BASE_URL, url)
            editor.apply()
            createInterstitialAd()
            //Вывод OpenAppAd если ISOPENAPPSHOW = true при входе в приложение
            if (sharedPreferences.getBoolean(ISOPENAPPSHOW, false) && !openApp) {
                appOpenManager = ExampleAppOpenManager(applicationContext)
                openApp = true
            }
            //Вывод BANNER если ISBANNERSHOW = true
            if (sharedPreferences.getBoolean(ISBANNERSHOW, false)) {
                if (!openApp)
                    createBannerAd()
                binding.adView.isVisible = true
                Log.e("TAG", "createBannerAd")
            } else {
                binding.adView.isVisible = false
            }
            binding.splash.isVisible = false
            binding.webView.isVisible = true


            //Вывод INTERSTITIAL если есть в ссылке /free и состояние ISINTERSHOW = true и вывод каждые 55 минут после запуска приложения
            Log.e("TAG", "URL -> $url")

            if (sharedPreferences.getBoolean(
                    ISINTERSHOW,
                    false
                ) && url!!.contains("/free")///wallet
            ) {

                showAdIfReady()
                Log.e("TAG", "INTERSTITIAL Is ready -> $url")
            }
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString()
            Log.d(TAG, "shouldOverrideUrlLoading: $url")

            return if (URLUtil.isNetworkUrl(url)) {
                false
            } else {
                val intent = Intent(Intent.ACTION_VIEW, request?.url)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                true
            }
        }
    }

    private inner class MyWebChromeClient : WebChromeClient() {
        private var mCustomView: View? = null
        private var mCustomViewCallback: CustomViewCallback? = null
        private var mOriginalOrientation = 0
        private var mOriginalSystemUiVisibility = 0

        //For Android API >= 21 (5.0 OS)
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri?>?>,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            uploadMessageAboveL = filePathCallback
            openChooserActivity()
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            request.grant(request.resources)
        }

        override fun getDefaultVideoPoster(): Bitmap? {
            return if (mCustomView == null) {
                null
            } else BitmapFactory.decodeResource(applicationContext.resources, 2130837573)
        }

        override fun onHideCustomView() {
            (window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null
            window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
            requestedOrientation = mOriginalOrientation
            mCustomViewCallback!!.onCustomViewHidden()
            mCustomViewCallback = null
        }

        override fun onShowCustomView(
            paramView: View?,
            paramCustomViewCallback: CustomViewCallback?
        ) {
            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
            mOriginalOrientation = requestedOrientation
            mCustomViewCallback = paramCustomViewCallback
            (window.decorView as FrameLayout).addView(
                mCustomView,
                FrameLayout.LayoutParams(-1, -1)
            )
            window.decorView.systemUiVisibility = 3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun openChooserActivity() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, "Choose"), FILE_CHOOSER_RESULT_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
        adView.destroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data)
            }
        }
    }

    private fun onActivityResultAboveL(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null) return
        var results: Array<Uri?>? = null
        if (resultCode == RESULT_OK) {
            if (intent != null) {
                val dataString = intent.dataString
                val clipData = intent.clipData
                if (clipData != null) {
                    results = arrayOfNulls(clipData.itemCount)
                    for (i in 0 until clipData.itemCount) {
                        val item = clipData.getItemAt(i)
                        results[i] = item.uri
                    }
                }
                if (dataString != null) results = arrayOf(Uri.parse(dataString))
            }
        }
        uploadMessageAboveL?.onReceiveValue(results)
        uploadMessageAboveL = null
    }

    override fun onAdLoaded(p0: MaxAd) {
        retryAttempt = 0.0
    }

    override fun onAdDisplayed(p0: MaxAd) {
        TODO("Not yet implemented")
    }

    override fun onAdHidden(p0: MaxAd) {
        interstitialAd.loadAd()
    }

    override fun onAdClicked(p0: MaxAd) {
        TODO("Not yet implemented")
    }

    override fun onAdLoadFailed(p0: String, p1: MaxError) {
        retryAttempt++
        val delayMillis = TimeUnit.SECONDS.toMillis(2.0.pow(min(6.0, retryAttempt)).toLong())

        Handler().postDelayed({ interstitialAd.loadAd() }, delayMillis)
    }

    override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {
        interstitialAd.loadAd()
    }

    override fun onAdExpanded(p0: MaxAd) {
        TODO("Not yet implemented")
    }

    override fun onAdCollapsed(p0: MaxAd) {
        TODO("Not yet implemented")
    }
}