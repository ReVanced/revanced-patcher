package app.revanced.patcher.patch.annotation.processor

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

class PatchProcessor internal constructor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private fun KSAnnotated.isSubclassOf(cls: KClass<*>): Boolean {
        if (this !is KSClassDeclaration) return false

        if (qualifiedName?.asString() == cls.qualifiedName) return true

        return superTypes.any { it.resolve().declaration.isSubclassOf(cls) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val patches = buildMap {
            resolver.getSymbolsWithAnnotation(Patch::class.qualifiedName!!).filter {
                // Do not check here if Patch is super of the class, because it is expensive.
                // Check it later when processing.
                it.validate() && it.isSubclassOf(app.revanced.patcher.patch.Patch::class)
            }.map {
                it as KSClassDeclaration
            }.forEach { patchDeclaration ->
                patchDeclaration.annotations.find {
                    it.annotationType.resolve().declaration.qualifiedName!!.asString() == Patch::class.qualifiedName!!
                }?.let { annotation ->
                    fun KSAnnotation.property(name: String) =
                        arguments.find { it.name!!.asString() == name }?.value!!

                    val name =
                        annotation.property("name").toString().ifEmpty { null }

                    val description =
                        annotation.property("description").toString().ifEmpty { null }

                    val dependencies =
                        (annotation.property("dependencies") as List<KSType>).ifEmpty { null }

                    val compatiblePackages =
                        (annotation.property("compatiblePackages") as List<KSAnnotation>).ifEmpty { null }

                    val use =
                        annotation.property("use") as Boolean

                    val requiresIntegrations =
                        annotation.property("requiresIntegrations") as Boolean

                    // Data class for KotlinPoet
                    data class PatchData(
                        val name: String?,
                        val description: String?,
                        val dependencies: List<ClassName>?,
                        val compatiblePackages: List<CodeBlock>?,
                        val use: Boolean,
                        val requiresIntegrations: Boolean
                    )

                    this[patchDeclaration] = PatchData(
                        name,
                        description,
                        dependencies?.map { dependency -> dependency.toClassName() },
                        compatiblePackages?.map {
                            val packageName = it.property("name")
                            val packageVersions = (it.property("versions") as List<String>).ifEmpty { null }
                                ?.joinToString(", ") { version -> "\"$version\"" }

                            CodeBlock.of(
                                "%T(%S, %L)",
                                app.revanced.patcher.patch.Patch.CompatiblePackage::class,
                                packageName,
                                packageVersions?.let { "setOf($packageVersions)" },
                            )
                        },
                        use,
                        requiresIntegrations
                    )
                }
            }
        }

        // If a patch depends on another, that is annotated, the dependency should be replaced with the generated patch,
        // because the generated patch has all the necessary properties to invoke the super constructor with,
        // unlike the annotated patch.
        val dependencyResolutionMap = buildMap {
            patches.values.filter { it.dependencies != null }.flatMap {
                it.dependencies!!
            }.distinct().forEach { dependency ->
                patches.keys.find { it.qualifiedName?.asString() == dependency.toString() }
                    ?.let { patch ->
                        this[dependency] = ClassName(
                            patch.packageName.asString(),
                            patch.simpleName.asString() + "Generated"
                        )
                    }
            }
        }

        patches.forEach { (patchDeclaration, patchAnnotation) ->
            val isBytecodePatch = patchDeclaration.isSubclassOf(BytecodePatch::class)

            val superClass = if (isBytecodePatch) {
                BytecodePatch::class
            } else {
                ResourcePatch::class
            }

            val contextClass = if (isBytecodePatch) {
                BytecodeContext::class
            } else {
                ResourceContext::class
            }

            val generatedPatchClassName = ClassName(
                patchDeclaration.packageName.asString(),
                patchDeclaration.simpleName.asString() + "Generated"
            )

            FileSpec.builder(generatedPatchClassName)
                .addType(
                    TypeSpec.objectBuilder(generatedPatchClassName)
                        .superclass(superClass).apply {
                            patchAnnotation.name?.let { name ->
                                addSuperclassConstructorParameter("name = %S", name)
                            }

                            patchAnnotation.description?.let { description ->
                                addSuperclassConstructorParameter("description = %S", description)
                            }

                            patchAnnotation.compatiblePackages?.let { compatiblePackages ->
                                addSuperclassConstructorParameter(
                                    "compatiblePackages = setOf(%L)",
                                    compatiblePackages.joinToString(", ")
                                )
                            }

                            // The generated patch always depends on the source patch.
                            addSuperclassConstructorParameter(
                                "dependencies = setOf(%L)",
                                buildList {
                                    patchAnnotation.dependencies?.forEach { dependency ->
                                        add("${(dependencyResolutionMap[dependency] ?: dependency)}::class")
                                    }

                                    add("${patchDeclaration.toClassName()}::class")
                                }.joinToString(", "),
                            )

                            addSuperclassConstructorParameter(
                                "use = %L", patchAnnotation.use
                            )

                            addSuperclassConstructorParameter(
                                "requiresIntegrations = %L",
                                patchAnnotation.requiresIntegrations
                            )
                        }
                        .addFunction(
                            FunSpec.builder("execute")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter("context", contextClass)
                                .build()
                        )
                        .addInitializerBlock(
                            CodeBlock.builder()
                                .add(
                                    "%T.options.forEach { (_, option) ->",
                                    patchDeclaration.toClassName()
                                )
                                .addStatement(
                                    "options.register(option)"
                                )
                                .add(
                                    "}"
                                )
                                .build()
                        )
                        .build()
                ).build().writeTo(
                    codeGenerator,
                    Dependencies(false, patchDeclaration.containingFile!!)
                )
        }

        return emptyList()
    }
}