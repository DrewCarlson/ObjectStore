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
package objectstore.cbor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.serializer
import objectstore.core.ObjectStoreSerializer
import kotlin.reflect.KType

@OptIn(ExperimentalSerializationApi::class)
private val defaultCbor = Cbor {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalSerializationApi::class)
public class CborStoreSerializer(
    private val cbor: Cbor = defaultCbor
) : ObjectStoreSerializer {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserialize(type: KType, data: String): T {
        return cbor.decodeFromHexString(serializer(type), data) as T
    }

    override fun <T : Any> serialize(type: KType, data: T): String {
        return cbor.encodeToHexString(serializer(type), data)
    }
}
