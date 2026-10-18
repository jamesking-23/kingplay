package com.kingplay.core.common

import javax.inject.Qualifier

enum class KpDispatchers { Default, IO, Main }

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: KpDispatchers)

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
