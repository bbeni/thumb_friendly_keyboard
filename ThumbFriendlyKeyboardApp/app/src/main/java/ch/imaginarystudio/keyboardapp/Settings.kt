package ch.imaginarystudio.keyboardapp
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.keyboardPreferencesStore by preferencesDataStore(
    name = "keyboard_preferences"
)

val ACTIVE_THEME_KEY = intPreferencesKey("active_theme")

suspend fun preferencesUpdateActiveTheme(context: Context, value: Int) {
    context.keyboardPreferencesStore.edit { settings ->
        settings[ACTIVE_THEME_KEY] = value
    }
}