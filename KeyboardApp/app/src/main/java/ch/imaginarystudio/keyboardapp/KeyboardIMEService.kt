package ch.imaginarystudio.keyboardapp

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
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

        // *insert rage comment here* because it is hard to share state between keyboard and main activity

        val keyboardTheme = KeyboardTheme (
            shrinkKeyDp = 1.dp,
            keyPaint = Paint().apply {
                textAlign = Paint.Align.CENTER
                textSize = 64f
                color = Color(198, 199, 200).toArgb()
                setShadowLayer(0.8f, 2.0f, 2.0f, Color.DarkGray.toArgb())
            }
        )

        val fillDefaultKeyInfos = { keys: List<Key> ->
            val l = mutableStateListOf<KeyInfo>()
            val positions = regularGrid1(keyboardTheme.aspectRatio)
            keys.forEachIndexed { i, k ->
                addKey(l, positions[i], k)
            }
            recalculateBoundaries(l, keyboardTheme.aspectRatio)
            l
        }

        val keyboardState = KeyboardState (
            modifierShift = remember { mutableStateOf(false) },
            modifierNumeric = remember { mutableStateOf(false) },
            showSettings = remember { mutableStateOf(false) }
        )

        val keyboardData1 = KeyboardData (
            finishedConstruction = remember { mutableStateOf(false) },
            alphaPage = remember { mutableStateListOf<KeyInfo>() },
            numericPage = remember { mutableStateListOf<KeyInfo>() }
        )

        val keyboardData = KeyboardData (
            finishedConstruction = remember { mutableStateOf(true) },
            alphaPage = remember { fillDefaultKeyInfos(keysPageAlpha) },
            numericPage = remember { fillDefaultKeyInfos(keysPageNumeric) }
        )

        if (!keyboardData.finishedConstruction.value) {
            KeyboardConstructView(keyboardData, keyboardState, keyboardTheme)
        } else {
            KeyboardView(keyboardData, keyboardState, keyboardTheme)
            //MockKeyboard()
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