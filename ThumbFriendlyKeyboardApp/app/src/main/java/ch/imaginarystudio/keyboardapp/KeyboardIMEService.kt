package ch.imaginarystudio.keyboardapp

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import ch.imaginarystudio.keyboardapp.ui.theme.KeyboardColorThemes
import kotlinx.coroutines.flow.map


class KeyboardIMEService : LifecycleInputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner
{

    override fun onCreateInputView(): View {
        return ComposeKeyboardView(this)
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)

        window?.window?.decorView?.let {view->
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
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

@Composable
fun makeKeyboardData(positions: List<Vec2>, aspectRatio: Float): KeyboardData {

    val fillKeyInfos = { keys: List<Key>, positions: List<Vec2> ->
        val l = mutableStateListOf<KeyInfo>()
        positions.forEachIndexed { i, pos ->
            if (i < keys.count()) {
                addKey(l, pos, keys[i])
            } else {
                addKey(l, pos, Key(" "))
            }
        }
        recalculateBoundaries(l, aspectRatio)
        l
    }

    return KeyboardData(
        finishedConstruction = remember { mutableStateOf(true) },
        alphaPage = remember { fillKeyInfos(keysPageAlpha, positions) },
        numericPage = remember { fillKeyInfos(keysPageNumeric, positions) },
    )
}

class ComposeKeyboardView(context: Context) : AbstractComposeView(context) {
    @Composable
    override fun Content() {

        val activeThemeIndex by LocalContext.current.keyboardPreferencesStore.data.map {
            it[ACTIVE_THEME_KEY]?:0
        }.collectAsState(initial = 0)

        val keyboardTheme = KeyboardTheme (
            shrinkKeyDp = 1.2.dp,
            keyPaint = Paint(),
            colorTheme = KeyboardColorThemes[activeThemeIndex],
        )

        keyboardTheme.keyPaint.apply {
            textAlign = Paint.Align.CENTER
            textSize = 64f
            color = keyboardTheme.colorTheme.keyForeground.toArgb()
        }

        val regularPositions = regularGrid1(keyboardTheme.aspectRatio)
        val concentricPositions1 = concentricGrid1(keyboardTheme.aspectRatio)
        val concentricPositions2 = concentricGrid2(keyboardTheme.aspectRatio)

        // TODO: cleanup messy string literals as used keys
        val aspect = keyboardTheme.aspectRatio
        val keyboardOptions = mapOf(
            "Regular Keyboard" to makeKeyboardData(regularPositions, aspect),
            "Rings Keyboard" to makeKeyboardData(concentricPositions2, aspect),
            "Rings Keyboard 2" to  makeKeyboardData(concentricPositions1, aspect),
            "Custom Keyboard" to makeKeyboardData(emptyList(), aspect),
        )
        val activeKeyboardKey by LocalContext.current.keyboardPreferencesStore.data.map {
            it[ACTIVE_KEYBOARD_KEY]?:keyboardOptions.keys.first()
        }.collectAsState(initial = keyboardOptions.keys.first())

        val keyboardState = KeyboardState (
            modifierShift = remember { mutableStateOf(false) },
            modifierNumeric = remember { mutableStateOf(false) },
            mode = remember { mutableStateOf(Mode.KEYBOARD) }
        )

        if (activeKeyboardKey in keyboardOptions) {
            val keyboardData = keyboardOptions.getValue(activeKeyboardKey)

            if (!keyboardData.finishedConstruction.value) {
                KeyboardConstructView(keyboardData, keyboardState, keyboardTheme, 0.9f)
            } else {
                if (keyboardState.mode.value == Mode.MENU) {
                    MenuView(activeKeyboardKey, keyboardOptions, keyboardData, keyboardState, keyboardTheme)
                } else if (keyboardState.mode.value == Mode.KEYBOARD) {
                    KeyboardView(keyboardData, keyboardState, keyboardTheme)
                } else if (keyboardState.mode.value == Mode.MOVE_MODIFYING) {
                    KeyboardMoveEditorView(keyboardData, keyboardState, keyboardTheme)
                }
            }
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