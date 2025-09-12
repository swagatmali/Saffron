package org.saffron

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

@Serializable
data class User(
    val srNo: String,
    val receiptDate: String,
    val receiptNo: String,
    val flatNo: String,
    val amt: String,
    val transactionId: String,
    val purpose: String,
    val expiryDate: String,
)

@Serializable
data class UserResponse(
    val user: List<User>
)

class UserApiService {
    companion object {
        private const val BASE_URL =
            "https://script.google.com/macros/s/AKfycbxjKDs5WypHLqj3XG6l6vsbqaOo_hWUT75SpvxhaC9glQvz7YDsEqUmelvcDs04cQ4lmg/exec"
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private suspend fun fetchUsers(action: String): List<User> {
        val responseText: String =
            client.get("$BASE_URL?action=$action")
                .body()
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val response: UserResponse = json.decodeFromString(responseText)
        return response.user
    }

    suspend fun fetchActiveUsers(): List<User> = fetchUsers("getActive")

    suspend fun fetchAllUsers(): List<User> = fetchUsers("getAll")
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        var activeUsers by remember { mutableStateOf<List<User>>(emptyList()) }
        var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var isRefreshing by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var activeSearchQuery by remember { mutableStateOf("") }
        var allSearchQuery by remember { mutableStateOf("") }
        var selectedTab by remember { mutableStateOf(0) }
        val scope = rememberCoroutineScope()
        val userApiService = remember { UserApiService() }

        val filteredActiveUsers = remember(activeSearchQuery, activeUsers) {
            if (activeSearchQuery.isBlank()) {
                activeUsers
            } else {
                activeUsers.filter {
                    it.flatNo.contains(activeSearchQuery, ignoreCase = true)
                }
            }
        }

        val filteredAllUsers = remember(allSearchQuery, allUsers) {
            if (allSearchQuery.isBlank()) {
                allUsers
            } else {
                allUsers.filter {
                    it.flatNo.contains(allSearchQuery, ignoreCase = true)
                }
            }
        }

        suspend fun loadActiveUsers() {
            try {
                val response = userApiService.fetchActiveUsers()
                activeUsers = response
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load data: ${e.message}"
            }
        }

        suspend fun loadAllUsers() {
            try {
                val response = userApiService.fetchAllUsers()
                allUsers = response
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load data: ${e.message}"
            }
        }

        LaunchedEffect(Unit) {
            scope.launch {
                loadActiveUsers()
                loadAllUsers()
                isLoading = false
            }
        }

        LaunchedEffect(selectedTab) {
            if (!isLoading) {
                scope.launch {
                    if (selectedTab == 0) {
                        isRefreshing = true
                        loadActiveUsers()
                        isRefreshing = false
                    } else if (selectedTab == 1) {
                        isRefreshing = true
                        loadAllUsers()
                        isRefreshing = false
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                    )
                }
            ) {
                Tab(
                    text = { Text("Active") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                Tab(
                    text = { Text("All") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Active Tab Content
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = activeSearchQuery,
                                onValueChange = { activeSearchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search active users...") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading
                            )

                            Button(onClick = {
                                scope.launch {
                                    isRefreshing = true
                                    loadActiveUsers()
                                    isRefreshing = false
                                }
                            }) {
                                Text("Refresh")
                            }
                        }

                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                scope.launch {
                                    isRefreshing = true
                                    loadActiveUsers()
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when {
                                isLoading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator()
                                            Text(
                                                text = "Loading...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(top = 16.dp)
                                            )
                                        }
                                    }
                                }

                                errorMessage != null -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = errorMessage!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }

                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(filteredActiveUsers) { user ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                elevation = CardDefaults.cardElevation(
                                                    defaultElevation = 2.dp
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Text(
                                                        text = "${user.flatNo} | Expiry: ${user.expiryDate}",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // All Tab Content
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = allSearchQuery,
                                onValueChange = { allSearchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search all users...") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading
                            )

                            Button(onClick = {
                                scope.launch {
                                    isRefreshing = true
                                    loadAllUsers()
                                    isRefreshing = false
                                }
                            }) {
                                Text("Refresh")
                            }
                        }

                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                scope.launch {
                                    isRefreshing = true
                                    loadAllUsers()
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when {
                                isLoading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator()
                                            Text(
                                                text = "Loading...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(top = 16.dp)
                                            )
                                        }
                                    }
                                }

                                errorMessage != null -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = errorMessage!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(filteredAllUsers) { user ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                elevation = CardDefaults.cardElevation(
                                                    defaultElevation = 2.dp
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Text(
                                                        text = "${user.flatNo} | Expiry: ${user.expiryDate}",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}