package com.tina.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tina.app.capture.CaptureViewModel
import com.tina.app.resources.Res
import com.tina.app.resources.onb_capture_body
import com.tina.app.resources.onb_capture_example
import com.tina.app.resources.onb_capture_title
import com.tina.app.resources.onb_skip
import com.tina.app.resources.onb_start
import com.tina.app.ui.CaptureFocus
import com.tina.app.ui.capture.CaptureChips
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * One screen on first launch: the real capture field, prefilled with an example, its chips
 * live. Whatever is typed here is still in the bar when the screen goes, keyboard up, so the
 * first capture is one tap from the start button. Sort and reminders explain themselves in
 * context: Sort the first time something has no date, reminders the first time one needs to ring.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    // the shell's own view model: the text carries over instead of being retyped
    val viewModel: CaptureViewModel = koinViewModel()
    val example = stringResource(Res.string.onb_capture_example)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (viewModel.text.isBlank()) viewModel.prefill(example)
        focus.requestFocus()
    }
    fun start() {
        CaptureFocus.request()
        onDone()
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                // untouched example: leave the bar the way they found it
                if (viewModel.text == example) viewModel.discard()
                onDone()
            }) { Text(stringResource(Res.string.onb_skip)) }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(Res.string.onb_capture_title),
                style = MaterialTheme.typography.headlineMediumEmphasized,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(Res.string.onb_capture_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp),
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            CaptureChips(viewModel, Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = viewModel.fieldValue,
                onValueChange = viewModel::onFieldChange,
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { start() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = ::start) { Text(stringResource(Res.string.onb_start)) }
        }
    }
}
