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

import kotlinx.cinterop.*
import objectstore.core.ObjectStoreWriter
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.darwin.OSStatus
import platform.darwin.noErr

public class KeychainStoreWriter(
    private val serviceName: String,
    private val accessGroup: String,
    private val access: KeychainAccess = KeychainAccess.WhenUnlockedThisDeviceOnly
) : ObjectStoreWriter {

    override fun get(key: String): String? {
        return value(key)?.asString()
    }

    override fun put(key: String, value: String?) {
        if (exists(key)) {
            if (value == null) {
                delete(key)
            } else {
                update(key, value)
            }
        } else {
            add(key, value?.toNSData())
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

    private fun String.toNSData(): NSData? =
        NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)

    private fun NSData.asString(): String? {
        return NSString.create(this, NSUTF8StringEncoding) as String?
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