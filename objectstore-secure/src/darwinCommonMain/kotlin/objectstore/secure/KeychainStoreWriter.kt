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
@file:OptIn(UnsafeNumber::class)

package objectstore.secure

import kotlinx.cinterop.*
import objectstore.core.ObjectStoreWriter
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.darwin.OSStatus
import platform.darwin.noErr
import kotlin.reflect.KType

public class KeychainStoreWriter(
    private val serviceName: String,
    private val accessGroup: String,
    private val access: KeychainAccess = KeychainAccess.WhenUnlockedThisDeviceOnly
) : ObjectStoreWriter {

    override fun canStoreType(type: KType): Boolean {
        return when (type.classifier) {
            String::class,
            Boolean::class,
            Int::class,
            Long::class,
            Float::class -> true

            else -> false
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun keys(): Set<String> = memScoped {
        context {
            val query = query {
                put(kSecClass, kSecClassGenericPassword)
                put(kSecReturnAttributes, kCFBooleanTrue)
                put(kSecMatchLimit, kSecMatchLimitAll)
            }
            val result = alloc<CFTypeRefVar>()
            val isValid = SecItemCopyMatching(query, result.ptr).checkState()
            if (isValid) {
                val items = CFBridgingRelease(result.value) as? List<Map<String, Any>>
                items?.mapNotNull { it["acct"] as? String }?.toSet() ?: setOf()
            } else {
                setOf()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: KType, key: String): T? {
        return when (type.classifier) {
            String::class -> value(key)?.asString() as T?
            Boolean::class -> value(key)?.asBoolean() as T?
            Long::class -> value(key)?.asLong() as T?
            Int::class -> value(key)?.asInt() as T?
            Float::class -> value(key)?.asFloat() as T?
            else -> unhandledType(type)
        }
    }

    override fun <T : Any> put(type: KType, key: String, value: T?) {
        if (exists(key)) {
            if (value == null) {
                delete(key)
            } else {
                update(key, value)
            }
        } else {
            val convertedValue = when (value) {
                is String -> value.toNSData()
                is Boolean -> value.toNSData()
                is Int -> value.toNSData()
                is Long -> value.toNSData()
                is Float -> value.toNSData()
                else -> unhandledType(type)
            }
            add(key, convertedValue)
        }
    }

    override fun clear() {
        context {
            SecItemDelete(
                query {
                    put(kSecClass, kSecClassGenericPassword)
                }
            ).checkState()
        }
    }

    private fun exists(key: String): Boolean = context(key) { (account) ->
        val query = query {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrAccount, account)
            put(kSecReturnData, kCFBooleanFalse)
        }

        SecItemCopyMatching(query, null).checkState()
    }

    private fun add(key: String, value: NSData?): Boolean = context(key, value) { (account, data) ->
        val query = query {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrAccount, account)
            put(kSecValueData, data)
            put(kSecAttrAccessible, access.value)
        }
        SecItemAdd(query, null).checkState()
    }

    private fun update(key: String, value: Any?): Boolean = context(key, value) { (account, data) ->
        val query = query {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrAccount, account)
            put(kSecReturnData, kCFBooleanFalse)
        }

        val updateQuery = query { put(kSecValueData, data) }
        SecItemUpdate(query, updateQuery).checkState()
    }

    private fun delete(key: String) = context(key) { (account) ->
        val query = query {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrAccount, account)
        }

        SecItemDelete(query).checkState()
    }

    private fun value(key: String): NSData? = context(key) { (account) ->
        val query = query {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrAccount, account)
            put(kSecReturnData, kCFBooleanTrue)
            put(kSecMatchLimit, kSecMatchLimitOne)
        }

        memScoped {
            val result = alloc<CFTypeRefVar>()
            SecItemCopyMatching(query, result.ptr)
            CFBridgingRelease(result.value) as? NSData
        }
    }

    private class Context(val refs: Map<CFStringRef?, CFTypeRef?>) {
        class Builder(refs: Map<CFStringRef?, CFTypeRef?>) {
            private val map = mutableMapOf<CFStringRef?, CFTypeRef?>().apply { putAll(refs) }
            fun put(key: CFStringRef?, value: CFTypeRef?) {
                map[key] = value
            }

            fun build(): CFDictionaryRef? {
                return CFDictionaryCreateMutable(null, map.size.convert(), null, null).apply {
                    map.entries.forEach { CFDictionaryAddValue(this, it.key, it.value) }
                    CFAutorelease(this)
                }
            }
        }

        fun query(build: Builder.() -> Unit): CFDictionaryRef? {
            return Builder(refs).apply(build).build()
        }
    }

    private fun <T> context(vararg values: Any?, block: Context.(List<CFTypeRef?>) -> T): T {
        val standard = buildMap {
            put(kSecAttrService, CFBridgingRetain(serviceName))
            put(kSecAttrAccessGroup, CFBridgingRetain(accessGroup))
        }
        val custom = values.map { CFBridgingRetain(it) }
        return block(Context(standard), custom).apply {
            (standard.values + custom).forEach(::CFBridgingRelease)
        }
    }

    private fun OSStatus.checkState(): Boolean {
        check(toUInt() == noErr) {
            "Keychain access failed: errorCode=$this " + when (this) {
                errSecInteractionNotAllowed -> "errSecInteractionNotAllowed"
                errSecUnimplemented -> "errSecUnimplemented"
                errSecNotAvailable -> "errSecNotAvailable"
                errSecItemNotFound -> "errSecItemNotFound"
                errSecAuthFailed -> "errSecAuthFailed"
                errSecAllocate -> "errSecAllocate"
                errSecDecode -> "errSecDecode"
                errSecBadReq -> "errSecBadReq"
                errSecParam -> "errSecParam"
                errSecFileTooBig -> "errSecFileTooBig"
                errSecInvalidKeyLabel -> "errSecInvalidKeyLabel"
                errSecInvalidAttributeKey -> "errSecInvalidAttributeKey"
                errSecInvalidKeychain -> "errSecInvalidKeychain"
                else -> ""
            }
        }
        return true
    }
}

internal fun String.toNSData(): NSData? =
    NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)

internal fun Boolean.toNSData(): NSData? {
    return (if (this) "1" else "0").toNSData()
}

internal fun Int.toNSData(): NSData = memScoped {
    val int = alloc<IntVar>().also { it.value = this@toNSData }
    return NSData.create(bytes = int.ptr, length = sizeOf<IntVar>().convert())
}

internal fun Long.toNSData(): NSData = memScoped {
    val int = alloc<LongVar>().also { it.value = this@toNSData }
    return NSData.create(bytes = int.ptr, length = sizeOf<LongVar>().convert())
}

internal fun Float.toNSData(): NSData = memScoped {
    val int = alloc<FloatVar>().also { it.value = this@toNSData }
    return NSData.create(bytes = int.ptr, length = sizeOf<FloatVar>().convert())
}

internal fun NSData.asString(): String? {
    return NSString.create(this, NSUTF8StringEncoding) as String?
}

internal fun NSData.asBoolean(): Boolean? {
    return (asString() ?: return null) == "1"
}

internal fun NSData.asInt(): Int? {
    return bytes?.reinterpret<IntVar>()?.pointed?.value
}

internal fun NSData.asLong(): Long? {
    return bytes?.reinterpret<LongVar>()?.pointed?.value
}

internal fun NSData.asFloat(): Float? {
    return bytes?.reinterpret<FloatVar>()?.pointed?.value
}
