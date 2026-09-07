package com.example.liquidprismclock

import android.app.Application

/**
 * Application entry point. Kept intentionally minimal: all persistent state
 * lives in DataStore and is created lazily by the repository, so no heavy
 * initialization happens at process start (fast cold launch, low idle cost).
 */
class LiquidPrismApp : Application()
