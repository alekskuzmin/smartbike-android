/*
 * MIT License
 *
 * Copyright (c) 2019 Kreitai OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.kreitai.orangebikes.core.injection

import com.kreitai.orangebikes.core.dispatcher.StationsDispatcher
import com.kreitai.orangebikes.core.dispatcher.StationsDispatcherImpl
import com.kreitai.orangebikes.core.state.Reducer
import com.kreitai.orangebikes.core.state.ReducerImpl
import com.kreitai.orangebikes.core.state.StateHolder
import com.kreitai.orangebikes.core.state.StateHolderImpl
import com.kreitai.orangebikes.core.utils.coroutines.CoroutineContextProvider
import org.koin.dsl.module

val stateModule = module {

    single { CoroutineContextProvider() }
    single { StationsDispatcherImpl(get(), get()) as StationsDispatcher }
    single { ReducerImpl() as Reducer }
    single { StateHolderImpl(get(), get()) as StateHolder }
}
