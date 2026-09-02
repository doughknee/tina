package com.tina.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/** The resumed activity, for APIs that need one (billing flow, in-app review). */
object ForegroundActivity : Application.ActivityLifecycleCallbacks {
    private var ref = WeakReference<Activity>(null)
    val current: Activity? get() = ref.get()

    override fun onActivityResumed(activity: Activity) { ref = WeakReference(activity) }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
