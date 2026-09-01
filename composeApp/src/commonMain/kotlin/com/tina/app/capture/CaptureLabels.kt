package com.tina.app.capture

import androidx.compose.runtime.Composable
import com.tina.app.data.ItemType
import com.tina.app.resources.Res
import com.tina.app.resources.type_event
import com.tina.app.resources.type_inbox
import com.tina.app.resources.type_note
import com.tina.app.resources.type_task
import org.jetbrains.compose.resources.stringResource

@Composable
fun typeLabel(type: ItemType): String = when (type) {
    ItemType.INBOX -> stringResource(Res.string.type_inbox)
    ItemType.TASK -> stringResource(Res.string.type_task)
    ItemType.EVENT -> stringResource(Res.string.type_event)
    ItemType.NOTE -> stringResource(Res.string.type_note)
}
