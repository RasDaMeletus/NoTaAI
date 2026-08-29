package com.vinote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNotePrimaryContainer
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextSecondary

enum class ViNoteNavTab {
    HOME,
    ACTIVITY,
    NOTA,
    GOALS,
    ME
}

@Composable
fun ViNoteBottomNavigation(
    currentTab: ViNoteNavTab,
    onTabSelected: (ViNoteNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = Color(0x1F171827),
                spotColor = Color(0x1F171827)
            )
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(ViNoteSurfaceContainerLowest.copy(alpha = 0.95f))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Home",
                selected = currentTab == ViNoteNavTab.HOME,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                onClick = { onTabSelected(ViNoteNavTab.HOME) },
                testTag = "nav_home"
            )
            NavItem(
                label = "Activity",
                selected = currentTab == ViNoteNavTab.ACTIVITY,
                selectedIcon = Icons.Filled.Insights,
                unselectedIcon = Icons.Outlined.Insights,
                onClick = { onTabSelected(ViNoteNavTab.ACTIVITY) },
                testTag = "nav_activity"
            )
            NavItem(
                label = "Nota",
                selected = currentTab == ViNoteNavTab.NOTA,
                selectedIcon = Icons.Filled.Face,
                unselectedIcon = Icons.Outlined.Face,
                onClick = { onTabSelected(ViNoteNavTab.NOTA) },
                testTag = "nav_nota"
            )
            NavItem(
                label = "Goals",
                selected = currentTab == ViNoteNavTab.GOALS,
                selectedIcon = Icons.Filled.TrackChanges,
                unselectedIcon = Icons.Outlined.TrackChanges,
                onClick = { onTabSelected(ViNoteNavTab.GOALS) },
                testTag = "nav_goals"
            )
            NavItem(
                label = "Me",
                selected = currentTab == ViNoteNavTab.ME,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                onClick = { onTabSelected(ViNoteNavTab.ME) },
                testTag = "nav_me"
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val pillBgColor by animateColorAsState(
        targetValue = if (selected) ViNotePrimaryContainer else Color.Transparent,
        animationSpec = tween(200),
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else ViNoteTextSecondary,
        animationSpec = tween(200),
        label = "content_color"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(pillBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                letterSpacing = 0.05.sp
            )
        }
    }
}
