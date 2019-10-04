package edu.jellymath

import com.squareup.kotlinpoet.*


fun buildFile(pack: String, fileName: String, action: FileSpec.Builder.() -> Unit): FileSpec =
    FileSpec.builder(pack, fileName).apply(action).build()

fun buildCompanionObject(name: String? = null, action: TypeSpec.Builder.() -> Unit): TypeSpec =
    TypeSpec.companionObjectBuilder(name).apply(action).build()

fun buildProperty(name: String, type: TypeName, action: PropertySpec.Builder.() -> Unit): PropertySpec =
    PropertySpec.builder(name, type).apply(action).build()

inline fun <reified T> buildProperty(name: String, action: PropertySpec.Builder.() -> Unit): PropertySpec =
    PropertySpec.builder(name, T::class).apply(action).build()

fun buildClass(className: String, action: TypeSpec.Builder.() -> Unit): TypeSpec =
    TypeSpec.classBuilder(className).apply(action).build()

fun buildConstructor(action: FunSpec.Builder.() -> Unit): FunSpec =
    FunSpec.constructorBuilder().apply(action).build()

fun buildFunction(name: String, action: FunSpec.Builder.() -> Unit): FunSpec =
    FunSpec.builder(name).apply(action).build()

fun TypeSpec.Builder.companionObject(name: String? = null, action: TypeSpec.Builder.() -> Unit = {}): TypeSpec.Builder =
    addType(buildCompanionObject(name, action))

fun TypeSpec.Builder.property(name: String, type: TypeName, action: PropertySpec.Builder.() -> Unit): TypeSpec.Builder =
    addProperty(buildProperty(name, type, action))

fun FileSpec.Builder.clazz(className: String, action: TypeSpec.Builder.() -> Unit): FileSpec.Builder =
    addType(buildClass(className, action))

fun FileSpec.Builder.function(name: String, action: FunSpec.Builder.() -> Unit): FileSpec.Builder =
    addFunction(buildFunction(name, action))

fun TypeSpec.Builder.primaryConstructor(action: FunSpec.Builder.() -> Unit): TypeSpec.Builder =
    primaryConstructor(buildConstructor(action))

fun FunSpec.Builder.parameter(name: String, type: TypeName): FunSpec.Builder = addParameter(name, type)

inline fun <reified T> FunSpec.Builder.parameter(name: String): FunSpec.Builder = addParameter(name, T::class)

inline fun <reified T> FunSpec.Builder.receiver(): FunSpec.Builder = receiver(T::class)

inline fun <reified T> FunSpec.Builder.returns(): FunSpec.Builder = returns(T::class)

inline fun <reified T> TypeSpec.Builder.property(
    name: String,
    action: PropertySpec.Builder.() -> Unit
): TypeSpec.Builder =
    addProperty(buildProperty<T>(name, action))

val TypeSpec.Builder.inline: TypeSpec.Builder get() = addModifiers(KModifier.INLINE)

val FunSpec.Builder.infix: FunSpec.Builder get() = addModifiers(KModifier.INFIX)

val FunSpec.Builder.operator: FunSpec.Builder get() = addModifiers(KModifier.OPERATOR)