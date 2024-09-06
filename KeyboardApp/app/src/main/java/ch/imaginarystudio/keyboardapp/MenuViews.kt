package ch.imaginarystudio.keyboardapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun SelectKeyboardGroup(selectedOption: MutableState<String>, options: Map<String, KeyboardData>) {

    val onSelectionChange = { text: String ->
        selectedOption.value = text
    }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { it ->
            val text = it.key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        all = 6.dp,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (it.value.finishedConstruction.value) {"ready!"} else {"not rdy"},
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
                        .padding(
                            vertical = 8.dp,
                            horizontal = 10.dp,
                        ),
                )
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
                            if (text == selectedOption.value) {
                                Color.Magenta
                            } else {
                                Color.LightGray
                            }
                        )
                        .padding(
                            vertical = 8.dp,
                            horizontal = 10.dp,
                        ),
                )
            }
        }
    }
}

@Composable
fun MenuView(selected: MutableState<String> , options: Map<String, KeyboardData>, keyboardData: KeyboardData, state: KeyboardState, theme: KeyboardTheme) {
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
                    state.showSettings.value = false
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
            SelectKeyboardGroup(selectedOption = selected, options = options)
        }
    }
}
