package it.polito.mad.group25.lab.utils.persistence.instantiator

import android.util.Log
import androidx.lifecycle.LiveData
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JavaType
import it.polito.mad.group25.lab.utils.persistence.*
import it.polito.mad.group25.lab.utils.toJavaType
import it.polito.mad.group25.lab.utils.type
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

object PersistorInstantiator {
    val LOG_TAG = "PERSISTORS"

    // controlla se la classe che si sta gestendo appartiene ad una di quelle per cui sono state implementati
    // dei handler particolari
    private fun <T> customHandler(
        targetType: JavaType,
        id: String,
        observer: PersistenceObserver<T>,
        //alreadyHandled: MutableSet<JavaType> = mutableSetOf()
    ): AbstractPersistenceHandler<T, *>? {
        var handler: AbstractPersistenceHandler<T, *>? = null

        //if (targetType is ResolvedRecursiveType && alreadyHandled.contains(targetType.selfReferencedType)) return handler

        val targetClass = targetType.rawClass

        if (PersistenceAware::class.java.isAssignableFrom(targetClass)) {
            handler = persistenceAwareHandler(
                id, handler as AbstractPersistenceHandler<PersistenceAware, *>?
            ) as AbstractPersistenceHandler<T, *>
        }

        if (LiveData::class.java.isAssignableFrom(targetClass)) {
            handler = liveDataHandler(
                targetClass as Class<LiveData<Any?>>,
                targetType,
                id, observer as PersistenceObserver<LiveData<Any?>>,
                handler as AbstractPersistenceHandler<Any?, *>?
            ) as AbstractPersistenceHandler<T, *>
        }

        return handler
    }

    private fun <T, L : LiveData<T>> liveDataHandler(
        targetClass: Class<L>,
        targetType: JavaType,
        id: String,
        observer: PersistenceObserver<L>,
        //alreadyHandled: MutableSet<JavaType>,
        handler: AbstractPersistenceHandler<T, *>?
    ): AbstractPersistenceHandler<L, T> {
        Log.d(LOG_TAG, "Persisted type $id is a Live Data, providing custom handling.")

        var liveDataType: JavaType? = targetType

        while (liveDataType != null && liveDataType.rawClass != LiveData::class.java)
            liveDataType = liveDataType.superClass

        if (liveDataType == null)
            throw IllegalStateException("Couldn't find LiveData java type. This should never happen.")

        val innerJavaType = liveDataType.bindings.typeParameters[0]
        val innerType = innerJavaType.rawClass as Class<T>

        val innerHandler = handler ?: customHandler(
            innerJavaType, id,
            object : PersistenceObserver<T> {},
            //alreadyHandled.apply { add(targetType) },
        ) as PersistenceHandler<T>?

        if (!LiveDataPersistenceObserver::class.java.isAssignableFrom(observer::class.java)) {
            Log.e(
                LOG_TAG,
                "Persisted value is LiveData but the observer is not an instance " +
                        "of ${LiveDataPersistenceObserver::class.java.simpleName}" +
                        " ad will no handle live data changing! Consider changing your observer type."
            )
            return LiveDataPersistenceHandler(
                targetClass,
                innerType, nextHandler = innerHandler
            )
        } else {
            return LiveDataPersistenceHandler(
                targetClass, innerType,
                observer as LiveDataPersistenceObserver<T>,
                nextHandler = innerHandler
            )
        }
    }

    private fun <T : PersistenceAware> persistenceAwareHandler(
        id: String,
        handler: AbstractPersistenceHandler<T, *>?
    ): AbstractPersistenceHandler<T, T> {
        Log.d(LOG_TAG, "Persisted type $id is a Persistence Aware, providing custom handling.")
        return PersistenceAwarePersistenceHandler(handler as PersistenceHandler<T>?)
    }

    // i persistors vengono creati tramite il provider in modo da avere thisRef e Property.
    // con istanziazione diretta non vengono passati dal compilatore.
    fun <T, C, P : SimplePersistor<T, C>> createThroughProvider(
        typeReference: TypeReference<T>,
        default: T,
        id: String?,
        observer: PersistenceObserver<T>,
        targetExtractor: (KProperty<*>) -> Class<T> = { it.type().java as Class<T> },
        creator: (String, C, Class<T>, AbstractPersistenceHandler<T, *>?) -> P
    ): PropertyDelegateProvider<C, P> {
        return PropertyDelegateProvider { thisRef, property ->
            val targetClass = targetExtractor(property)
            val computedID = id ?: computeId(thisRef, property)
            val persistor = creator(
                computedID, thisRef, targetClass,
                customHandler(
                    typeReference.toJavaType(), computedID, observer
                )
            )
            persistor.observer = FieldsIntrospectorObserver(persistor.observer, persistor)
            persistor
        }
    }


    private fun <C> computeId(container: C, property: KProperty<*>): String =
        "${container!!::class.java.canonicalName}.${property.type().java.simpleName}.${property.name}"


}