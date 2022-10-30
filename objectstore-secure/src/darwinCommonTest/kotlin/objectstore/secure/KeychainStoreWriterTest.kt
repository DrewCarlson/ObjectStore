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
package objectstore.secure

import kotlin.test.Test
import kotlin.test.assertEquals

class KeychainStoreWriterTest {

    @Test
    fun testDataConversion() {
        assertEquals("test", "test".toNSData()?.asString())
        assertEquals("1", true.toNSData()?.asString())
        assertEquals(true, true.toNSData()?.asBoolean())
        assertEquals(Int.MIN_VALUE, Int.MIN_VALUE.toNSData().asInt())
        assertEquals(Int.MAX_VALUE, Int.MAX_VALUE.toNSData().asInt())
        assertEquals(Long.MAX_VALUE, Long.MAX_VALUE.toNSData().asLong())
        assertEquals(Long.MIN_VALUE, Long.MIN_VALUE.toNSData().asLong())
        assertEquals(Float.MAX_VALUE, Float.MAX_VALUE.toNSData().asFloat())
        assertEquals(Float.MIN_VALUE, Float.MIN_VALUE.toNSData().asFloat())
    }
}
