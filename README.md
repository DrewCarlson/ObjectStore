# ObjectStore

[![Maven Central](https://img.shields.io/maven-central/v/org.drewcarlson/objectstore-core-jvm?label=maven&color=blue)](https://search.maven.org/search?q=g:org.drewcarlson%20a:objectstore-*)
![](https://github.com/DrewCarlson/ObjectStore/workflows/Tests/badge.svg)
![Codecov](https://img.shields.io/codecov/c/github/drewcarlson/objectstore?token=7DKJUD60BO)

A modular object storage framework for Kotlin multiplatform projects.

## Usage

`ObjectStore` provides a simple key/value storage interface which by default uses Type details to derive the Key
automatically.  To create an `ObjectStore` you need two things:

- `ObjectStoreWriter`: Providers the persistence mechanism to store data for later access.
- `ObjectStoreSerializer`: Provides the serialization mechanism to transform objects for storage.

```kotlin
val store = ObjectStore(
    storeWriter = SharedPreferencesStoreWriter("prefs", context),
    storeSerializer = JsonStoreSerializer()
)

// Store an object
store.put(User("username", "email", ...))
// Remove an object
store.put<User>(null)

// Get an object or null
val user: User? = store.getOrNull<User>()
// Get an object or throw
val user: User = store.get<User>()
// Get an object or default
val user: User = store.get(default = User(...))

// Get a StateFlow
val userFlow: StateFlow<User?> = store.getFlow<User>()

// Calls to `put` new user objects will be emitted
userFlow.collect { println(it) }
```

When storing basic types such as `String`, `Boolean`, etc. you must provide a `key` for the record.
```kotlin
store.put(false, key = "my_key")
store.get<Boolean>(default = false, key = "my_key")
```

## Serializers

Turning objects into data suitable for storage requires a `ObjectStoreSerializer` implementation.
The following modules provide serialization capabilities using the matching
[Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) module.

- `objectstore-cbor`: `CborStoreSerializer()`
- `objectstore-json`: `JsonStoreSerializer()`
- `objectstore-protobuf`: `ProtoBufStoreSerializer()`

## Writers

Storing object data requires a `ObjectStoreWriter` implementation.
The following Writers are provided in the `objectstore-core` module:

- Android: `SharedPreferencesStoreWriter("prefs_name", context)`
- iOS/macOS/tvOS/watchOS: `UserDefaultsStoreWriter()`
- All: `InMemoryStoreWriter()`

### Secure Writers

To store data in a secure way, the `objectstore-secure` module provides Writers which encrypt data when stored on disk.

- iOS/macOS/tvOS/watchOS: `KeychainStoreWritre("com.service.name", "com.service.group")`
- Android: `EncryptedSharedPreferencesStoreWriter("prefs_name", context)`

## Download

[![Maven Central](https://img.shields.io/maven-central/v/org.drewcarlson/objectstore-core-jvm?label=maven&color=blue)](https://search.maven.org/search?q=g:org.drewcarlson%20a:objectstore-*)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/org.drewcarlson/objectstore-core-jvm?server=https%3A%2F%2Fs01.oss.sonatype.org)

![](https://img.shields.io/static/v1?label=&message=Platforms&color=grey)
![](https://img.shields.io/static/v1?label=&message=Js&color=blue)
![](https://img.shields.io/static/v1?label=&message=Jvm&color=blue)
![](https://img.shields.io/static/v1?label=&message=Linux&color=blue)
![](https://img.shields.io/static/v1?label=&message=macOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=Windows&color=blue)
![](https://img.shields.io/static/v1?label=&message=iOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=tvOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=watchOS&color=blue)

```kotlin
repositories {
    mavenCentral()
    // Or snapshots
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("org.drewcarlson:objectstore-core:$VERSION")
    
    // Serializers
    implementation("org.drewcarlson:objectstore-cbor:$VERSION")
    implementation("org.drewcarlson:objectstore-json:$VERSION")
    implementation("org.drewcarlson:objectstore-protobuf:$VERSION")
    
    // Writers
    implementation("org.drewcarlson:objectstore-secure:$VERSION")
}
```

<details>
<summary>Toml (Click to expand)</summary>

```toml
[versions]
objectstore = "1.0.0-SNAPSHOT"

[libraries]
objectstore-core = { module = "org.drewcarlson:objectstore-core", version.ref = "objectstore" }
objectstore-cbor = { module = "org.drewcarlson:objectstore-cbor", version.ref = "objectstore" }
objectstore-json = { module = "org.drewcarlson:objectstore-json", version.ref = "objectstore" }
objectstore-protobuf = { module = "org.drewcarlson:objectstore-protobuf", version.ref = "objectstore" }
objectstore-secure = { module = "org.drewcarlson:objectstore-secure", version.ref = "objectstore" }
```
</details>

### License

This project is licensed under Apache-2.0, found in [LICENSE](LICENSE).
