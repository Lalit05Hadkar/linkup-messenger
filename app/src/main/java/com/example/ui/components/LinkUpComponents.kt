package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserIdStatus
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.HomeTab

@Composable
fun LinkUpAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    imageUrl: String = "",
    isOnline: Boolean = false,
    showOnlineDot: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()

        // Avatar circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = name,
                    tint = AccentBlue,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }

        // Online status dot
        if (showOnlineDot && isOnline) {
            val dotSize = (size * 0.28f).coerceIn(10.dp, 16.dp)
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun StatusChip(
    status: UserIdStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        UserIdStatus.EMPTY -> {}
        UserIdStatus.CHECKING -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = AccentBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Checking...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        UserIdStatus.AVAILABLE -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SuccessGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Available",
                    tint = SuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Available",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
            }
        }
        UserIdStatus.TAKEN -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DangerRed.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Taken",
                    tint = DangerRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Taken",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )
            }
        }
        UserIdStatus.INVALID -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DangerRed.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Invalid",
                    tint = DangerRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Invalid format",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )
            }
        }
    }
}

@Composable
fun LinkUpBottomNav(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    unreadMessagesCount: Int = 0,
    pendingRequestsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                title = "Chats",
                icon = Icons.Outlined.ChatBubbleOutline,
                activeIcon = Icons.AutoMirrored.Filled.Chat,
                isSelected = currentTab == HomeTab.CHATS,
                badgeCount = unreadMessagesCount,
                onClick = { onTabSelected(HomeTab.CHATS) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_chats")
            )

            BottomNavItem(
                title = "Friends",
                icon = Icons.Outlined.PeopleOutline,
                activeIcon = Icons.Outlined.PeopleOutline,
                isSelected = currentTab == HomeTab.FRIENDS,
                badgeCount = pendingRequestsCount,
                onClick = { onTabSelected(HomeTab.FRIENDS) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_friends")
            )

            BottomNavItem(
                title = "Search",
                icon = Icons.Outlined.Search,
                activeIcon = Icons.Outlined.Search,
                isSelected = currentTab == HomeTab.SEARCH,
                onClick = { onTabSelected(HomeTab.SEARCH) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_search")
            )

            BottomNavItem(
                title = "Profile",
                icon = Icons.Outlined.PersonOutline,
                activeIcon = Icons.Default.Person,
                isSelected = currentTab == HomeTab.PROFILE,
                onClick = { onTabSelected(HomeTab.PROFILE) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_profile")
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    title: String,
    icon: ImageVector,
    activeIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pill background for active tab matching PDF design
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isSelected) AccentBlue.copy(alpha = 0.14f) else Color.Transparent
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                BadgedBox(
                    badge = {
                        if (badgeCount > 0) {
                            Badge(
                                containerColor = AccentBlue,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isSelected) activeIcon else icon,
                        contentDescription = title,
                        tint = if (isSelected) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
