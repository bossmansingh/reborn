/*
 * Copyright (C) 2019 Su Khai Koh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sukhaikoh.reborn.viewmodel

import androidx.lifecycle.ViewModel
import dagger.MapKey
import kotlin.reflect.KClass

/**
 * Annotate the method to indicate the view model key is the [value].
 *
 * ### Example
 * ```
 * MyViewModel.kt
 * --------------
 * class MyViewModel @Inject constructor() : ViewModel { ... }
 *
 * MyDaggerModule.kt
 * -----------------
 * @Module
 * abstract class MyDaggerModule {
 *     ...
 *     @ViewModelKey(MyViewModel::class)
 *     abstract fun bindMyViewModel(viewModel: MyViewModel): ViewModel
 *     ...
 * ```
 *
 * @param value the class of the [ViewModel].
 */
@MustBeDocumented
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)