package com.example.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.model.RequestStatus
import com.example.ui.components.LinkUpBottomNav
import com.example.ui.screens.chats.ChatsScreen
import com.example.ui.screens.friends.FriendsScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.search.SearchScreen
import com.example.viewmodel.HomeTab
import com.example.viewmodel.LinkUpViewModel

@Composable
fun HomeScreen(
    viewModel: LinkUpViewModel
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val chats by viewModel.repository.chats.collectAsState()
    val friendRequests by viewModel.repository.friendRequests.collectAsState()
    val currentUser by viewModel.repository.currentUser.collectAsState()

    val myUid = currentUser?.uid ?: ""

    // Calculate unread messages count
    val totalUnread = chats.sumOf { it.unreadCounts[myUid] ?: 0 }

    // Calculate pending requests count
    val pendingRequestsCount = friendRequests.count {
        it.receiverId == myUid && it.status == RequestStatus.PENDING
    }

    BackHandler(enabled = currentTab != HomeTab.CHATS) {
        viewModel.switchTab(HomeTab.CHATS)
    }

    Scaffold(
        bottomBar = {
            LinkUpBottomNav(
                currentTab = currentTab,
                onTabSelected = { viewModel.switchTab(it) },
                unreadMessagesCount = totalUnread,
                pendingRequestsCount = pendingRequestsCount
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(
                targetState = currentTab,
                label = "HomeTabTransition"
            ) { tab ->
                when (tab) {
                    HomeTab.CHATS -> ChatsScreen(viewModel = viewModel)
                    HomeTab.FRIENDS -> FriendsScreen(viewModel = viewModel)
                    HomeTab.SEARCH -> SearchScreen(viewModel = viewModel)
                    HomeTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }
}
