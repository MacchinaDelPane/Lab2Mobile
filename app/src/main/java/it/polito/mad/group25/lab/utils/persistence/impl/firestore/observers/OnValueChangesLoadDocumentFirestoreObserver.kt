package it.polito.mad.group25.lab.utils.persistence.impl.firestore.observers

import androidx.lifecycle.LiveData
import com.google.firebase.firestore.DocumentSnapshot
import it.polito.mad.group25.lab.utils.datastructure.Identifiable
import it.polito.mad.group25.lab.utils.persistence.LiveDataPersistenceObserver
import it.polito.mad.group25.lab.utils.persistence.PersistenceObserver
import it.polito.mad.group25.lab.utils.persistence.PersistorAware
import it.polito.mad.group25.lab.utils.persistence.impl.firestore.FirestoreLivePersistenceObserver
import it.polito.mad.group25.lab.utils.persistence.impl.firestore.FirestoreLivePersistorDelegate

class OnValueChangesLoadDocumentFirestoreObserver<T : Identifiable?>
    : PersistenceObserver<T>,
    FirestoreLivePersistenceObserver<DocumentSnapshot, T>,
    PersistorAware<T, Any?, FirestoreLivePersistorDelegate<T, Any?>> {

    override lateinit var persistor: FirestoreLivePersistorDelegate<T, Any?>

    override fun beforeValueChanges(oldValue: T, newValue: T): T? =
        super.beforeValueChanges(oldValue, newValue)
            ?.apply { this.id?.let { persistor.loadAnotherDocument(it) } }

    override fun beforePerformingPersistence(value: T): T? = null //do not persist

}

class OnLiveDataValueChangesLoadDocumentFirestoreObserver<T : Identifiable?, L : LiveData<T>>
    : PersistenceObserver<L>,
    FirestoreLivePersistenceObserver<DocumentSnapshot, L>,
    LiveDataPersistenceObserver<T>,
    PersistorAware<L, Any?, FirestoreLivePersistorDelegate<L, Any?>> {

    override lateinit var persistor: FirestoreLivePersistorDelegate<L, Any?>

    override fun beforeValueChanges(oldValue: L, newValue: L): L? {
        newValue.value?.id?.let { persistor.loadAnotherDocument(it) }
        newValue.observeForever {
            if (it?.id != null) {
                persistor.loadAnotherDocument(it.id!!)
            }
        }
        return newValue
    }

    override fun beforePerformingPersistence(value: L): L? = null //don't persist
    override fun beforePerformingLiveValuePersistency(value: T): T? = null //do not persist

}