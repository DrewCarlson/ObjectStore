rootProject.name = "ObjectStore"

include(
    ":objectstore-core",
    ":objectstore-fs",
    ":objectstore-cbor",
    ":objectstore-json",
    ":objectstore-protobuf",
    ":objectstore-secure",
)