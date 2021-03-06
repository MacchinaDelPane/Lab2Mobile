package it.polito.mad.group25.lab.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.type.TypeFactory
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty

fun Field.isGenericType(): Boolean = type.typeParameters.isNotEmpty()

@Suppress("UNCHECKED_CAST")
private fun <T> tryInstantiateClass(clazz: Class<T>, vararg args: Any?): T {
    if (args.size == 1 && clazz.isAssignableFrom(args[0]!!.javaClass))
        return args[0]!! as T

    if (args.size == 1 && args[0] is String) {
        val str = args[0] as String
        when (clazz.canonicalName) {
            Int::class.java.canonicalName -> return str.toInt() as T
            Short::class.java.canonicalName -> return str.toShort() as T
            Long::class.java.canonicalName -> return str.toLong() as T
            Double::class.java.canonicalName -> return str.toDouble() as T
            Float::class.java.canonicalName -> return str.toFloat() as T
            BigDecimal::class.java.canonicalName -> return str.toBigDecimal() as T
            BigInteger::class.java.canonicalName -> return str.toBigInteger() as T
            Byte::class.java.canonicalName -> return str.toByte() as T
            Boolean::class.java.canonicalName -> return str.toBoolean() as T
            CharArray::class.java.canonicalName -> return str.toCharArray() as T
            Regex::class.java.canonicalName -> return str.toRegex() as T
        }
    }

    val destinationConstructors = clazz.constructors
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val childConstructor = destinationConstructors.filter {
            it.parameterCount == args.size && it.parameterTypes.contentEquals(args.map { a -> a?.javaClass }
                .toTypedArray())
        }.map { it.isAccessible = true; it }

        if (childConstructor.isNotEmpty()) return childConstructor[0].newInstance(*args) as T
    } else {
        destinationConstructors.forEach {
            try {
                return it.newInstance(*args) as T
            } catch (ignored: Exception) {
            }
        }
    }

    throw NoSuchMethodException("Cannot instantiate ${clazz.canonicalName} from ${args.map { it.toString() }}")
}

fun <T> Class<T>.tryInstantiate(vararg args: Any?): T = tryInstantiateClass(this, *args)

fun <T : Any> KClass<T>.tryInstantiate(vararg args: Any?): T = tryInstantiateClass(this.java, *args)

fun <T : Iterable<Annotation>> T.containsAnnotation(annotationClass: KClass<out Annotation>): Boolean =
    any { annotationClass.java.isAssignableFrom(it::class.java) }

@Suppress("UNCHECKED_CAST")
fun <A : Annotation, T : Iterable<Annotation>> T.getAnnotation(annotationClass: KClass<A>): A? =
    find { annotationClass.java.isAssignableFrom(it::class.java) } as A?


fun KParameter.parameterType() = type.classifier as KClass<*>
fun <T> KProperty<T>.type() = getter.returnType.classifier as KClass<*>

fun <T : Any> Class<T>.distanceFrom(sourceClass: Class<*>): Int? {
    if (this == sourceClass) return 0
    var actualParent = sourceClass.superclass
    var i = 1
    while (actualParent != Any::class.java) {
        if (actualParent == this) return i
        i++
        actualParent = actualParent.superclass
    }
    return if (this == Any::class.java) i else null
}

fun <T : Any> KClass<T>.distanceFrom(sourceClass: KClass<*>): Int? =
    java.distanceFrom(sourceClass.java)

@RequiresApi(Build.VERSION_CODES.P)
fun <T> TypeReference<T>.genericType(): Array<Type> {
    if (this.type !is ParameterizedType)
        throw IllegalArgumentException("Given type is not a generic one!")
    return (this.type as ParameterizedType).actualTypeArguments
}

@RequiresApi(Build.VERSION_CODES.P)
fun <T> Class<T>.genericType(): Array<Type> {
    val genericSuperClass = this.genericSuperclass
    if (genericSuperClass == null || genericSuperClass !is ParameterizedType)
        throw IllegalArgumentException("Given type is not a generic one")
    return genericSuperClass.actualTypeArguments
}


fun Type.extractClass(): Class<*> = Class.forName(cleanGenericClassName(this.typeName))

private fun cleanGenericClassName(className: String) = className.let {
    if (it.contains("<"))
        it.substring(0, it.indexOf("<"))
    else it
}

fun <T> TypeReference<T>.toJavaType() = TypeFactory.defaultInstance().constructType(this)


private fun searchForFieldsRecursive(clazz: Class<*>?, destination: MutableCollection<Field>) {
    if (clazz == null) return
    destination.addAll(clazz.declaredFields)
    searchForFieldsRecursive(clazz.superclass, destination)
}

fun <T> Class<T>.getAllFields(): Array<Field> {
    val destination = mutableListOf<Field>()
    searchForFieldsRecursive(this, destination)
    return destination.toTypedArray()
}

fun Field.isStatic() = Modifier.isStatic(modifiers)