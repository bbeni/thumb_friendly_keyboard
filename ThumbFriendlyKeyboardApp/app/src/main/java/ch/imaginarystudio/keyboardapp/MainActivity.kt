package ch.imaginarystudio.keyboardapp

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.imaginarystudio.keyboardapp.ui.theme.KeyboardAppTheme
import ch.imaginarystudio.keyboardapp.ui.theme.KeyboardColorThemes
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import splitties.systemservices.inputMethodManager

/*
    Goals:
        -[x] Multiple Pages of keyboard: Letters, Numbers, ...
        -[ ] Save keyboards persistently
        -[ ] Predefined Grid/Patterns for placing keys
        -[x] Have a good default Keyboard
        -[ ] Languages
        -[x] UI to select keyboard when app opened

    Bugs:
        -[ ] Keyboard crash on landscape
        -[x] App crash on select other IME
        -[x] Handle return/newline better
        -[x] Handle long press
        -[ ] Crash in editing keyboard view
        -[ ] Option Construct Keyboard does not remember state
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KeyboardAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainView()
                }
            }
        }
    }
}

@Composable
fun MainView() {

    val themeIndex by LocalContext.current.keyboardPreferencesStore.data.map {
        it[ACTIVE_THEME_KEY]?:0
    }.collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .padding(0.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        TitleView()
        MyText(text = "")
        MyText(text = stringResource(id = R.string.welcome_select_theme))
        ThemeSelection(themeIndex)
        MyText(text = stringResource(id = R.string.welcome_select_keyboard))
        KeyboardSelection(themeIndex=themeIndex)
        MyText(text = stringResource(id = R.string.welcome_hints))
        Column (
            modifier = Modifier
                .padding(5.dp)
        ) {
            EnableIMEButton(stringResource(id=R.string.enable_ime_steps))
            SelectIMEButton(stringResource(id=R.string.select_ime_steps))
        }
        TestInputView()
        MyText(modifier = Modifier.padding(25.dp, 420.dp),
            text = stringResource(id = R.string.lore))
    }
}


@Composable
fun MyText(text: String, modifier: Modifier = Modifier.padding(25.dp, 2.dp)) {
    Text(
        modifier = modifier,
        text = text,
    )
}

@Composable
fun KeyboardSelection(modifier: Modifier = Modifier, themeIndex: Int) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // copy-pasta code from KeyboardIMEService
    val keyboardTheme = KeyboardTheme (
        shrinkKeyDp = 1.2.dp,
        keyPaint = Paint(),
        colorTheme = KeyboardColorThemes[themeIndex],
    )

    keyboardTheme.keyPaint.apply {
        textAlign = Paint.Align.CENTER
        textSize = 64f
        color = keyboardTheme.colorTheme.keyForeground.toArgb()
    }

    val regularPositions = regularGrid1(keyboardTheme.aspectRatio)
    val concentricPositions1 = concentricGrid1(keyboardTheme.aspectRatio)
    val concentricPositions2 = concentricGrid2(keyboardTheme.aspectRatio)

    val keyboardState = KeyboardState (
        modifierShift = remember { mutableStateOf(false) },
        modifierNumeric = remember { mutableStateOf(false) },
        mode = remember { mutableStateOf(Mode.KEYBOARD) }
    )


    val aspect = keyboardTheme.aspectRatio

    val keyboardOptions = mapOf(
        "Regular Keyboard" to makeKeyboardData(regularPositions, aspect),
        "Rings Keyboard" to makeKeyboardData(concentricPositions2, aspect),
        "Rings Keyboard 2" to  makeKeyboardData(concentricPositions1, aspect),
        "Custom Keyboard" to makeKeyboardData(emptyList(), aspect),
    )

    val selectedKeyboardKey by context.keyboardPreferencesStore.data.map {
        it[ACTIVE_KEYBOARD_KEY]?: keyboardOptions.keys.first()
    }.collectAsState(initial = keyboardOptions.keys.first())

    val onKeyboardSelected: (String) -> (Unit) = { newKeyboardKey ->
        scope.launch { preferencesUpdateActiveKeyboard(context, newKeyboardKey) }
    }

    keyboardOptions["Custom Keyboard"]?.finishedConstruction = mutableStateOf(false)

    Column(modifier.selectableGroup()) {
        keyboardOptions.keys.forEach { text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (text == selectedKeyboardKey),
                        onClick = { onKeyboardSelected(text) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (text == selectedKeyboardKey),
                    onClick = null // null recommended for accessibility with screen readers
                )
                if (text in keyboardOptions) {
                    val keyboardData = keyboardOptions.getValue(text)
                    if (keyboardData.finishedConstruction.value) {
                        KeyboardView(
                            keyboardData, keyboardState, keyboardTheme,
                            disableInput = true, scale = 0.9f
                        )
                    } else {
                        if (text == selectedKeyboardKey) {
                            KeyboardConstructView(
                                keyboardData = keyboardData,
                                keyboardState = keyboardState,
                                theme = keyboardTheme,
                                scale=0.9f
                            )
                        } else {
                            Text(
                                text = "Be wild and construct the keyboard by hand!",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}


@Composable
fun ThemeSelection(themeIndex: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val index by context.keyboardPreferencesStore.data.map { it[ACTIVE_THEME_KEY]?:0 }.collectAsState(initial = 0)

    Row() {
        Button(
            modifier = Modifier
                .padding(10.dp),
            onClick = {
                scope.launch {
                    var next = (index - 1)
                    if (next < 0) next = KeyboardColorThemes.size-1
                    preferencesUpdateActiveTheme(context, next)
                }
            }) {
            Text(
                text = "previous"
            )
        }

        Button(
            modifier = Modifier
                .padding(10.dp),
            onClick = {
                scope.launch {
                    val next = (index + 1) % KeyboardColorThemes.size
                    preferencesUpdateActiveTheme(context, next)
                }
            }) {
            Text(
                text = "next"
            )
        }

    }
}


@Composable
fun TestInputView() {

    var text by remember {
        mutableStateOf("")
    }

    OutlinedTextField(
        value = text,
        onValueChange ={ x -> text=x },
        label = { Text(text = stringResource(id = R.string.hint_to_test_keyboard))},
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .imePadding(),
        singleLine = false,
        maxLines = 4,
    )
}

@Composable
fun SwitchSettingCol(description: String, checked: MutableState<Boolean>) {
    Row (
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Switch(checked = checked.value, onCheckedChange = {checked.value = !checked.value})
        Text(text = description)
    }
}

@Composable
fun MyStartIntentButton(intent: Intent, text: String) {
    Row (
        modifier = Modifier
            .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        val ctx = LocalContext.current
        Button(
            onClick = {
                      ctx.startActivity(intent)
            },
            modifier = Modifier.padding(5.dp)
        ) {
            Text(text = text)
        }
    }
}


@Composable
fun EnableIMEButton(text: String = "Enable IME") {
    MyStartIntentButton(
        intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS),
        text = text
    )
}

@Composable
fun SelectIMEButton(text: String = "Select IME") {
    Row (
        modifier = Modifier
            .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Button(
            onClick = {
                inputMethodManager.showInputMethodPicker()
            },
            modifier = Modifier.padding(5.dp)
        ) {
            Text(text = text)
        }
    }
}

@Composable
fun DropdownSettingCol(description: String ) {
    val languages = listOf(
        "en-US",
        "de-DE",
        "de-CH",
        "en-UK"
    )
    var chosen by remember {
        mutableStateOf(0)
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    Row (
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopEnd),
        ) {
            Button(onClick = {expanded = !expanded}) {
                Text(text = languages[chosen])
            }
            DropdownMenu(expanded = expanded, onDismissRequest = {expanded = false}) {
                for (l in languages.indices) {
                    DropdownMenuItem(
                        text = {Text(languages[l])},
                        onClick = {
                            chosen=l
                            expanded = false
                        })
                }
            }
        }
        Text(text = description)
    }
}

@Composable
fun SettingsPanelView(darkMode: MutableState<Boolean>, editMode: MutableState<Boolean>) {
    Column (
        modifier=Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            EnableIMEButton()
            SelectIMEButton()
            SwitchSettingCol(description = "Editing Keyboard", editMode)
            SwitchSettingCol(description = "Dark Mode", darkMode)
            DropdownSettingCol(description = "Language")
        }
    )
}

@Composable
fun SubTitleView() {
    Text(
        stringResource(id = R.string.settings_subtitle),
        modifier = Modifier
            .padding(15.dp)
            .fillMaxSize()
            .border(1.dp, Color.DarkGray, shape = RoundedCornerShape(5.dp))
            .background(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Cyan,
                    1.0f to Color.Yellow,
                    startX = 0.0f
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(15.dp),
        fontSize = 20.sp
    )
}

@Composable
fun TitleView() {
    Row (
        modifier = Modifier
            .padding(0.dp)
            .background(colorResource(id = R.color.purple_200)),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TitleIcon()
        Text(
            text = stringResource(id = R.string.app_name),
            fontSize = 30.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
fun TitleIcon() {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "Title Logo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .padding(10.dp)
            .size(128.dp)
            .clip(CircleShape)
            .border(1.dp, Color.Gray, CircleShape))
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KeyboardAppTheme {
        MainView()
    }
}