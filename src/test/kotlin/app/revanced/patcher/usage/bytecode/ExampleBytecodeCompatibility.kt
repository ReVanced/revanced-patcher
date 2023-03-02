package app.revanced.patcher.usage.bytecode

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility(
    [Package(
        "com.example.examplePackage", arrayOf("0.0.1", "0.0.2")
    )]
)
@Target(AnnotationTarget.CLASS)
internal annotation class ExampleBytecodeCompatibility

