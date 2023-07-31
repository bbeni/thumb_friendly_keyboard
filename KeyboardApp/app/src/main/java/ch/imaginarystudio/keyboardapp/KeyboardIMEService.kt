package ch.imaginarystudio.keyboardapp

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import ch.imaginarystudio.keyboardapp.ui.theme.KeyboardAppTheme


class KeyboardIMEService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner
{

    override fun onCreateInputView(): View {
        var view = ComposeView(this)
        view.setContent {
            KeyboardAppTheme {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                    TestView()
                }
            }
        }

        window?.window?.decorView?.let {view->
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }

        var v = TextView(this)
        v.text = "asdf asdf asdf asdf asdf asdf "
        v.textSize = 30.0f
        return v
    }

    @Composable
    fun TestView() {
        Text(text = "asdf asdf asdf asd asdf ")
    }




    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    //Lifecylce Methods
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private fun handleLifecycleEvent(event: Lifecycle.Event) =
        lifecycleRegistry.handleLifecycleEvent(event)

    //ViewModelStore Methods
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = store


    //SaveStateRegestry Methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

}