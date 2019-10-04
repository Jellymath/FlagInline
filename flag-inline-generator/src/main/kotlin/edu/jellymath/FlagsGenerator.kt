package edu.jellymath

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class FlagsGenerator : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): Set<String> = setOf(Flags::class.java.name)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        println("process")
        roundEnv.getElementsAnnotatedWith(Flags::class.java)
            .filter { it.kind == ElementKind.ENUM }
            .forEach {

                val className = it.simpleName.toString()
                println("Processing: $className")
                val pack = processingEnv.elementUtils.getPackageOf(it).toString()
                generateClass(className, pack, it.enclosedElements.filter { it.kind == ElementKind.ENUM_CONSTANT })
            }
        return true
    }

    private fun generateClass(
        originalClassName: String,
        pack: String,
        enclosedElements: List<Element>
    ) {
        val className = "Inline$originalClassName"
        val type = ClassName(pack, className)
        val setName = "${className}Set"
        val setType = ClassName(pack, setName)
        val fileSpec = buildFile(pack, className) {
            clazz(className) {
                inline
                primaryConstructor {
                    parameter<UInt>("flag")
                }
                property<UInt>("flag") {
                    initializer("flag")
                }
                companionObject {
                    var current = 1u
                    for (el in enclosedElements) {
                        property(el.simpleName.toString(), type) {
                            initializer("$className(%L)", "${current}u")
                        }
                        current = current shl 1
                    }
                }
            }

            clazz(setName) {
                inline
                primaryConstructor {
                    parameter<UInt>("set")
                }
                property<UInt>("set") {
                    initializer("set")
                }
            }

            function("toSet") {
                receiver(type)
                returns(setType)
                addStatement("return $setName(flag)")
            }

            function("contains") {
                operator
                receiver(setType)
                parameter("value", type)
                returns<Boolean>()
                addStatement("return (set and value.flag) != 0u")
            }

            function("or") {
                infix
                receiver(type)
                parameter("other", type)
                returns(setType)
                addStatement("return $setName(flag or other.flag)")
            }

            function("or") {
                infix
                receiver(setType)
                parameter("other", type)
                returns(setType)
                addStatement("return this + other")
            }

            function("or") {
                infix
                receiver(setType)
                parameter("other", setType)
                returns(setType)
                addStatement("return this + other")
            }

            function("plus") {
                operator
                receiver(setType)
                parameter("other", type)
                returns(setType)
                addStatement("return $setName(set or other.flag)")
            }

            function("plus") {
                operator
                receiver(setType)
                parameter("other", setType)
                returns(setType)
                addStatement("return $setName(set or other.set)")
            }

            function("intersect") {
                infix
                receiver(setType)
                parameter("other", setType)
                returns(setType)
                addStatement("return $setName(set and other.set)")
            }
        }

        val kaptKotlinGeneratedDir = processingEnv.options.getValue(KAPT_KOTLIN_GENERATED_OPTION_NAME)
        fileSpec.writeTo(File(kaptKotlinGeneratedDir))
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}