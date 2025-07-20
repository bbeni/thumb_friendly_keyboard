package ch.imaginarystudio.keyboardapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch


@Composable
fun SelectKeyboardGroup(keyboardState: KeyboardState, selectedOption: String, options: Map<String, KeyboardData>) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()    

    val onSelectionChange = { text: String ->
        scope.launch {
            preferencesUpdateActiveKeyboard(context, text)
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        options.forEach {
            val text = it.key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        all = 3.dp,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row() {
                    Text(
                        text = "Move Keys",
                        style = MaterialTheme.typography.bodyMedium.merge(),
                        color = Color.White,
                        modifier = Modifier
                            .clip(
                                shape = RoundedCornerShape(
                                    size = 12.dp
                                )
                            )
                            .clickable {
                                onSelectionChange(text)
                                keyboardState.mode.value = Mode.MOVE_MODIFYING
                            }
                            .background(
                                if (text == selectedOption) {
                                    Color.DarkGray
                                } else {
                                    Color.LightGray
                                }
                            )
                            .padding(6.dp)
                    )
                    Text(
                        text = if (it.value.finishedConstruction.value) {"rdy"} else {"not"},
                        style = MaterialTheme.typography.bodyMedium.merge(),
                        color = Color.White,
                        modifier = Modifier
                            .clip(
                                shape = RoundedCornerShape(
                                    size = 12.dp,
                                ),
                            )
                            .clickable {
                                onSelectionChange(text)
                            }
                            .background(
                                if (it.value.finishedConstruction.value) {
                                    Color.Green
                                } else {
                                    Color.Red
                                }
                            )
                            .padding(6.dp),
                    )}
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.merge(),
                    color = Color.White,
                    modifier = Modifier
                        .clip(
                            shape = RoundedCornerShape(
                                size = 12.dp,
                            ),
                        )
                        .clickable {
                            onSelectionChange(text)
                        }
                        .background(
                            if (text == selectedOption) {
                                Color.Magenta
                            } else {
                                Color.LightGray
                            }
                        )
                        .padding(6.dp),
                )
            }
        }
    }
}

@Composable
fun MenuView(selected: String, options: Map<String, KeyboardData>, keyboardData: KeyboardData, state: KeyboardState, theme: KeyboardTheme) {
    Column(
        modifier = Modifier
            .aspectRatio(theme.aspectRatio)
            .fillMaxWidth()
            .background(Color.Gray),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            SelectIMEButton("Change Android Keyboard")
            Button(
                modifier = Modifier.padding(10.dp),
                onClick = {
                    state.mode.value = Mode.KEYBOARD
                }
            ) {
                    Text(text = "back")
            }
        }
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text="Available Keyboards",
                modifier = Modifier
                    .padding(10.dp, 3.dp),
                fontSize = 25.sp,
            )
        }

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SelectKeyboardGroup(state, selected, options)
        }
    }
}
