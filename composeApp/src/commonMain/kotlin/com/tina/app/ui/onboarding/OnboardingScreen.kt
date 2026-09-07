package com.tina.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.capture.CaptureViewModel
import com.tina.app.data.ItemType
import com.tina.app.resources.Res
import com.tina.app.resources.onb_questions
import com.tina.app.resources.onb_skip
import com.tina.app.resources.onb_start
import com.tina.app.ui.CaptureFocus
import com.tina.app.ui.FirstCapture
import com.tina.app.ui.capture.CaptureChips
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * First run (design/first-run row A): a blank screen, the real capture field with its chips live,
 * and a question as the placeholder that changes every few seconds. Nothing is prefilled. Send
 * saves the capture and lands the app where it went: Plan on that day, the Need a day page, or
 * Ideas, each with one callout that says why. Skip goes straight to the app.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    // the shell's own view model: what is typed here is the same draft the bar holds
    val viewModel: CaptureViewModel = koinViewModel()
    val settings = LocalSettings.current
    val questions = stringArrayResource(Res.array.onb_questions)
    var question by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(questions.size) {
        while (true) {
            delay(4_000)
            question = (question + 1) % questions.size
        }
    }
    fun send() {
        if (viewModel.text.isBlank()) {
            CaptureFocus.request()
            onDone()
            return
        }
        val p = viewModel.effective()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val landing = when {
            p.type == ItemType.NOTE -> FirstCapture.Landing.Ideas
            p.type == ItemType.EVENT || p.date != null || p.rrule != null -> FirstCapture.Landing.Plan(p.date ?: today, p.time)
            settings.undatedToSort -> FirstCapture.Landing.NeedADay
            else -> FirstCapture.Landing.Plan(today, null)
        }
        viewModel.save {
            FirstCapture.request(landing)
            onDone()
        }
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text(stringResource(Res.string.onb_skip)) }
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            CaptureChips(viewModel, Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = viewModel.fieldValue,
                onValueChange = viewModel::onFieldChange,
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = {
                    AnimatedContent(questions[question], transitionSpec = { fadeIn() togetherWith fadeOut() }) { Text(it) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = ::send) { Text(stringResource(Res.string.onb_start)) }
        }
    }
}
