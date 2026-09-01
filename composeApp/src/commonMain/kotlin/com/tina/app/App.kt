package com.tina.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.tina.app.db.NoteDao
import org.koin.compose.koinInject

data object HomeRoute

@Composable
fun App() {
    AppTheme {
        val backStack = remember { mutableStateListOf<Any>(HomeRoute) }
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<HomeRoute> { HomeScreen() }
            },
        )
    }
}

@Composable
private fun HomeScreen(dao: NoteDao = koinInject()) {
    val notes by dao.all().collectAsState(initial = emptyList())
    Scaffold { padding ->
        Text(
            text = "tina — ${notes.size} notes",
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        )
    }
}
