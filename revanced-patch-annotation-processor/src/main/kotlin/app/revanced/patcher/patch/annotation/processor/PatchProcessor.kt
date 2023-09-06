package app.revanced.patcher.patch.annotation.processor

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

class PatchProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private fun KSAnnotated.isSubclassOf(cls: KClass<*>): Boolean {
        if (this !is KSClassDeclaration) return false

        if (qualifiedName?.asString() == cls.qualifiedName) return true

        return superTypes.any { it.resolve().declaration.isSubclassOf(cls) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val executablePatches = buildMap {
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
                            val packageVersions = (it.property("versions") as List<String>)
                                .joinToString(", ") { version -> "\"$version\"" }

                            CodeBlock.of(
                                "%T(%S, setOf(%L))",
                                app.revanced.patcher.patch.Patch.CompatiblePackage::class,
                                packageName,
                                packageVersions
                            )
                        },
                        use,
                        requiresIntegrations
                    )
                }
            }
        }

        // If a patch depends on another, that is annotated, the dependency should be replaced with the generated patch,
        // because the generated patch has all the necessary properties to invoke the super constructor,
        // unlike the annotated patch.
        val dependencyResolutionMap = buildMap {
            executablePatches.values.filter { it.dependencies != null }.flatMap {
                it.dependencies!!
            }.distinct().forEach { dependency ->
                executablePatches.keys.find { it.qualifiedName?.asString() == dependency.toString() }
                    ?.let { patch ->
                        this[dependency] = ClassName(
                            patch.packageName.asString(),
                            patch.simpleName.asString() + "Generated"
                        )
                    }
            }
        }

        executablePatches.forEach { (patchDeclaration, patchAnnotation) ->
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

                            patchAnnotation.dependencies?.let { dependencies ->
                                addSuperclassConstructorParameter(
                                    "dependencies = setOf(%L)",
                                    buildList {
                                        addAll(dependencies)
                                        // Also add the source class of the generated class so that it is also executed.
                                        add(patchDeclaration.toClassName())
                                    }.joinToString(", ") { dependency ->
                                        "${(dependencyResolutionMap[dependency] ?: dependency)}::class"
                                    }
                                )
                            }
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
                                    "%T.options.forEach { (key, option) ->",
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