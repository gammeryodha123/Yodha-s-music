package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OSClient(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val platform: String,
    val repoUrl: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceScreen(
    onNavigateToSearchWithSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expandedClientId by remember { mutableStateOf<String?>(null) }

    val clients = remember {
        listOf(
            OSClient(
                id = "peertube",
                name = "PeerTube",
                subtitle = "Decentralized Federated P2P Network",
                description = "A free, decentralized, and federated video platform powered by WebTorrent and ActivityPub. Peer-to-peer streaming reduces server bandwidth by sharing playback among viewers dynamically.",
                platform = "Federated Web / P2P Streaming",
                repoUrl = "https://joinpeertube.org",
                icon = Icons.Default.Cloud,
                color = Color(0xFFF1680D)
            ),
            OSClient(
                id = "newpipe",
                name = "NewPipe",
                subtitle = "Lightweight Privacy Client",
                description = "A lightweight Android app designed for privacy, letting you play videos in the background and download media without logging into a Google account.",
                platform = "Android (Mobile)",
                repoUrl = "https://newpipe.net",
                icon = Icons.Default.Smartphone,
                color = Color(0xFFCA2026)
            ),
            OSClient(
                id = "freetube",
                name = "FreeTube",
                subtitle = "Private Desktop YouTube Client",
                description = "A private YouTube client built for desktop operating systems like Windows, Mac, and Linux, storing all user data locally to maximize security.",
                platform = "Desktop (Win/Mac/Linux)",
                repoUrl = "https://freetubeapp.io",
                icon = Icons.Default.Computer,
                color = Color(0xFF33B5E5)
            ),
            OSClient(
                id = "libretube",
                name = "LibreTube",
                subtitle = "Piped-powered Alternative Client",
                description = "An alternative Android client that connects via Piped proxies to provide a tracking-free experience and bypass geo-restrictions.",
                platform = "Android (Mobile)",
                repoUrl = "https://github.com/LibreTube/LibreTube",
                icon = Icons.Default.SettingsCell,
                color = Color(0xFF8E44AD)
            ),
            OSClient(
                id = "invidious",
                name = "Invidious",
                subtitle = "Privacy Web Alternative Frontend",
                description = "A lightweight, open-source web frontend for YouTube that protects your privacy, blocks ads, and doesn't require a Google account to view feeds.",
                platform = "Web Frontend",
                repoUrl = "https://invidious.io",
                icon = Icons.Default.Language,
                color = Color(0xFF2C3E50)
            ),
            OSClient(
                id = "flow",
                name = "Flow",
                subtitle = "Modern Client with Local AI Recs",
                description = "A modern client for Android and desktop that features a local, on-device recommendation engine along with SponsorBlock integration to auto-skip sponsor segments.",
                platform = "Android & Desktop",
                repoUrl = "https://github.com/Flow-App/Flow",
                icon = Icons.Default.PlayCircleOutline,
                color = Color(0xFF2ECC71)
            )
        )
    }

    val launchUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handled gracefully
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Header Gradient Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "Privacy Hub",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Decentralized & Open Source Media Networks",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Federated PeerTube Highlight Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1680D).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .testTag("peertube_featured_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1680D).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "PeerTube Icon",
                                    tint = Color(0xFFF1680D)
                                )
                            }
                            Column {
                                Text(
                                    text = "PeerTube Federation",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Join the Decentralized Video Era",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Unlike central platforms, PeerTube is owned by everyone. It connects independent video servers across the world using open protocols, enabling community-run video sharing without tracking or corporate oversight.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToSearchWithSource("PeerTube") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF1680D),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.5f).height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search PeerTube icon",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search PeerTube", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { launchUrl("https://joinpeertube.org") },
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(listOf(Color(0xFFF1680D), Color(0xFFF1680D)))
                                ),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Join Now", fontSize = 12.sp, color = Color(0xFFF1680D), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Top YouTube Clients Title
            item {
                Text(
                    text = "Top Open-Source Clients",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Clients List
            items(clients) { client ->
                val isExpanded = expandedClientId == client.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clickable {
                            expandedClientId = if (isExpanded) null else client.id
                        }
                        .testTag("client_card_${client.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(client.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = client.icon,
                                    contentDescription = client.name,
                                    tint = client.color
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = client.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = client.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(client.color.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (client.id == "peertube") "Federated" else "Privacy",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = client.color
                                )
                            }
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = client.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Platform: ${client.platform}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                TextButton(
                                    onClick = { launchUrl(client.repoUrl) },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Visit Official Project", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Forward Icon",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Extra padding for bottom player
            }
        }
    }
}
