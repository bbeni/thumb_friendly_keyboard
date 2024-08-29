package ch.imaginarystudio.keyboardapp

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.AbstractComposeView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

// At the top level of your kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
object DS {
   val edit_mode = booleanPreferencesKey("edit_mode")
}

class KeyboardIMEService : LifecycleInputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner
{

    override fun onCreateInputView(): View {
        /*var view = ComposeView(this)
        view.setContent {
            KeyboardAppTheme {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                    TestView()
                }
            }
        }*/

        var v = ComposeKeyboardView(this)

        window?.window?.decorView?.let {view->
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }

        return v
    }


    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    //Lifecycle Methods
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private fun handleLifecycleEvent(event: Lifecycle.Event) =
        lifecycleRegistry.handleLifecycleEvent(event)

    //ViewModelStore Methods
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = store


    //SaveStateRegistry Methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

}

class ComposeKeyboardView(context: Context) : AbstractComposeView(context) {
    @Composable
    override fun Content() {

        // *insert rage comment here*

        /*val edit_mode by context.dataStore.data.map {
            it[DS.edit_mode] ?: false
        }.collectAsState(initial = false)
        */

        val edit_mode = remember {
            mutableStateOf(true)
        }

        if (edit_mode.value) {
            KeyboardConstructView()
        } else {
            MockKeyboard()
        }
    }
}


// Copy pasta from https://github.com/THEAccess/compose-keyboard-ime
abstract class LifecycleInputMethodService : InputMethodService(), LifecycleOwner {

    protected val dispatcher = ServiceLifecycleDispatcher(this)

    @CallSuper
    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    override fun onBindInput() {
        super.onBindInput()
        dispatcher.onServicePreSuperOnBind()
    }


    // this method is added only to annotate it with @CallSuper.
    // In usual service super.onStartCommand is no-op, but in LifecycleService
    // it results in mDispatcher.onServicePreSuperOnStart() call, because
    // super.onStartCommand calls onStart().
    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

}