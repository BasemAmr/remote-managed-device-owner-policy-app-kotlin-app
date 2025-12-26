package com.selfcontrol.data.worker

/**
 * CustomWorkerFactory is not needed when using @HiltWorker annotation
 * with androidx.hilt.work.HiltWorkerFactory.
 * 
 * The HiltWorkerFactory provided by Hilt handles DI for all workers
 * annotated with @HiltWorker.
 * 
 * This file is kept for documentation purposes and potential future
 * customization if needed.
 * 
 * Usage with Hilt:
 * 1. Annotate your Application class with @HiltAndroidApp
 * 2. Implement Configuration.Provider in Application
 * 3. Inject HiltWorkerFactory and use it in workManagerConfiguration
 * 
 * Example:
 * ```
 * @HiltAndroidApp
 * class SelfControlApp : Application(), Configuration.Provider {
 *     @Inject lateinit var workerFactory: HiltWorkerFactory
 *     
 *     override val workManagerConfiguration: Configuration
 *         get() = Configuration.Builder()
 *             .setWorkerFactory(workerFactory)
 *             .build()
 * }
 * ```
 */

// No implementation needed - Hilt handles this automatically
