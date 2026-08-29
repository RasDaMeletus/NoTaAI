package com.vinote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.GoalItem
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.ViNoteCard
import com.vinote.ui.components.ViNoteProgressBar
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNoteOnPrimaryContainer
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNotePrimaryContainer
import com.vinote.ui.theme.ViNoteSecondary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSoftPink
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTertiaryContainer
import com.vinote.ui.theme.ViNoteTertiaryFixed
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.viewmodel.ViNoteViewModel

@Composable
fun GoalsScreen(
    viewModel: ViNoteViewModel,
    onNavigateToCreateGoal: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.allGoals.collectAsState()
    val totalSavings = goals.sumOf { it.currentAmount }

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
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ViNoteSoftPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✦ ✦",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNotePrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "ViNote",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNotePrimary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ViNoteSurfaceContainerLow)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = ViNoteTextPrimary
                        )
                    }
                }
            }

            // Total Savings Hero Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color(0x2A0057C2)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(ViNotePrimaryContainer)
                        .padding(24.dp)
                ) {
                    // Decorative glow
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-40).dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    )

                    Column {
                        Text(
                            text = "Total Savings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = FormatUtils.formatRupiah(totalSavings),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.02).sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // + New Goal button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .clickable { onNavigateToCreateGoal() }
                                .padding(horizontal = 22.dp, vertical = 12.dp)
                                .testTag("new_goal_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Goal",
                                    tint = ViNotePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "New Goal",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNotePrimary
                                )
                            }
                        }
                    }
                }
            }

            // Goals List
            items(goals) { goal ->
                GoalCard(
                    goal = goal,
                    onAddSavings = {
                        viewModel.addSavingsToGoal(goal, 50000L)
                    }
                )
            }

            // Nota Companion Encouragement Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(ViNoteSoftPink.copy(alpha = 0.35f))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ViNoteSoftPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✦ ✦",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNotePrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = "Kamu semakin dekat dengan impianmu! 🎉",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ViNoteTextPrimary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // clearance for bottom navigation
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalItem,
    onAddSavings: () -> Unit
) {
    val (iconVector, iconBg, iconTint, progressFill) = when (goal.iconName) {
        "flight_takeoff" -> Quadruple(
            Icons.Default.FlightTakeoff,
            ViNoteTertiaryFixed,
            ViNoteTertiaryContainer,
            ViNoteTertiaryContainer
        )
        "school" -> Quadruple(
            Icons.Default.School,
            ViNoteMintSuccess.copy(alpha = 0.2f),
            ViNoteMintSuccess,
            ViNoteMintSuccess
        )
        "laptop" -> Quadruple(
            Icons.Default.Laptop,
            ViNoteSecondaryFixed,
            ViNotePrimary,
            ViNotePrimary
        )
        else -> Quadruple(
            Icons.Default.Headphones,
            ViNoteSecondaryFixed,
            ViNotePrimary,
            ViNotePrimary
        )
    }

    ViNoteCard(
        padding = 18.dp,
        onClick = onAddSavings
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = goal.title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = goal.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                    Text(
                        text = "${FormatUtils.formatShortRupiah(goal.currentAmount)} of ${FormatUtils.formatShortRupiah(goal.targetAmount)}",
                        fontSize = 13.sp,
                        color = ViNoteTextSecondary
                    )
                }
            }

            // Percentage Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(iconBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${goal.progressPercentage}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Bar
        ViNoteProgressBar(
            progress = goal.progressPercentage / 100f,
            fillColor = progressFill
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
