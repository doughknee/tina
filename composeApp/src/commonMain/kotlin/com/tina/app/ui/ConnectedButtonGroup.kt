package com.tina.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * The M3 Expressive connected button group: equal-width [ToggleButton]s with a 2dp gap,
 * leading / middle / trailing shapes, and the checked one morphing to its checked shape.
 * Exactly one option is checked, so each button announces as a radio button.
 */
@Composable
fun ConnectedButtonGroup(
    count: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int, checked: Boolean) -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        repeat(count) { index ->
            val checked = index == selectedIndex
            ToggleButton(
                checked = checked,
                // a group is single-choice: tapping the checked one keeps it checked
                onCheckedChange = { if (it) onSelect(index) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .semantics {
                        role = Role.RadioButton
                        selected = checked
                    },
                shapes = when {
                    count == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                content(index, checked)
            }
        }
    }
}
