package ch.imaginarystudio.keyboardapp
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.keyboardPreferencesStore by preferencesDataStore(
    name = "keyboard_preferences"
)

val ACTIVE_THEME_KEY = intPreferencesKey("active_theme")
val ACTIVE_KEYBOARD_KEY = stringPreferencesKey("active_keyboard")

suspend fun preferencesUpdateActiveTheme(context: Context, value: Int) {
    context.keyboardPreferencesStore.edit { settings ->
        settings[ACTIVE_THEME_KEY] = value
    }
}

suspend fun preferencesUpdateActiveKeyboard(context: Context, value: String) {
    context.keyboardPreferencesStore.edit { settings ->
        settings[ACTIVE_KEYBOARD_KEY] = value
    }
}
