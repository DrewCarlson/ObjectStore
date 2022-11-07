/**
 * ObjectStore
 * Copyright (C) 2022 Drew Carlson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package objectstore.core

import kotlin.reflect.KType

public typealias ValueTransformer<T> = (type: KType, value: T) -> T

public fun <T : Any> ObjectStoreWriter.transformValue(
    transformGet: ValueTransformer<T?>,
    transformSet: ValueTransformer<T?>
): ValueTransformingStoreWriter {
    @Suppress("UNCHECKED_CAST")
    return ValueTransformingStoreWriter(
        this,
        transformGet as ValueTransformer<Any?>,
        transformSet as ValueTransformer<Any?>
    )
}

public class ValueTransformingStoreWriter(
    private val storeWriter: ObjectStoreWriter,
    private val transformGet: ValueTransformer<Any?>,
    private val transformSet: ValueTransformer<Any?>
) : ObjectStoreWriter by storeWriter {

    override fun <T : Any> getRaw(type: KType, key: String): T? {
        return get(type, key)
    }

    override fun <T : Any> putRaw(type: KType, key: String, value: T?) {
        put(type, key, value)
    }

    override fun <T : Any> get(type: KType, key: String): T? {
        return storeWriter.get<T>(type, key)?.let {
            @Suppress("UNCHECKED_CAST")
            transformGet(type, it) as? T?
        }
    }

    override fun <T : Any> put(type: KType, key: String, value: T?) {
        storeWriter.put(type, key, transformSet(type, value))
    }
}
