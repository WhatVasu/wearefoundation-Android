package com.example.prajarakshak.ui

import android.Manifest
import android.content.Intent
import android.content.MutableContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.prajarakshak.BuildConfig
import com.example.prajarakshak.data.model.CaseReport
import com.example.prajarakshak.data.model.ReportRequest
import com.example.prajarakshak.data.model.SearchRequest
import com.example.prajarakshak.viewmodel.FindBackViewModel
import com.example.prajarakshak.viewmodel.FindBackUiState
import com.example.prajarakshak.viewmodel.NoticeType
import com.example.prajarakshak.viewmodel.ReportFormDraft
import com.example.prajarakshak.viewmodel.UiNotice
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.io.File
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val tabs = listOf(
    Tab("home", "Home", Icons.Default.Home),
    Tab("search", "Search", Icons.Default.Search),
    Tab("reports", "My Reports", Icons.Default.Assignment),
    Tab("profile", "Profile", Icons.Default.Person)
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FindBackApp(viewModel: FindBackViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("We are Foundation Telangana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Every Person Matters. Every Reunion Counts.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (tabs.any { it.route == currentDestination?.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize()
            ) {
            composable("home") {
                HomeScreen(
                    state = state,
                    onFound = { navController.navigate("found") },
                    onFind = { navController.navigate("search") },
                    onReports = { navController.navigate("reports") },
                    onNotifications = { navController.navigate("notifications") },
                    onProfile = { navController.navigate("profile") },
                    onEmergency = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
                    }
                )
            }
            composable("search") {
                SearchBrowseScreen(
                    state = state,
                    onOpenSearch = { navController.navigate("advancedSearch") },
                    onReportMissing = { navController.navigate("missing") },
                    onOpenDetails = { navController.navigate("reportDetails/$it") },
                    onDistrictChanged = { district -> viewModel.browseReports(district) },
                    onLoadMore = { viewModel.browseReports(refresh = false) },
                    onClearSearch = { viewModel.updateSearchFilters(state.searchFilters.copy(name = null)) }
                )
            }
            composable("advancedSearch") {
                AdvancedSearchScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSearch = viewModel::search,
                    onFiltersChanged = viewModel::updateSearchFilters,
                    onNotify = viewModel::notify,
                    onOpenDetails = { navController.navigate("reportDetails/$it") },
                    onLoadMore = viewModel::searchMore
                )
            }
            composable("reportDetails/{reportId}") { entry ->
                val reportId = entry.arguments?.getString("reportId").orEmpty()
                LaunchedEffect(reportId) { viewModel.loadReportDetails(reportId) }
                ReportDetailsScreen(
                    report = state.selectedReport?.takeIf { it.reportId == reportId },
                    loading = state.detailsLoading,
                    error = state.detailsError,
                    onRetry = { viewModel.loadReportDetails(reportId) },
                    onBack = {
                        viewModel.clearSelectedReport()
                        navController.popBackStack()
                    },
                    onPotentialMatch = { reportId ->
                        navController.navigate("found-match/${Uri.encode(reportId)}")
                    }
                )
            }
            composable("reports") {
                ReportsScreen(state, viewModel, onLogin = { navController.navigate("profile") }, onRefresh = viewModel::loadReports)
            }
            composable("notifications") {
                NotificationsScreen(state, viewModel, onBack = { navController.popBackStack() })
            }
            composable("profile") {
                ProfileScreen(
                    state = state,
                    viewModel = viewModel,
                    onAdmin = { navController.navigate("admin") },
                    onAuthenticated = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("admin") {
                AdminScreen(state, viewModel, onBack = { navController.popBackStack() })
            }
            composable("found") {
                ReportFormScreen(
                    missing = false,
                    state = state,
                    viewModel = viewModel,
                    onSignIn = { navController.navigate("profile") },
                    onBack = { navController.popBackStack() },
                    onSubmit = viewModel::submitReport
                )
            }
            composable("found-match/{reportId}") { entry ->
                ReportFormScreen(
                    missing = false,
                    state = state,
                    viewModel = viewModel,
                    potentialCaseId = entry.arguments?.getString("reportId"),
                    onSignIn = { navController.navigate("profile") },
                    onBack = { navController.popBackStack() },
                    onSubmit = viewModel::submitReport
                )
            }
            composable("missing") {
                ReportFormScreen(
                    missing = true,
                    state = state,
                    viewModel = viewModel,
                    onSignIn = { navController.navigate("profile") },
                    onBack = { navController.popBackStack() },
                    onSubmit = viewModel::submitReport
                )
            }
            }
            PopupNotification(
                notice = state.notice,
                onDismiss = viewModel::dismissNotice,
                modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PopupNotification(
    notice: UiNotice?,
    onDismiss: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
        LaunchedEffect(notice?.id) {
            val currentId = notice?.id ?: return@LaunchedEffect
            kotlinx.coroutines.delay(4_000)
            onDismiss(currentId)
        }

        AnimatedVisibility(
            visible = notice != null,
            modifier = modifier,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 }
        ) {
            val current = notice ?: return@AnimatedVisibility
            val (background, foreground, icon) = when (current.type) {
                NoticeType.SUCCESS -> Triple(
                    androidx.compose.ui.graphics.Color(0xFFE7F6EC),
                    androidx.compose.ui.graphics.Color(0xFF176B36),
                    Icons.Default.CheckCircle
                )
                NoticeType.ERROR -> Triple(
                    androidx.compose.ui.graphics.Color(0xFFFCE8E6),
                    androidx.compose.ui.graphics.Color(0xFFB3261E),
                    Icons.Default.Error
                )
                NoticeType.WARNING -> Triple(
                    androidx.compose.ui.graphics.Color(0xFFFFF3D6),
                    androidx.compose.ui.graphics.Color(0xFF8A4B08),
                    Icons.Default.Warning
                )
                NoticeType.INFORMATION -> Triple(
                    androidx.compose.ui.graphics.Color(0xFFE6F2FF),
                    androidx.compose.ui.graphics.Color(0xFF1557A0),
                    Icons.Default.Info
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(14.dp),
                color = background,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(icon, contentDescription = current.type.name.lowercase(), tint = foreground)
                    Text(
                        current.message,
                        modifier = Modifier.weight(1f),
                        color = foreground,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { onDismiss(current.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss notification", tint = foreground)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: FindBackUiState,
    onFound: () -> Unit,
    onFind: () -> Unit,
    onReports: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onEmergency: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.user == null) "Welcome" else "Hello, ${state.user.displayName ?: "there"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("A safer way to help families reconnect.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp).clickable(onClick = onProfile)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("FB", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            ActionCard(
                title = "Report a Person Found",
                subtitle = "Share safe, useful details about someone who may need help.",
                badge = "HELP SOMEONE",
                onClick = onFound
            )
        }
        item {
            ActionCard(
                title = "Find a Missing Person",
                subtitle = "Search published cases using name, area and physical details.",
                badge = "SEARCH CASES",
                onClick = onFind,
                secondary = true
            )
        }
        item {
            SecondaryAction("My Reports", onReports, Modifier.fillMaxWidth())
        }
        if (state.user != null) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your report status", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusCount("Submitted", state.reports.count { it.status == "SUBMITTED" })
                            StatusCount("Review", state.reports.count { it.status == "UNDER_REVIEW" })
                            StatusCount("Resolved", state.reports.count { it.status == "RESOLVED" })
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onNotifications, modifier = Modifier.fillMaxWidth()) {
                Text("Notifications")
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A note on safety", fontWeight = FontWeight.SemiBold)
                    Text(
                        "This is a public-service prototype, not an official government or police service. " +
                            "For immediate danger or urgent assistance, contact emergency services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onEmergency) { Text("Call 112") }
                }
            }
        }
        item {
            TextButton(onClick = onProfile) { Text(if (state.user == null) "Sign in to submit reports" else "View your profile") }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
    secondary: Boolean = false
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (secondary) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.primary
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall,
                color = if (secondary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f),
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (secondary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (secondary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary.copy(alpha = .86f)
            )
        }
    }
}

@Composable
private fun SecondaryAction(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(72.dp), shape = RoundedCornerShape(14.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBrowseScreen(
    state: FindBackUiState,
    onOpenSearch: () -> Unit,
    onReportMissing: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onDistrictChanged: (String) -> Unit,
    onLoadMore: () -> Unit,
    onClearSearch: () -> Unit = {}
) {
    var district by rememberSaveable { mutableStateOf(state.browseDistrict) }
    val searchSummary = listOfNotNull(
        state.searchFilters.name?.takeIf(String::isNotBlank),
        state.searchFilters.referenceId?.takeIf(String::isNotBlank),
        state.searchFilters.location?.takeIf(String::isNotBlank)
    ).firstOrNull()
    LaunchedEffect(Unit) {
        if (state.browseResults.isEmpty() && !state.browseLoading && state.browseError == null) {
            onDistrictChanged(district)
        }
    }

    val listState = rememberLazyListState()
    var pullDistance by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxSize().pointerInput(state.browseLoading, district) {
            detectVerticalDragGestures(
                onVerticalDrag = { _, amount ->
                    if (listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0 &&
                        amount > 0
                    ) {
                        pullDistance = (pullDistance + amount).coerceAtMost(180f)
                    } else if (amount < 0) {
                        pullDistance = (pullDistance + amount).coerceAtLeast(0f)
                    }
                },
                onDragEnd = {
                    if (pullDistance >= 96f && !state.browseLoading) onDistrictChanged(district)
                    pullDistance = 0f
                }
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Find Missing People", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Browse reports and help identify individuals.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Surface(
                    onClick = onOpenSearch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        Modifier.padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            searchSummary
                                ?: "Search by name, location or report ID...",
                            Modifier.weight(1f).padding(horizontal = 10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (!state.searchFilters.name.isNullOrBlank()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Open search filters")
                        }
                    }
                    if (state.browseLoading && state.browseResults.isNotEmpty()) {
                        CircularProgressIndicator(Modifier.align(Alignment.TopCenter).padding(10.dp).size(28.dp))
                    } else if (pullDistance >= 56f) {
                        Text(
                            "Release to refresh",
                            Modifier.align(Alignment.TopCenter).padding(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            item {
                OutlinedButton(onClick = onOpenSearch, modifier = Modifier.fillMaxWidth()) {
                    Text("Search Filters")
                }
            }
            item {
                OutlinedButton(onClick = onReportMissing, modifier = Modifier.fillMaxWidth()) {
                    Text("Register a missing person")
                }
            }
            item {
                FormField("District (optional)", district, { district = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDistrictChanged(district) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Apply district") }
                    TextButton(
                        onClick = {
                            district = ""
                            onDistrictChanged("")
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("All districts") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recently reported", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Newest first", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.browseError != null && state.browseResults.isEmpty()) {
                item {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Reports could not be loaded.")
                            Text(state.browseError, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { onDistrictChanged(district) }) { Text("Retry") }
                        }
                    }
                }
            } else if (state.browseLoading && state.browseResults.isEmpty()) {
                items(3) { FeedSkeleton() }
            } else if (state.browseResults.isEmpty()) {
                item {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No verified found-person reports are available yet.", fontWeight = FontWeight.SemiBold)
                            Text("Try another district or check back later.")
                            OutlinedButton(onClick = { onDistrictChanged(district) }) { Text("Refresh") }
                            if (state.browseHasMore) {
                                OutlinedButton(
                                    onClick = onLoadMore,
                                    enabled = !state.browseLoading
                                ) { Text("Load more reports") }
                            }
                        }
                    }
                }
            } else {
                items(state.browseResults, key = { it.reportId }) { report ->
                    BrowseReportCard(report) { onOpenDetails(report.reportId) }
                }
                if (state.browseError != null) {
                    item {
                        TextButton(onClick = { onDistrictChanged(district) }) {
                            Text("Couldn't refresh. Tap to retry.")
                        }
                    }
                }
                if (state.browseHasMore) {
                    item {
                        OutlinedButton(
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.browseLoading
                        ) {
                            if (state.browseLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Load more reports")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedSkeleton() {
    Card(shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(width = 104.dp, height = 132.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth(.7f).height(18.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                Box(Modifier.fillMaxWidth().height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                Box(Modifier.fillMaxWidth(.85f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                Box(Modifier.fillMaxWidth(.55f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
            }
        }
    }
}

@Composable
private fun BrowseReportCard(report: CaseReport, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(width = 104.dp, height = 132.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                val imageUrl = report.imageUrls.orEmpty().firstOrNull()
                if (imageUrl.isNullOrBlank()) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = "Photo unavailable", modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                } else {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Public identification photo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (!report.name.isNullOrBlank()) {
                    Text(
                        report.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text(
                    listOfNotNull(report.gender?.takeIf { it.isNotBlank() }, report.age?.let { "Approx. $it years" })
                        .joinToString(" · ").ifBlank { "Age and gender unknown" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Reported in ${report.district ?: "district not listed"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Reported ${report.createdAt?.take(10) ?: "date unavailable"}",
                    style = MaterialTheme.typography.bodySmall
                )
                val featureSummary = report.physicalFeatures.values.filter { it.isNotBlank() }.joinToString(", ")
                if (featureSummary.isNotBlank()) {
                    Text(featureSummary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Text("Report ID: ${report.reportId}", style = MaterialTheme.typography.labelSmall)
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(report.status.replace('_', ' '), Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
                Text("View details →", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ReportDetailsScreen(
    report: CaseReport?,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onPotentialMatch: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹  Back to reports") }
        when {
            report != null -> {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!report.imageUrls.isNullOrEmpty()) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(report.imageUrls) { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Public identification photo",
                                        modifier = Modifier.size(width = 240.dp, height = 280.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = "Photo unavailable", modifier = Modifier.size(64.dp))
                                }
                            }
                        }
                    }
                    item { Text("Report details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                    item { Text("Reference ID: ${report.reportId}", style = MaterialTheme.typography.titleSmall) }
                    if (report.reportType == "MISSING" && !report.name.isNullOrBlank()) {
                        item { DetailLine("Missing person's name", report.name) }
                    }
                    item {
                        DetailLine(
                            "Person",
                            listOfNotNull(
                                report.gender?.takeIf(String::isNotBlank),
                                report.age?.let { "Approx. $it years" }
                            ).joinToString(" · ").ifBlank { "Age and gender unknown" }
                        )
                    }
                    item { DetailLine("Reported district", report.district ?: "Not available") }
                    item { DetailLine("General location", "Precise location is not displayed for privacy.") }
                    item { DetailLine("Date reported", report.createdAt?.take(10) ?: "Not available") }
                    if (!report.lastSeenDate.isNullOrBlank()) {
                        item { DetailLine("Last-seen date", report.lastSeenDate) }
                    }
                    item {
                        val characteristics = report.physicalFeatures.entries
                            .filter { it.value.isNotBlank() }
                            .joinToString("\n") { (key, value) -> "${key.replaceFirstChar(Char::uppercase)}: $value" }
                            .ifBlank { "No public physical characteristics were provided." }
                        DetailLine("Physical characteristics", characteristics)
                    }
                    item {
                        DetailLine(
                            "Case status",
                            if (report.status == "VERIFIED") "Verified for public browsing" else "Status not available"
                        )
                    }
                    item {
                        Text(
                            "Reporter identity, contact information and exact location are never shown here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (report.reportType == "FOUND") {
                    Button(
                        onClick = { onPotentialMatch(report.reportId) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("I Think This Person Is Missing") }
                }
            }
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(error ?: "This report is unavailable.", color = MaterialTheme.colorScheme.error)
                if (error != null) TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AdvancedSearchScreen(
    state: FindBackUiState,
    onBack: () -> Unit,
    onSearch: (SearchRequest) -> Unit,
    onFiltersChanged: (SearchRequest) -> Unit,
    onNotify: (String, NoticeType) -> Unit,
    onOpenDetails: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val initial = state.searchFilters
    var name by rememberSaveable { mutableStateOf(initial.name.orEmpty()) }
    var referenceId by rememberSaveable { mutableStateOf(initial.referenceId.orEmpty()) }
    var ageMin by rememberSaveable { mutableStateOf(initial.ageMin?.toString().orEmpty()) }
    var ageMax by rememberSaveable { mutableStateOf(initial.ageMax?.toString().orEmpty()) }
    var gender by rememberSaveable { mutableStateOf(initial.gender.orEmpty()) }
    var district by rememberSaveable { mutableStateOf(initial.district.orEmpty()) }
    var area by rememberSaveable { mutableStateOf(initial.area.orEmpty()) }
    var lastSeenLocation by rememberSaveable { mutableStateOf(initial.lastSeenLocation.orEmpty()) }
    var hair by rememberSaveable { mutableStateOf(initial.hair.orEmpty()) }
    var height by rememberSaveable { mutableStateOf(initial.height.orEmpty()) }
    var mole by rememberSaveable { mutableStateOf(initial.mole.orEmpty()) }
    var scar by rememberSaveable { mutableStateOf(initial.scar.orEmpty()) }
    var birthmark by rememberSaveable { mutableStateOf(initial.birthmark.orEmpty()) }
    var otherFeatures by rememberSaveable { mutableStateOf(initial.features.orEmpty()) }
    var dateFrom by rememberSaveable { mutableStateOf(initial.dateFrom.orEmpty()) }
    var dateTo by rememberSaveable { mutableStateOf(initial.dateTo.orEmpty()) }
    var referenceImage by remember { mutableStateOf<Uri?>(null) }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }
    var searchValidationAttempted by rememberSaveable { mutableStateOf(false) }
    val ageMinError = if (searchValidationAttempted && ageMin.isNotBlank()) {
        if (ageMin.toIntOrNull() in 0..120) null else "Enter an age from 0 to 120."
    } else null
    val ageMaxError = if (searchValidationAttempted && ageMax.isNotBlank()) {
        if (ageMax.toIntOrNull() in 0..120) null else "Enter an age from 0 to 120."
    } else null
    val dateFromError = if (searchValidationAttempted) reportDateValidationError(dateFrom.trim()) else null
    val dateToError = if (searchValidationAttempted) {
        reportDateValidationError(dateTo.trim())
            ?: if (dateFrom.isNotBlank() && dateTo.isNotBlank() && dateFrom > dateTo) {
                "End date must be on or after the start date."
            } else null
    } else null
    val imagePicker = rememberLauncherForActivityResult(PickVisualMedia()) { referenceImage = it }

    fun snapshot() = SearchRequest(
        mode = "match",
        name = name.trim().ifBlank { null },
        referenceId = referenceId.trim().ifBlank { null },
        ageMin = ageMin.toIntOrNull(),
        ageMax = ageMax.toIntOrNull(),
        gender = gender.ifBlank { null },
        district = district.trim().ifBlank { null },
        location = listOf(area, lastSeenLocation).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null },
        area = area.trim().ifBlank { null },
        lastSeenLocation = lastSeenLocation.trim().ifBlank { null },
        hair = hair.trim().ifBlank { null },
        height = height.trim().ifBlank { null },
        mole = mole.trim().ifBlank { null },
        scar = scar.trim().ifBlank { null },
        birthmark = birthmark.trim().ifBlank { null },
        features = otherFeatures.trim().ifBlank { null },
        dateFrom = dateFrom.trim().ifBlank { null },
        dateTo = dateTo.trim().ifBlank { null }
    )

    fun updateFilters() {
        onFiltersChanged(snapshot())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹  Back to reports") }
            Text("Search Missing Persons", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Combine any details you know. Matches are suggestions for human review.")
        }
        item { SectionTitle("Personal details") }
        item { FormField("Name", name, { name = it; updateFilters() }) }
        item { FormField("Report ID", referenceId, { referenceId = it; updateFilters() }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField(
                    "Age from", ageMin, { ageMin = it; updateFilters() },
                    Modifier.weight(1f), KeyboardType.Number,
                    isError = ageMinError != null, supportingText = ageMinError
                )
                val ageOrderError = searchValidationAttempted &&
                    ageMin.toIntOrNull() != null && ageMax.toIntOrNull() != null &&
                    ageMin.toInt() > ageMax.toInt()
                FormField(
                    "Age to", ageMax, { ageMax = it; updateFilters() },
                    Modifier.weight(1f), KeyboardType.Number,
                    isError = ageMaxError != null || ageOrderError,
                    supportingText = ageMaxError
                        ?: if (ageOrderError) "End age must be at least the start age." else null
                )
            }
        }
        item { GenderDropdown(gender) { gender = it; updateFilters() } }
        item { SectionTitle("Location") }
        item { FormField("District", district, { district = it; updateFilters() }) }
        item { FormField("Area / locality", area, { area = it; updateFilters() }) }
        item { FormField("Last-seen location", lastSeenLocation, { lastSeenLocation = it; updateFilters() }) }
        item { SectionTitle("Physical characteristics") }
        item { FormField("Hair colour", hair, { hair = it; updateFilters() }) }
        item { FormField("Height", height, { height = it; updateFilters() }) }
        item { FormField("Mole", mole, { mole = it; updateFilters() }) }
        item { FormField("Scar", scar, { scar = it; updateFilters() }) }
        item { FormField("Birthmark", birthmark, { birthmark = it; updateFilters() }) }
        item { FormField("Additional identifying features", otherFeatures, { otherFeatures = it; updateFilters() }, minLines = 2) }
        item { SectionTitle("Date range (YYYY-MM-DD)") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField(
                    "Last seen from", dateFrom, { dateFrom = it; updateFilters() },
                    Modifier.weight(1f), isError = dateFromError != null, supportingText = dateFromError
                )
                FormField(
                    "Last seen to", dateTo, { dateTo = it; updateFilters() },
                    Modifier.weight(1f), isError = dateToError != null, supportingText = dateToError
                )
            }
        }
        item { SectionTitle("Reference photograph (optional)") }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    referenceImage?.let {
                        AsyncImage(it, contentDescription = "Search reference photo", modifier = Modifier.fillMaxWidth().height(180.dp))
                    }
                    Text(
                        "A reference photo is for your comparison only. Automated facial matching is not enabled.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { imagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) {
                            Text(if (referenceImage == null) "Upload photo" else "Replace photo")
                        }
                        if (referenceImage != null) TextButton(onClick = { referenceImage = null }) { Text("Remove") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        searchValidationAttempted = true
                        validation = when {
                            ageMinError != null -> ageMinError
                            ageMaxError != null -> ageMaxError
                            ageMin.toIntOrNull() != null && ageMax.toIntOrNull() != null &&
                                ageMin.toInt() > ageMax.toInt() -> "Minimum age cannot exceed maximum age."
                            dateFromError != null -> dateFromError
                            dateToError != null -> dateToError
                            else -> null
                        }
                        if (validation == null) onSearch(snapshot())
                        else onNotify(validation!!, NoticeType.WARNING)
                    },
                    enabled = !state.searchLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.searchLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Search")
                }
                OutlinedButton(onClick = {
                    name = ""; referenceId = ""; ageMin = ""; ageMax = ""; gender = ""; district = ""
                    area = ""; lastSeenLocation = ""; hair = ""; height = ""; mole = ""
                    scar = ""; birthmark = ""; otherFeatures = ""; dateFrom = ""; dateTo = ""
                    referenceImage = null
                    validation = null
                    searchValidationAttempted = false
                    onFiltersChanged(SearchRequest())
                    onSearch(SearchRequest())
                }) { Text("Reset Filters") }
            }
        }
        if (validation != null) {
            item { Text(validation!!, color = MaterialTheme.colorScheme.error) }
        }
        if (state.searchError != null) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.searchError, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = { onSearch(snapshot()) }) { Text("Retry search") }
                    }
                }
            }
        }
        if (!state.searchLoading && state.searchResults.isEmpty() && state.searchError == null) {
            item {
                Card {
                    Text(
                        "No matching reports. Try fewer filters or broaden the date and location range.",
                        Modifier.padding(18.dp)
                    )
                }
            }
        }
        items(state.searchResults, key = { it.reportId }) { report ->
            BrowseReportCard(report) { onOpenDetails(report.reportId) }
        }
        if (state.searchHasMore) {
            item {
                OutlinedButton(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.searchLoading
                ) { Text("Load more results") }
            }
        }
    }
}

@Composable
private fun ReportsScreen(
    state: FindBackUiState,
    viewModel: FindBackViewModel,
    onLogin: () -> Unit,
    onRefresh: () -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf("FOUND") }
    var editingReport by remember { mutableStateOf<CaseReport?>(null) }
    var editDescription by rememberSaveable { mutableStateOf("") }
    var processedUpdateId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.lastUpdatedReportId) {
        val updatedId = state.lastUpdatedReportId
        if (updatedId == null) {
            processedUpdateId = null
        } else if (processedUpdateId != updatedId) {
            processedUpdateId = updatedId
            if (editingReport?.reportId == updatedId) editingReport = null
        }
    }
    LaunchedEffect(state.user?.uid) {
        if (state.user != null) onRefresh()
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Reports", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPill("Found reports", selectedType == "FOUND") { selectedType = "FOUND" }
            FilterPill("Missing cases", selectedType == "MISSING") { selectedType = "MISSING" }
            FilterPill("Sightings", selectedType == "SIGHTING") { selectedType = "SIGHTING" }
        }
        if (state.user == null) {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sign in to view your reports.")
                    Button(onClick = onLogin) { Text("Sign in") }
                }
            }
        } else {
            val reports = state.reports.filter { it.reportType == selectedType }
            if (state.reportsError != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.reportsError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRefresh, enabled = !state.reportsLoading) { Text("Retry") }
                }
            } else if (reports.isEmpty()) {
                Card {
                    Text(
                        if (state.reportsLoading) "Loading your reports…" else "No reports in this category yet.",
                        Modifier.padding(18.dp)
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(reports) { report ->
                        CaseCard(report, showDescription = true) {
                            editingReport = report
                            editDescription = report.description.orEmpty()
                        }
                    }
                }
            }
        }
        if (editingReport != null) {
            AlertDialog(
                onDismissRequest = { if (!state.loading) editingReport = null },
                title = { Text("Add report information") },
                text = {
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Additional details") },
                        minLines = 3
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            editingReport?.let { viewModel.updateReportDescription(it.reportId, editDescription) }
                        },
                        enabled = !state.loading
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editingReport = null }, enabled = !state.loading) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun StatusCount(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NotificationsScreen(
    state: FindBackUiState,
    viewModel: FindBackViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(state.user?.uid) {
        if (state.user != null) viewModel.loadNotifications()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onBack) { Text("‹  Back") }
                Text("Notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        if (state.user == null) {
            item { Text("Sign in to view report updates.") }
        } else if (state.notificationsError != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.notificationsError, color = MaterialTheme.colorScheme.error)
                    TextButton(
                        onClick = { viewModel.loadNotifications() },
                        enabled = !state.notificationsLoading
                    ) { Text("Retry") }
                }
            }
        } else if (state.notifications.isEmpty()) {
            item { Text(if (state.notificationsLoading) "Loading notifications…" else "No notifications yet.") }
        } else {
            items(state.notifications) { notification ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(notification.message, fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold)
                        if (!notification.read) {
                            TextButton(onClick = { viewModel.markNotificationRead(notification.notificationId) }) {
                                Text("Mark as read")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) FilledTonalButton(onClick = onClick) { Text(label) }
    else OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun CaseCard(
    report: CaseReport,
    showDescription: Boolean = false,
    onUpdate: (() -> Unit)? = null
) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (showDescription && !report.imageUrls.isNullOrEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(report.imageUrls.orEmpty()) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Submitted report photo",
                            modifier = Modifier.size(112.dp)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(report.name?.ifBlank { null } ?: "Unidentified person", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(report.status, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("Ref ${report.reportId} · ${report.district ?: "District not listed"}", style = MaterialTheme.typography.bodySmall)
            Text(
                listOfNotNull(report.age?.let { "Approx. $it years" }, report.location).joinToString(" · ").ifBlank { "Location details restricted" },
                style = MaterialTheme.typography.bodyMedium
            )
            if (report.matchingSignals.isNotEmpty()) {
                Text(
                    "Matching details: ${report.matchingSignals.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (showDescription && !report.description.isNullOrBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 3.dp))
                Text(report.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            }
            if (showDescription && report.statusHistory.isNotEmpty()) {
                Text(
                    "Status history: " + report.statusHistory.joinToString(" → ") { it.status.replace('_', ' ') },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showDescription && onUpdate != null && report.status in setOf("SUBMITTED", "UNDER_REVIEW")) {
                TextButton(onClick = onUpdate) { Text("Add or update details") }
            }
            Text("Potential match only · Do not share private contact details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileScreen(
    state: FindBackUiState,
    viewModel: FindBackViewModel,
    onAdmin: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var creatingAccount by rememberSaveable { mutableStateOf(false) }
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }
    var authValidationAttempted by rememberSaveable { mutableStateOf(false) }
    var profileValidationAttempted by rememberSaveable { mutableStateOf(false) }
    var wasSignedOut by remember { mutableStateOf(state.user == null) }
    val emailError = if (authValidationAttempted) emailValidationError(email) else null
    val passwordError = if (authValidationAttempted) {
        passwordValidationError(password, requireStrong = creatingAccount)
    } else null
    val nameError = if (authValidationAttempted && creatingAccount && name.isBlank()) {
        "Full name is required."
    } else null
    val phoneError = if (authValidationAttempted && creatingAccount) phoneValidationError(phone) else null
    val confirmPasswordError = if (
        authValidationAttempted && creatingAccount && password != confirmPassword
    ) "Passwords do not match." else null

    LaunchedEffect(state.user?.uid) {
        if (state.user == null) {
            wasSignedOut = true
        } else if (wasSignedOut) {
            wasSignedOut = false
            onAuthenticated()
        }
    }
    LaunchedEffect(state.profile?.userId) {
        state.profile?.let { profile ->
            name = profile.name.orEmpty()
            phone = profile.phone.orEmpty()
            district = profile.district.orEmpty()
        }
    }
    LaunchedEffect(state.profileUpdateVersion) {
        if (state.profileUpdateVersion > 0) editingProfile = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (state.user != null) {
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.user.displayName ?: "We are Foundation member", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(state.user.email ?: "")
                        Text("Account type: ${state.profile?.role?.replaceFirstChar(Char::uppercase) ?: "Citizen"}")
                        Text("Admin privileges are assigned by the service operator.", style = MaterialTheme.typography.bodySmall)
                        if (state.profile == null) {
                            Text("Your account is signed in, but profile data could not be loaded.")
                            TextButton(
                                onClick = viewModel::loadProfile,
                                enabled = !state.loading
                            ) { Text("Retry profile loading") }
                        } else if (editingProfile) {
                            val profileNameError = if (profileValidationAttempted && name.isBlank()) "Full name is required." else null
                            val profilePhoneError = if (profileValidationAttempted) phoneValidationError(phone) else null
                            val profileDistrictError = if (profileValidationAttempted && district.isBlank()) "District is required." else null
                            FormField("Full name", name, { name = it }, isError = profileNameError != null, supportingText = profileNameError)
                            FormField(
                                "Mobile number", phone, { phone = it }, keyboardType = KeyboardType.Phone,
                                isError = profilePhoneError != null, supportingText = profilePhoneError
                            )
                            FormField("District", district, { district = it }, isError = profileDistrictError != null, supportingText = profileDistrictError)
                            Button(
                                onClick = {
                                    profileValidationAttempted = true
                                    if (name.isBlank() || phoneValidationError(phone) != null || district.isBlank()) {
                                        validation = "Correct the highlighted profile fields."
                                        viewModel.notify(validation!!, NoticeType.WARNING)
                                    } else {
                                        validation = null
                                        viewModel.updateProfile(name, phone, district)
                                    }
                                },
                                enabled = !state.loading
                            ) { Text("Save profile") }
                            TextButton(onClick = { editingProfile = false }) { Text("Cancel") }
                            if (validation != null) Text(validation!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Mobile: ${state.profile?.phone.orEmpty()}")
                            Text("District: ${state.profile?.district.orEmpty()}")
                            TextButton(onClick = { editingProfile = true }) { Text("Edit profile") }
                        }
                        if (state.profile?.role == "admin") {
                            OutlinedButton(onClick = onAdmin) { Text("Open admin review") }
                        }
                        OutlinedButton(onClick = viewModel::signOut) { Text("Sign out") }
                    }
                }
            }
        } else {
            item {
                Text(
                    if (creatingAccount) "Create a citizen account" else "Sign in",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (creatingAccount) {
                item { FormField("Full name", name, { name = it }, isError = nameError != null, supportingText = nameError) }
                item {
                    FormField(
                        "Mobile number", phone, { phone = it }, keyboardType = KeyboardType.Phone,
                        isError = phoneError != null, supportingText = phoneError
                    )
                }
                item {
                    val error = if (authValidationAttempted && district.isBlank()) "District is required." else null
                    FormField("District", district, { district = it }, isError = error != null, supportingText = error)
                }
            }
            item {
                FormField(
                    "Email", email, { email = it }, keyboardType = KeyboardType.Email,
                    isError = emailError != null, supportingText = emailError
                )
            }
            item {
                FormField(
                    "Password", password, { password = it },
                    visualTransformation = if (passwordVisible) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = passwordError != null, supportingText = passwordError
                )
            }
            if (creatingAccount) {
                item {
                    FormField(
                        "Confirm password", confirmPassword, { confirmPassword = it },
                        visualTransformation = if (confirmPasswordVisible) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide confirm password" else "Show confirm password"
                                )
                            }
                        },
                        isError = confirmPasswordError != null, supportingText = confirmPasswordError
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = termsAccepted, onCheckedChange = { termsAccepted = it })
                        Text("I acknowledge the privacy and safety guidance.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (validation != null) item { Text(validation!!, color = MaterialTheme.colorScheme.error) }
            item {
                Button(
                    onClick = {
                        authValidationAttempted = true
                        validation = when {
                            emailValidationError(email) != null -> emailValidationError(email)
                            passwordValidationError(password, requireStrong = creatingAccount) != null ->
                                passwordValidationError(password, requireStrong = creatingAccount)
                            creatingAccount && name.isBlank() -> "Enter your full name."
                            creatingAccount && phoneValidationError(phone) != null -> phoneValidationError(phone)
                            creatingAccount && district.isBlank() -> "Enter your district."
                            creatingAccount && password != confirmPassword -> "Passwords do not match."
                            creatingAccount && !termsAccepted -> "Please acknowledge the guidance to continue."
                            else -> null
                        }
                        validation?.let { viewModel.notify(it, NoticeType.WARNING) }
                        if (validation == null) {
                            if (creatingAccount) viewModel.register(name, email, phone, district, password)
                            else viewModel.signIn(email, password)
                        }
                    },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.loading) "Please wait…" else if (creatingAccount) "Create account" else "Sign in") }
            }
            item {
                Row {
                    TextButton(onClick = { creatingAccount = !creatingAccount; validation = null }) {
                        Text(if (creatingAccount) "Already have an account? Sign in" else "Create an account")
                    }
                    if (!creatingAccount) {
                        TextButton(onClick = {
                            authValidationAttempted = true
                            val error = emailValidationError(email)
                            if (error != null) {
                                validation = error
                                viewModel.notify(error, NoticeType.WARNING)
                            } else {
                                validation = null
                                viewModel.resetPassword(email)
                            }
                        }) { Text("Forgot password?") }
                    }
                }
            }
            item {
                HorizontalDivider()
                Text(
                    "Or continue with",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = {
                        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
                            validation = "Configure googleWebClientId in local.properties to enable Google sign-in."
                            viewModel.notify(validation!!, NoticeType.WARNING)
                        } else {
                            validation = null
                            coroutineScope.launch {
                                try {
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(
                                            GetSignInWithGoogleOption.Builder(
                                                BuildConfig.GOOGLE_WEB_CLIENT_ID
                                            ).build()
                                        )
                                        .build()
                                    val result = CredentialManager.create(context).getCredential(
                                        context = MutableContextWrapper(context),
                                        request = request
                                    )
                                    val credential = result.credential
                                    if (
                                        credential is CustomCredential &&
                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    ) {
                                        val idToken = GoogleIdTokenCredential.createFrom(
                                            credential.data
                                        ).idToken
                                        viewModel.signInWithGoogle(idToken)
                                    } else {
                                        validation = "Google did not return a supported sign-in credential."
                                        viewModel.notify(validation!!, NoticeType.ERROR)
                                    }
                                } catch (_: GetCredentialCancellationException) {
                                    validation = null
                                } catch (error: GoogleIdTokenParsingException) {
                                    validation = "Could not read the Google sign-in response: ${error.message}"
                                    viewModel.notify("Google sign-in response could not be read. Try again.", NoticeType.ERROR)
                                } catch (error: GetCredentialException) {
                                    validation = "Google sign-in failed: ${error.message}"
                                    viewModel.notify("Google sign-in failed. Check your connection and try again.", NoticeType.ERROR)
                                }
                            }
                        }
                    },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.loading) "Please wait…" else "Continue with Google")
                }
            }
            item {
                Text(
                    "This prototype is not affiliated with or endorsed by the Government of Telangana or Telangana Police.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AdminScreen(
    state: FindBackUiState,
    viewModel: FindBackViewModel,
    onBack: () -> Unit
) {
    var status by rememberSaveable { mutableStateOf("SUBMITTED") }
    var closeCandidate by remember { mutableStateOf<CaseReport?>(null) }
    var editCandidate by remember { mutableStateOf<CaseReport?>(null) }
    var deleteCandidate by remember { mutableStateOf<CaseReport?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var editAge by rememberSaveable { mutableStateOf("") }
    var editDistrict by rememberSaveable { mutableStateOf("") }
    var editLocation by rememberSaveable { mutableStateOf("") }
    var editDescription by rememberSaveable { mutableStateOf("") }
    var editError by rememberSaveable { mutableStateOf<String?>(null) }
    var processedEditId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.lastUpdatedReportId) {
        val editedId = state.lastUpdatedReportId
        if (editedId == null) {
            processedEditId = null
        } else if (processedEditId != editedId) {
            processedEditId = editedId
            if (editCandidate?.reportId == editedId) editCandidate = null
        }
    }
    LaunchedEffect(state.profile?.role, status) {
        if (state.profile?.role == "admin") viewModel.loadAdminReports(status)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onBack) { Text("‹  Back to profile") }
                Text("Admin review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        if (state.profile?.role != "admin") {
            item {
                Card {
                    Text(
                        "Administrator access is assigned by the service operator and verified by the backend.",
                        Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(
                        listOf(
                            "SUBMITTED", "UNDER_REVIEW", "POTENTIAL_MATCH",
                            "VERIFIED", "RESOLVED", "CLOSED"
                        )
                    ) { queue ->
                        FilterPill(queue.replace('_', ' '), status == queue) { status = queue }
                    }
                }
            }
            if (state.adminError != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.adminError, color = MaterialTheme.colorScheme.error)
                        TextButton(
                            onClick = { viewModel.loadAdminReports(status) },
                            enabled = !state.adminLoading
                        ) { Text("Retry") }
                    }
                }
            }
            if (state.adminReports.isEmpty()) {
                item {
                    when {
                        state.adminLoading -> Text("Loading queue…")
                        state.adminError != null -> Unit
                        else -> Text("No reports in this queue.")
                    }
                }
            }
            items(state.adminReports) { report ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${report.reportType} · ${report.reportId}", fontWeight = FontWeight.SemiBold)
                        report.potentialMatchReportId?.let {
                            Text("Potential sighting linked to report $it")
                        }
                        Text("${report.district ?: "—"} · ${report.location ?: "—"}")
                        if (!report.name.isNullOrBlank()) Text("Name: ${report.name}")
                        report.age?.let { Text("Age: $it") }
                        if (!report.description.isNullOrBlank()) Text(report.description)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editCandidate = report
                                        editName = report.name.orEmpty()
                                        editAge = report.age?.toString().orEmpty()
                                        editDistrict = report.district.orEmpty()
                                        editLocation = report.location.orEmpty()
                                        editDescription = report.description.orEmpty()
                                        editError = null
                                    },
                                    enabled = !state.loading
                                ) { Text("Edit") }
                                TextButton(
                                    onClick = { deleteCandidate = report },
                                    enabled = !state.loading
                                ) { Text("Delete") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (report.status == "SUBMITTED") {
                                    OutlinedButton(onClick = {
                                        viewModel.updateReportStatus(report.reportId, "UNDER_REVIEW", status)
                                    }, enabled = !state.loading) {
                                        Text("Review")
                                    }
                                }
                                if (report.status in setOf("SUBMITTED", "UNDER_REVIEW", "POTENTIAL_MATCH")) {
                                    OutlinedButton(onClick = {
                                        viewModel.updateReportStatus(report.reportId, "VERIFIED", status)
                                    }, enabled = !state.loading) {
                                        Text("Verify")
                                    }
                                }
                                if (report.status !in setOf("CLOSED", "RESOLVED")) {
                                    TextButton(
                                        onClick = { closeCandidate = report },
                                        enabled = !state.loading
                                    ) {
                                        Text("Close")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    closeCandidate?.let { report ->
        AlertDialog(
            onDismissRequest = { if (!state.loading) closeCandidate = null },
            title = { Text("Close this report?") },
            text = { Text("This changes the report status to Closed. You can reopen it only through a later admin status update.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateReportStatus(report.reportId, "CLOSED", status)
                        closeCandidate = null
                    },
                    enabled = !state.loading
                ) { Text("Close report") }
            },
            dismissButton = {
                TextButton(onClick = { closeCandidate = null }, enabled = !state.loading) { Text("Cancel") }
            }
        )
    }
    editCandidate?.let { report ->
        AlertDialog(
            onDismissRequest = { if (!state.loading) editCandidate = null },
            title = { Text("Edit report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editName, { editName = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(
                        editAge,
                        { editAge = it.filter(Char::isDigit).take(3) },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(editDistrict, { editDistrict = it }, label = { Text("District") }, singleLine = true)
                    OutlinedTextField(editLocation, { editLocation = it }, label = { Text("Area / locality") }, singleLine = true)
                    OutlinedTextField(editDescription, { editDescription = it }, label = { Text("Description") }, minLines = 3)
                    editError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ageValue = editAge.toIntOrNull()
                        editError = when {
                            editDistrict.isBlank() -> "District is required."
                            editLocation.isBlank() -> "Area or locality is required."
                            editAge.isNotBlank() && ageValue !in 0..120 -> "Age must be between 0 and 120."
                            else -> null
                        }
                        if (editError == null) {
                            viewModel.updateAdminReport(
                                report.reportId,
                                mapOf(
                                    "name" to editName.trim(),
                                    "age" to (ageValue ?: ""),
                                    "district" to editDistrict.trim(),
                                    "location" to editLocation.trim(),
                                    "description" to editDescription.trim()
                                ),
                                status
                            )
                        }
                    },
                    enabled = !state.loading
                ) { Text("Save changes") }
            },
            dismissButton = {
                TextButton(onClick = { editCandidate = null }, enabled = !state.loading) { Text("Cancel") }
            }
        )
    }
    deleteCandidate?.let { report ->
        AlertDialog(
            onDismissRequest = { if (!state.loading) deleteCandidate = null },
            title = { Text("Delete report?") },
            text = {
                Text("This permanently removes report ${report.reportId} and its uploaded photos. The report owner will be notified.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAdminReport(report.reportId, status)
                        deleteCandidate = null
                    },
                    enabled = !state.loading
                ) { Text("Delete permanently") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }, enabled = !state.loading) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFormScreen(
    missing: Boolean,
    state: FindBackUiState,
    viewModel: FindBackViewModel,
    potentialCaseId: String? = null,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    onSubmit: (ReportRequest, List<Uri>) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(missing, potentialCaseId) { viewModel.resetSubmissionState() }
    val initialDraft = remember(missing, potentialCaseId) {
        viewModel.reportDraft(missing, potentialCaseId)
    }
    var name by rememberSaveable { mutableStateOf(initialDraft.name) }
    var age by rememberSaveable { mutableStateOf(initialDraft.age) }
    var gender by rememberSaveable { mutableStateOf(initialDraft.gender) }
    var height by rememberSaveable { mutableStateOf(initialDraft.height) }
    var hair by rememberSaveable { mutableStateOf(initialDraft.hair) }
    var clothing by rememberSaveable { mutableStateOf(initialDraft.clothing) }
    var marks by rememberSaveable { mutableStateOf(initialDraft.marks) }
    var district by rememberSaveable { mutableStateOf(initialDraft.district) }
    var location by rememberSaveable { mutableStateOf(initialDraft.location) }
    var landmark by rememberSaveable { mutableStateOf(initialDraft.landmark) }
    var date by rememberSaveable { mutableStateOf(initialDraft.date) }
    var details by rememberSaveable { mutableStateOf(initialDraft.details) }
    var contactName by rememberSaveable { mutableStateOf(initialDraft.contactName) }
    var contactPhone by rememberSaveable { mutableStateOf(initialDraft.contactPhone) }
    var policeStation by rememberSaveable { mutableStateOf(initialDraft.policeStation) }
    var fir by rememberSaveable { mutableStateOf(initialDraft.fir) }
    val photoUris = remember(missing, potentialCaseId) {
        androidx.compose.runtime.mutableStateListOf<Uri>().apply {
            addAll(initialDraft.photoUris.mapNotNull(Uri::parse))
        }
    }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }
    var reportValidationAttempted by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val districtError = if (reportValidationAttempted && district.isBlank()) "District is required." else null
    val locationError = if (reportValidationAttempted && location.isBlank()) "Area or locality is required." else null
    val nameError = if (reportValidationAttempted && missing && name.isBlank()) "Missing person's name is required." else null
    val ageError = if (reportValidationAttempted && age.isNotBlank()) {
        val ageValue = age.toIntOrNull()
        if (ageValue == null || ageValue !in 0..120) "Enter a whole age from 0 to 120." else null
    } else null
    val contactNameError = if (reportValidationAttempted && missing && contactName.isBlank()) {
        "Family contact name is required."
    } else null
    val contactPhoneError = if (reportValidationAttempted && missing) phoneValidationError(contactPhone) else null
    val dateError = if (reportValidationAttempted) reportDateValidationError(date.trim()) else null
    fun persistDraft() {
        viewModel.saveReportDraft(
            missing,
            ReportFormDraft(
                name = name,
                age = age,
                gender = gender,
                height = height,
                hair = hair,
                clothing = clothing,
                marks = marks,
                district = district,
                location = location,
                landmark = landmark,
                date = date,
                details = details,
                contactName = contactName,
                contactPhone = contactPhone,
                policeStation = policeStation,
                fir = fir,
                photoUris = photoUris.map(Uri::toString)
            ),
            potentialCaseId
        )
    }

    val galleryPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null && photoUris.size < 4) {
            photoUris.add(uri)
            persistDraft()
        }
        else if (uri != null) viewModel.notify("A maximum of four photos is allowed.", NoticeType.WARNING)
    }
    val camera = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let(photoUris::add)
            persistDraft()
        } else {
            pendingCameraFile?.delete()
        }
        pendingCameraUri = null
        pendingCameraFile = null
    }
    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            captureLocation(
                context,
                onLocation = { lat, lon -> location = "GPS $lat, $lon"; persistDraft() },
                onError = { viewModel.notify(it, NoticeType.WARNING) }
            )
        } else {
            viewModel.notify("Location permission denied. Enter the locality manually.", NoticeType.WARNING)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().imePadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                when {
                    potentialCaseId != null -> "Submit a Potential Sighting"
                    missing -> "Register a Missing Person"
                    else -> "Report a Person Found"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (potentialCaseId != null) {
                    "Share when and where you observed this person. This private sighting will be linked to report $potentialCaseId for administrator review."
                } else {
                    "Fields marked optional can be left blank when unknown."
                }
            )
        }
        if (state.user == null) {
            item {
                TextButton(onClick = onSignIn) { Text("Sign in to submit this report") }
            }
        }
        item {
            SectionTitle(
                if (potentialCaseId != null) "Observation photograph (optional)"
                else "Photograph (optional)"
            )
        }
        item {
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (photoUris.isNotEmpty()) {
                        Text("${photoUris.size} of 4 photos selected", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(photoUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected person photograph",
                                    modifier = Modifier.size(112.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(112.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("No photograph selected") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val file = File(context.cacheDir, "findback-${System.currentTimeMillis()}.jpg")
                                val cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                pendingCameraUri = cameraUri
                                pendingCameraFile = file
                                camera.launch(cameraUri)
                            },
                            enabled = photoUris.size < 4,
                            modifier = Modifier.weight(1f)
                        ) { Text("Take photo") }
                        OutlinedButton(
                            onClick = { galleryPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                            enabled = photoUris.size < 4,
                            modifier = Modifier.weight(1f)
                        ) { Text("Choose photo") }
                    }
                    if (photoUris.isNotEmpty()) TextButton(onClick = { photoUris.clear() }) { Text("Remove all photos") }
                }
            }
        }
        item {
            SectionTitle(
                if (potentialCaseId != null) "Observed person (optional details)"
                else "Person details"
            )
        }
        item {
            FormField(
                if (missing) "Missing person's name" else "Person's name (if known)",
                name,
                { name = it; persistDraft() },
                isError = nameError != null, supportingText = nameError
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField(
                    "Approx. age", age, { age = it; persistDraft() },
                    Modifier.weight(1f), KeyboardType.Number,
                    isError = ageError != null, supportingText = ageError
                )
                GenderDropdown(gender, Modifier.weight(1f)) { gender = it; persistDraft() }
            }
        }
        item { FormField("Approximate height", height, { height = it; persistDraft() }) }
        item { FormField("Hair colour", hair, { hair = it; persistDraft() }) }
        item { FormField("Clothing", clothing, { clothing = it; persistDraft() }) }
        item { FormField("Distinctive marks (mole, scar, birthmark)", marks, { marks = it; persistDraft() }, minLines = 2) }
        item {
            SectionTitle(
                when {
                    potentialCaseId != null -> "Sighting details"
                    missing -> "Last-seen details"
                    else -> "Encounter details"
                }
            )
        }
        item {
            FormField(
                "District", district, { district = it; persistDraft() },
                isError = districtError != null, supportingText = districtError
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FormField(
                    "Area / locality", location, { location = it; persistDraft() },
                    Modifier.weight(1f), isError = locationError != null, supportingText = locationError
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (granted) captureLocation(
                            context,
                            onLocation = { lat, lon -> location = "GPS $lat, $lon"; persistDraft() },
                            onError = { viewModel.notify(it, NoticeType.WARNING) }
                        )
                        else permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                ) { Text("GPS") }
            }
        }
        item { FormField("Nearby landmark", landmark, { landmark = it; persistDraft() }) }
        item {
            FormField(
                when {
                    potentialCaseId != null -> "Date observed"
                    missing -> "Last-seen date"
                    else -> "Date encountered"
                },
                date,
                { date = it; persistDraft() },
                hint = "YYYY-MM-DD",
                isError = dateError != null,
                supportingText = dateError
            )
        }
        item {
            FormField(
                if (potentialCaseId != null) "What did you observe? (optional)"
                else "Additional description / observations",
                details,
                { details = it; persistDraft() },
                minLines = 3
            )
        }
        if (missing) {
            item { SectionTitle("Family contact and police report") }
            item {
                FormField(
                    "Family contact name", contactName, { contactName = it; persistDraft() },
                    isError = contactNameError != null, supportingText = contactNameError
                )
            }
            item {
                FormField(
                    "Verified contact number", contactPhone, { contactPhone = it; persistDraft() },
                    keyboardType = KeyboardType.Phone,
                    isError = contactPhoneError != null, supportingText = contactPhoneError
                )
            }
            item { FormField("Police station (optional)", policeStation, { policeStation = it; persistDraft() }) }
            item { FormField("Complaint / FIR reference (optional)", fir, { fir = it; persistDraft() }) }
            item {
                Text(
                    "An FIR number is not required for an initial case. Please also report a missing person to the appropriate police station.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Urgent danger or injury?", fontWeight = FontWeight.SemiBold)
                        Text("Contact emergency services. Do not put yourself or the person at risk.")
                        OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) }) {
                            Text("Call 112")
                        }
                    }
                }
            }
        }
        if (validation != null) item { Text(validation!!, color = MaterialTheme.colorScheme.error) }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !state.loading,
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text("Back") }
            Button(
                onClick = {
                    reportValidationAttempted = true
                    validation = when {
                        state.user == null -> "Sign in from Profile before submitting a report."
                        district.isBlank() -> "District is required."
                        location.isBlank() -> "Area or locality is required."
                        missing && name.isBlank() -> "Enter the missing person's name."
                        missing && contactName.isBlank() -> "Enter a family contact name."
                        missing && phoneValidationError(contactPhone) != null -> phoneValidationError(contactPhone)
                        age.isNotBlank() && (age.toIntOrNull() == null || age.toIntOrNull() !in 0..120) ->
                            "Enter a whole age from 0 to 120."
                        reportDateValidationError(date.trim()) != null ->
                            reportDateValidationError(date.trim())
                        else -> null
                    }
                    validation?.let { viewModel.notify(it, NoticeType.WARNING) }
                    if (validation == null) {
                        val description = details.trim()
                        val physicalFeatures = mapOf(
                            "height" to height.trim(),
                            "hair" to hair.trim(),
                            "clothing" to clothing.trim(),
                            "distinctiveMarks" to marks.trim()
                        ).filterValues(String::isNotBlank)
                        onSubmit(
                            ReportRequest(
                                reportType = when {
                                    potentialCaseId != null -> "SIGHTING"
                                    missing -> "MISSING"
                                    else -> "FOUND"
                                },
                                name = name.trim().ifBlank { null },
                                age = age.toIntOrNull(),
                                gender = gender.trim().ifBlank { null },
                                physicalFeatures = physicalFeatures,
                                district = district.trim(),
                                location = listOf(location.trim(), landmark.trim())
                                    .filter(String::isNotBlank).joinToString(", "),
                                description = description,
                                lastSeenDate = date.trim().ifBlank { null },
                                contactName = contactName.trim().ifBlank { null },
                                contactPhone = contactPhone.trim().ifBlank { null },
                                policeStation = policeStation.trim().ifBlank { null },
                                firReference = fir.trim().ifBlank { null },
                                potentialMatchReportId = potentialCaseId
                            ),
                            photoUris.toList()
                        )
                    }
                },
                enabled = !state.loading && state.lastSubmittedReportType != when {
                    potentialCaseId != null -> "SIGHTING"
                    missing -> "MISSING"
                    else -> "FOUND"
                },
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(
                    if (state.lastSubmittedReportType == when {
                            potentialCaseId != null -> "SIGHTING"
                            missing -> "MISSING"
                            else -> "FOUND"
                        })
                        "Submitted"
                    else if (potentialCaseId != null) "Submit Sighting" else "Submit Report"
                )
            }
    }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(
    selectedGender: String,
    modifier: Modifier = Modifier,
    onGenderSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedGender,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            placeholder = { if (selectedGender.isBlank()) Text("Select Gender") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("Male", "Female", "Others").forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onGenderSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    hint: String? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = hint?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        minLines = minLines,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(12.dp),
        singleLine = minLines == 1
    )
}

private fun captureLocation(
    context: android.content.Context,
    onLocation: (Double, Double) -> Unit,
    onError: (String) -> Unit
) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    try {
        client.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) onLocation(location.latitude, location.longitude)
                else onError("Location unavailable. Enter the locality manually.")
            }
            .addOnFailureListener {
                onError("Could not read location. Enter the locality manually.")
            }
    } catch (_: SecurityException) {
        onError("Location permission is required.")
    }
}
