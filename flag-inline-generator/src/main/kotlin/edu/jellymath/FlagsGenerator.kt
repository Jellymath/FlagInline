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

private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

@AutoService(Processor::class)
class FlagsGenerator : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): Set<String> = setOf(name<Flags>())
    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith<Flags>()
            .filter { it.kind == ElementKind.ENUM }
            .forEach {
                val className = it.simpleName.toString()
                val pack = processingEnv.elementUtils.getPackageOf(it).toString()
                generateFile(className, pack, it.enclosedElements.filter { it.kind == ElementKind.ENUM_CONSTANT })
            }
        return true
    }

    private fun generateFile(
        originalClassName: String,
        packageName: String,
        enclosedElements: List<Element>
    ) {
        val name = "Inline$originalClassName"
        val type = ClassName(packageName, name)
        val setName = "${name}Set"
        val setType = ClassName(packageName, setName)
        val allValuesMask = generateSequence(1u) { (it shl 1) + 1u }.elementAt(enclosedElements.lastIndex)

        val fileSpec = buildFile(packageName, name) {
            mainClass(name, type, enclosedElements)
            setClass(setName)

            creationFunctions(packageName, type, setName, setType, allValuesMask)
            checkFunctions(type, setType, allValuesMask)
            bitwiseFunctions(type, setName, setType, allValuesMask)
            arithmeticFunctions(type, setName, setType)
        }

        val kaptKotlinGeneratedDir = processingEnv.options.getValue(KAPT_KOTLIN_GENERATED_OPTION_NAME)
        fileSpec.writeTo(File(kaptKotlinGeneratedDir))
    }

    private fun FileSpec.Builder.mainClass(
        name: String,
        type: ClassName,
        enclosedElements: List<Element>
    ) {
        clazz(name) {
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
                        initializer("$name(${current.hexLiteral()})")
                    }
                    current = current shl 1
                }
            }
        }
    }

    private fun FileSpec.Builder.setClass(name: String) {
        clazz(name) {
            inline
            primaryConstructor {
                parameter<UInt>("set")
            }
            property<UInt>("set") {
                initializer("set")
            }
            companionObject()
        }
    }

    private fun FileSpec.Builder.creationFunctions(
        packageName: String,
        type: ClassName,
        setName: String,
        setType: ClassName,
        allValuesMask: UInt
    ) {
        val companionType = ClassName(packageName, "$setName.Companion")

        function("toSet") {
            receiver(type)
            returns(setType)
            addStatement("return $setName(flag)")
        }

        function("emptySet") {
            receiver(companionType)
            returns(setType)
            addStatement("return $setName(0u)")
        }

        function("allValues") {
            receiver(companionType)
            returns(setType)
            addStatement("return $setName(${allValuesMask.hexLiteral()})")
        }
    }

    private fun FileSpec.Builder.checkFunctions(
        type: ClassName,
        setType: ClassName,
        allValuesMask: UInt
    ) {
        function("contains") {
            operator
            receiver(setType)
            parameter("value", type)
            returns<Boolean>()
            addStatement("return (set and value.flag) != 0u")
        }

        function("containsAll") {
            receiver(setType)
            parameter("values", setType)
            returns<Boolean>()
            addStatement("return (values - this).isEmpty()")
        }

        function("isEmpty") {
            receiver(setType)
            returns<Boolean>()
            addStatement("return set == 0u")
        }

        function("containsAllElements") {
            receiver(setType)
            returns<Boolean>()
            addStatement("return set == ${allValuesMask.hexLiteral()}")
        }
    }

    private fun FileSpec.Builder.bitwiseFunctions(
        type: ClassName,
        setName: String,
        setType: ClassName,
        allValuesMask: UInt
    ) {
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
            addStatement("return $setName(set or other.flag)")
        }

        function("or") {
            infix
            receiver(setType)
            parameter("other", setType)
            returns(setType)
            addStatement("return $setName(set or other.set)")
        }

        function("xor") {
            infix
            receiver(setType)
            parameter("other", type)
            returns(setType)
            addStatement("return $setName((set xor other.flag) and ${allValuesMask.hexLiteral()})")
        }

        function("and") {
            infix
            receiver(setType)
            parameter("other", setType)
            returns(setType)
            addStatement("return $setName(set and other.set)")
        }

        function("negate") {
            receiver(setType)
            returns(setType)
            addStatement("return $setName((set.inv()) and ${allValuesMask.hexLiteral()})")
        }
    }

    private fun FileSpec.Builder.arithmeticFunctions(
        type: ClassName,
        setName: String,
        setType: ClassName
    ) {
        function("plus") {
            operator
            receiver(setType)
            parameter("other", type)
            returns(setType)
            addStatement("return this.or(other)")
        }

        function("plus") {
            operator
            receiver(setType)
            parameter("other", setType)
            returns(setType)
            addStatement("return this.or(other)")
        }

        function("minus") {
            operator
            receiver(setType)
            parameter("other", setType)
            returns(setType)
            addStatement("return $setName((set or other.set) - other.set)")
        }

        function("minus") {
            operator
            receiver(setType)
            parameter("other", type)
            returns(setType)
            addStatement("return $setName((set or other.flag) - other.flag)")
        }
    }

    private fun UInt.hexLiteral(): String = "0x${toString(16)}u"
    private inline fun <reified T> name() = T::class.java.name
    private inline fun <reified T : Annotation> RoundEnvironment.getElementsAnnotatedWith() =
        getElementsAnnotatedWith(T::class.java)
}