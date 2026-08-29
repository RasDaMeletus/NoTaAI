package com.vinote.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.NotaEyeState
import com.vinote.data.repository.SyncStatus
import com.vinote.domain.finance.DailyTrendPoint
import com.vinote.domain.finance.SpendingTrendReport
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteTransactionTile
import com.vinote.ui.theme.ViNoteError
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNotePrimaryFixed
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSoftPink
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteSurfaceContainer
import com.vinote.ui.theme.ViNoteSurfaceContainerHigh
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow
import com.vinote.viewmodel.ActivityFilter
import com.vinote.viewmodel.ViNoteViewModel

@Composable
fun ActivityScreen(
    viewModel: ViNoteViewModel,
    onNavigateToDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.activityFilter.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val spendingTrends by viewModel.spendingTrends.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var isTrendsExpanded by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "sync_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cloud Sync button
                        IconButton(
                            onClick = { viewModel.syncExpensesWithFirestore() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ViNoteSecondaryFixed.copy(alpha = 0.5f))
                                .size(38.dp)
                                .testTag("sync_firestore_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync with Firestore",
                                tint = ViNotePrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .then(
                                        if (syncStatus == SyncStatus.SYNCING) Modifier.rotate(rotationAngle)
                                        else Modifier
                                    )
                            )
                        }

                        // Clear All History button
                        if (allTransactions.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearHistoryDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(ViNoteError.copy(alpha = 0.1f))
                                    .size(38.dp)
                                    .testTag("clear_history_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = ViNoteError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1. SPENDING TRENDS HERO SECTION
            item {
                SpendingTrendsCard(
                    report = spendingTrends,
                    isExpanded = isTrendsExpanded,
                    onToggleExpand = { isTrendsExpanded = !isTrendsExpanded },
                    notaConfig = notaConfig
                )
            }

            // 2. SEARCH & FILTER CONTROLS
            item {
                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(ViNoteSurfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ViNoteTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = "Search transactions...",
                                    color = ViNoteTextSecondary,
                                    fontSize = 15.sp
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_input")
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = ViNoteTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filters Row: All, Income, Expense
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        label = "All",
                        selected = currentFilter == ActivityFilter.ALL,
                        onClick = { viewModel.setActivityFilter(ActivityFilter.ALL) },
                        testTag = "filter_all"
                    )
                    FilterChip(
                        label = "Income",
                        selected = currentFilter == ActivityFilter.INCOME,
                        onClick = { viewModel.setActivityFilter(ActivityFilter.INCOME) },
                        testTag = "filter_income"
                    )
                    FilterChip(
                        label = "Expense",
                        selected = currentFilter == ActivityFilter.EXPENSE,
                        onClick = { viewModel.setActivityFilter(ActivityFilter.EXPENSE) },
                        testTag = "filter_expense"
                    )
                }
            }

            // 3. GROUPED TRANSACTION FEED
            val todayItems = filteredTransactions.filter { it.timeLabel.startsWith("Today") || it.timeLabel.startsWith("Just now") }
            if (todayItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Today",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(todayItems, key = { it.id }) { item ->
                    ViNoteTransactionTile(
                        transaction = item,
                        onClick = {
                            viewModel.selectTransactionDetail(item)
                        }
                    )
                }
            }

            val earlierItems = filteredTransactions.filter { !it.timeLabel.startsWith("Today") && !it.timeLabel.startsWith("Just now") }
            if (earlierItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Earlier",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(earlierItems, key = { it.id }) { item ->
                    ViNoteTransactionTile(
                        transaction = item,
                        onClick = {
                            viewModel.selectTransactionDetail(item)
                        }
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NotaAvatar(
                            size = 80.dp,
                            eyeState = NotaEyeState.CURIOUS,
                            baseColor = notaConfig.baseColor,
                            accessory = notaConfig.accessory
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching transactions found" else "No transactions recorded yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ViNoteTextSecondary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp)) // bottom navigation space
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = "Clear All Transactions?",
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently delete your entire transaction history locally and from Cloud Firestore sync.",
                    color = ViNoteTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearAllTransactions()
                    },
                    modifier = Modifier.testTag("confirm_clear_all_btn")
                ) {
                    Text("Clear All", color = ViNoteError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = ViNoteTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SpendingTrendsCard(
    report: SpendingTrendReport,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    notaConfig: com.vinote.data.model.NotaConfig
) {
    var selectedPoint by remember { mutableStateOf<DailyTrendPoint?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x14171827),
                spotColor = Color(0x1F171827)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(ViNoteSurfaceContainerLowest)
            .border(1.dp, Color(0x12000000), RoundedCornerShape(28.dp))
            .padding(18.dp)
    ) {
        Column {
            // Header Row: Title & Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ViNotePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "Trends",
                            tint = ViNotePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Spending Trends",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNoteTextPrimary
                        )
                        Text(
                            text = "Last 7 Days Breakdown",
                            fontSize = 12.sp,
                            color = ViNoteTextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Trend status pill badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (report.isTrendingLower) ViNoteMintSuccess.copy(alpha = 0.15f) else ViNoteError.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (report.isTrendingLower) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                contentDescription = "Trend",
                                tint = if (report.isTrendingLower) Color(0xFF0F6E3B) else ViNoteError,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (report.isTrendingLower) "On Track" else "High Spend",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (report.isTrendingLower) Color(0xFF0F6E3B) else ViNoteError
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = ViNoteTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(ViNoteSurfaceContainerLow)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL 7-DAY SPENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextSecondary,
                                letterSpacing = 0.06.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FormatUtils.formatRupiah(report.totalExpensePeriod),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ViNoteTextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(Color(0x1A000000))
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "DAILY AVERAGE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextSecondary,
                                letterSpacing = 0.06.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${FormatUtils.formatRupiah(report.averageDailyExpense)} / day",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNotePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Interactive 7-Day Bar Chart
                    Text(
                        text = selectedPoint?.let { "${it.dateLabel} (${it.dayLabel}): ${FormatUtils.formatRupiah(it.amount)}" }
                            ?: "DAILY SPENDING DISTRIBUTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPoint != null) ViNotePrimary else ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
                    )

                    // Daily Bars Container
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        report.dailyTrendPoints.forEach { point ->
                            val isSelected = selectedPoint == point
                            DailyBarColumn(
                                point = point,
                                isSelected = isSelected,
                                onClick = {
                                    selectedPoint = if (selectedPoint == point) null else point
                                }
                            )
                        }
                    }

                    // Peak Day Chip if available
                    if (report.peakDayAmount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(end = 4.dp)
                        ) {
                            Text(
                                text = "Peak day: ${report.peakExpenseDay} (${FormatUtils.formatRupiah(report.peakDayAmount)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ViNoteTextSecondary
                            )
                        }
                    }

                    // Top Categories Progress Breakdown
                    if (report.categoryTrends.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "TOP SPENDING CATEGORIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNoteTextSecondary,
                            letterSpacing = 0.05.sp,
                            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
                        )

                        report.categoryTrends.forEach { cat ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(cat.colorHex)))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.category,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ViNoteTextPrimary
                                        )
                                    }

                                    Text(
                                        text = "${FormatUtils.formatRupiah(cat.amount)} (${cat.percentage}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(ViNoteSurfaceContainerLow)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = (cat.percentage / 100f).coerceIn(0.05f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(android.graphics.Color.parseColor(cat.colorHex)))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // NoTa Advice Bubble
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(ViNoteSecondaryFixed.copy(alpha = 0.35f))
                            .border(1.dp, ViNotePrimary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NotaAvatar(
                                size = 44.dp,
                                eyeState = if (report.isTrendingLower) NotaEyeState.HAPPY else NotaEyeState.CURIOUS,
                                baseColor = notaConfig.baseColor,
                                accessory = notaConfig.accessory,
                                isAnimated = false
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = report.notaTrendInsight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ViNoteTextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBarColumn(
    point: DailyTrendPoint,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val barHeightRatio by animateFloatAsState(
        targetValue = point.relativeHeight,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "bar_height"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(36.dp)
            .clickable(onClick = onClick)
    ) {
        // Vertical Bar Slot (Max 78dp)
        Box(
            modifier = Modifier
                .height(78.dp)
                .width(18.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(50))
                    .background(ViNoteSurfaceContainerLow)
            )

            // Active bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = barHeightRatio)
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            isSelected -> Brush.verticalGradient(listOf(ViNotePrimary, ViNotePrimary))
                            point.isToday -> Brush.verticalGradient(
                                colors = listOf(ViNotePrimary, Color(0xFF003893))
                            )
                            point.amount > 0 -> Brush.verticalGradient(
                                colors = listOf(ViNoteSecondaryFixed, ViNotePrimary.copy(alpha = 0.7f))
                            )
                            else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day Label
        Text(
            text = point.dayLabel.take(3),
            fontSize = 11.sp,
            fontWeight = if (point.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (point.isToday || isSelected) ViNotePrimary else ViNoteTextSecondary
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ViNotePrimary else Color.Transparent)
            .then(
                if (!selected) {
                    Modifier.border(1.5.dp, ViNotePrimary, RoundedCornerShape(50))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 9.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else ViNotePrimary
        )
    }
}

