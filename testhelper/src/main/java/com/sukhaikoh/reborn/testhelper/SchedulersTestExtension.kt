package com.sukhaikoh.reborn.testhelper

import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A Junit 5 `Extension` for a test method or test class that helps
 * initializing [Schedulers] for each unit test.
 *
 * This class implements both [BeforeEachCallback] and [AfterEachCallback] to
 * initialize and tear down each unit test.
 */
class SchedulersTestExtension : BeforeEachCallback, AfterEachCallback {

    override fun afterEach(context: ExtensionContext?) {
        RxJavaPlugins.reset()
    }

    override fun beforeEach(context: ExtensionContext?) {
        RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
    }
}