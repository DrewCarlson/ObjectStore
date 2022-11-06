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

import kotlinx.serialization.Serializable
import objectstore.cbor.CborStoreSerializer
import objectstore.json.JsonStoreSerializer
import objectstore.protobuf.ProtoBufStoreSerializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.*

expect fun getStoreWriter(secure: Boolean): ObjectStoreWriter

expect abstract class RobolectricTestCases()

@Serializable
data class TestClass(
    val a: String = "test",
    val b: Long = Long.MAX_VALUE
)

@Serializable
data class TestClassWithGeneric<T>(
    val value: T?
)

class NoSerializerStoreTest : ObjectStoreTest(CborStoreSerializer())
class CborObjectStoreTest : ObjectStoreTest(CborStoreSerializer())
class JsonObjectStoreTest : ObjectStoreTest(JsonStoreSerializer())
class ProtobufObjectStoreTest : ObjectStoreTest(ProtoBufStoreSerializer())

abstract class ObjectStoreTest(
    private val serializer: ObjectStoreSerializer,
    private val secure: Boolean = false
) : RobolectricTestCases() {

    private lateinit var store: ObjectStore
    private lateinit var storeWriter: ObjectStoreWriter

    @BeforeTest
    fun setup() {
        storeWriter = getStoreWriter(secure)
        store = ObjectStore(
            storeWriter = storeWriter,
            storeSerializer = serializer
        )
    }

    private fun keyForType(type: KType): String {
        return ObjectStore.keyForType(type)
    }

    @Test
    fun testKeyForType() {
        assertEquals(nameFromKtype(typeOf<TestClass>()), keyForType(typeOf<TestClass>()))
        assertEquals(
            "objectstore.core.TestClassWithGeneric<kotlin.Boolean>",
            keyForType(typeOf<TestClassWithGeneric<Boolean>>())
        )
        assertEquals(
            "objectstore.core.TestClassWithGeneric<objectstore.core.TestClass>",
            keyForType(typeOf<TestClassWithGeneric<TestClass>>())
        )
    }

    @Test
    fun testGetOrNullWithoutValue() {
        assertNull(store.getOrNull<TestClass>())
    }

    @Test
    fun testGetWithoutDefaultAndWithoutValue() {
        assertFailsWith<IllegalArgumentException> {
            store.get<TestClass>()
        }
    }

    @Test
    fun testGetWithDefaultAndWithoutValue() {
        val expected = TestClass()
        val actual1 = store.get(expected)
        val actual2 = store.get<TestClass>()
        assertEquals(expected, actual1)
        assertEquals(expected, actual2)
    }

    @Test
    fun testGetFlowWithoutDefaultAndWithoutValueEmitsNull() {
        val subject = store.getFlow<TestClass>()
        assertNull(subject.value)
    }

    @Test
    fun testGetFlowWithoutDefaultEmitsSetValue() {
        val subject = store.getFlow<TestClass>()
        val expected = TestClass()

        store.put(expected)
        assertEquals(expected, subject.value)
    }

    @Test
    fun testGetFlowWithDefaultEmitsDefault() {
        val expected = TestClass()
        val subject = store.getFlow(default = expected)

        assertEquals(expected, subject.value)
    }

    @Test
    fun testGetFlowEmitsRemove() {
        val expected = TestClass()
        val subject = store.getFlow(default = expected)

        assertEquals(expected, subject.value)

        store.remove<TestClass>()

        assertNull(subject.value)
    }

    @Test
    fun testSetAndGetWithBasicType() {
        store.put(value = 1, "test")
        assertEquals(1, store.get(key = "test"))
        store.put(value = true, "test2")
        assertEquals(true, store.get(key = "test2"))
        store.put(value = 1L, "test3")
        assertEquals(1L, store.get(key = "test3"))
        store.put(value = 1f, "test4")
        assertEquals(1f, store.get(key = "test4"))
    }

    @Test
    fun testSetAndRemove() {
        store.put(value = 1, "test")
        store.remove<Int>("test")
        assertNull(store.getOrNull(key = "test"))
        store.put(value = true, "test2")
        store.remove<Int>("test2")
        assertNull(store.getOrNull(key = "test2"))
        store.put(value = 1L, "test3")
        store.remove<Long>("test3")
        assertNull(store.getOrNull(key = "test3"))
        store.put(value = 1f, "test4")
        store.remove<Float>("test4")
        assertNull(store.getOrNull(key = "test4"))
    }

    @Test
    fun testKeys() {
        store.put(TestClass())
        store.put(value = 1, "test")

        assertEquals(setOf("test", "objectstore.core.TestClass"), store.keys())
    }

    @Test
    fun testClear() {
        store.put(TestClass())
        store.put(value = 1, "test")
        store.clear()

        assertEquals(emptySet(), store.keys())
    }

    @Test
    fun testClearEmitsNullToFlows() {
        store.put(TestClass())

        val flow = store.getFlow<TestClass>()

        store.clear()

        assertNull(store.getOrNull<TestClass>())
        assertNull(flow.value)
    }

    @Test
    fun testBasicTypesFailWithoutKey() {
        assertFailsWith<IllegalArgumentException> { store.get<Boolean>() }
        assertFailsWith<IllegalArgumentException> { store.get<Long>() }
        assertFailsWith<IllegalArgumentException> { store.get<Int>() }
        assertFailsWith<IllegalArgumentException> { store.get<Double>() }
        assertFailsWith<IllegalArgumentException> { store.get<Float>() }
        assertFailsWith<IllegalArgumentException> { store.get<String>() }
        assertFailsWith<IllegalArgumentException> { store.get<Char>() }
        assertFailsWith<IllegalArgumentException> { store.get<Array<String>>() }
        assertFailsWith<IllegalArgumentException> { store.get<UInt>() }
        assertFailsWith<IllegalArgumentException> { store.get<ULong>() }
        assertFailsWith<IllegalArgumentException> { store.get<UByte>() }
        assertFailsWith<IllegalArgumentException> { store.get<UShort>() }
    }

    @Test
    fun testValueTransformingStoreWriter() {
        val subject = storeWriter.transformValue<Any>(
            transformGet = { _, value -> (value as? String)?.removePrefix("test-") },
            transformSet = { _, value -> (value as? String)?.let { "test-$it" } }
        )
        val testStore = ObjectStore(subject, serializer)

        testStore.put(key = "test", value = "abc")

        assertEquals("test-abc", storeWriter.get(typeOf<String>(), key = "test"))
        assertEquals("abc", subject.get(typeOf<String>(), key = "test"))
        assertEquals("abc", testStore.get(key = "test"))

        testStore.put<String>(key = "test", value = null)
    }

    @Test
    fun testMemCachedStoreWriterWithBasicType() {
        val cache = InMemoryStoreWriter()
        val subject = storeWriter.memCached(cache)
        val testStore = ObjectStore(subject, serializer)

        val testStringKey = "test"
        val testStringValue = "abc"

        testStore.put(key = testStringKey, value = testStringValue)

        assertEquals(setOf(testStringKey), cache.keys())

        assertEquals(testStringValue, cache.get(typeOf<String>(), testStringKey))
        assertEquals(testStringValue, storeWriter.get(typeOf<String>(), testStringKey))

        testStore.remove<String>(testStringKey)

        assertNull(cache.get(typeOf<String>(), testStringKey))
        assertNull(storeWriter.get(typeOf<String>(), testStringKey))
    }

    @Test
    fun testMemCachedStoreWriterWithObject() {
        val cache = InMemoryStoreWriter()
        val subject = storeWriter.memCached(cache)
        val testStore = ObjectStore(subject, serializer)

        val key = "objectstore.core.TestClass"
        val testClass = TestClass()

        testStore.put(testClass)

        assertEquals(setOf(key), cache.keys())

        val serializedClass = serializer.serialize(typeOf<TestClass>(), TestClass())
        assertEquals(testClass, testStore.getOrNull())
        assertEquals(testClass, cache.get(typeOf<TestClass>(), key))
        assertEquals(serializedClass, storeWriter.get(typeOf<String>(), key))

        testStore.remove<TestClass>()

        assertNull(cache.get(typeOf<TestClass>(), key))
        assertNull(storeWriter.get(typeOf<String>(), key))
        assertNull(testStore.getOrNull(key))
    }

    @Test
    fun testMemCachedStoreWriterWithExistingData() {
        val cache = InMemoryStoreWriter()
        val subject = storeWriter.memCached(cache)
        val testStore = ObjectStore(subject, serializer)

        val testClass = TestClass()
        val key = "objectstore.core.TestClass"
        val serializedClass = serializer.serialize(typeOf<TestClass>(), TestClass())

        storeWriter.put(typeOf<String>(), key, serializedClass)

        assertEquals(emptySet(), cache.keys())
        assertEquals(setOf(key), subject.keys())
        assertEquals(setOf(key), testStore.keys())
        assertEquals(serializedClass, storeWriter.get(typeOf<String>(), key))
        assertEquals(serializedClass, subject.get(typeOf<String>(), key))
        assertEquals(testClass, testStore.getOrNull())
        assertEquals(testClass, cache.get(typeOf<TestClass>(), key))
        assertEquals(setOf(key), cache.keys())

        testStore.remove<TestClass>()

        assertNull(cache.get(typeOf<TestClass>(), key))
        assertNull(storeWriter.get(typeOf<String>(), key))
        assertNull(subject.get(typeOf<String>(), key))
        assertNull(testStore.getOrNull(key))
    }
}
