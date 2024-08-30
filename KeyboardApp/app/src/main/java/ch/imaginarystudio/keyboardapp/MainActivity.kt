package ch.imaginarystudio.keyboardapp

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.imaginarystudio.keyboardapp.ui.theme.KeyboardAppTheme
import splitties.systemservices.inputMethodManager


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
fun MyText(text: String) {
    Text(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp, 0.dp),
        text = text
    )
}

@Composable
fun MainView() {
    val darkMode = remember { mutableStateOf(false) }
    val editMode = remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .padding(0.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        TitleView()
        MyText(text = "Hello!  To start press 'Enable the Keyboard' then switch 'Thumb Friendly Keyboard' on and come back")
        EnableIMEButton("Enable the Keyboard")
        MyText(text = "Now it is time to 'Select the Keyboard'")
        SelectIMEButton("Select the Keyboard")
        MyText(text =
            "Click on the test text field below. " +
            "An empty space will appear with a red text on top telling you which Key is next. " +
            "One by one place each key where you like it. " +
            "When all keys are done, indicated when the red bar disappears, the keyboard is ready to use! " +
            "Upon phone restart you might have to do this again..."
        )
        TestInputView()
        SubTitleView()
        SettingsPanelView(darkMode, editMode)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestInputView() {

    var text by remember {
        mutableStateOf("")
    }

    OutlinedTextField(
        value = text,
        onValueChange = {x -> text=x},
        label = { Text(text = stringResource(id = R.string.hint_to_test_keyboard))},
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        singleLine = false,
        maxLines = 4
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
fun EnableIMEButton(text: String = "Enable IME") {
    Row (
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        val ctx = LocalContext.current
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }) {
            Text(text = text)
        }
    }
}

@Composable
fun SelectIMEButton(text: String = "Select IME") {
    Row (
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        val ctx = LocalContext.current
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            inputMethodManager.showInputMethodPicker()
        }) {
            Text(text = text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSettingCol(description: String ) {
    val languages = listOf<String>(
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
fun SettingsPanelView(dark_mode: MutableState<Boolean>, edit_mode: MutableState<Boolean>  ) {
    val list = (1..10).map { it.toString() }
    Column (
        modifier=Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            EnableIMEButton()
            SelectIMEButton()
            SwitchSettingCol(description = "Editing Keyboard", edit_mode)
            SwitchSettingCol(description = "Dark Mode", dark_mode)
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