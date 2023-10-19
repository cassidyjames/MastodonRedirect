package dev.zwander.shared.components

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.zwander.shared.data.LinkSelectionData
import dev.zwander.shared.util.collectAvailableDomains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LinkBlocklistLayout(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var filter by remember {
        mutableStateOf("")
    }
    var domains by remember {
        mutableStateOf(listOf<LinkSelectionData>())
    }

    val filteredDomains by remember {
        derivedStateOf {
            if (filter.isBlank()) domains else domains.filter { it.host.contains(filter, true) }
        }
    }

    LaunchedEffect(key1 = null) {
        domains = withContext(Dispatchers.IO) {
            try {
                context.collectAvailableDomains()
            } catch (e: Throwable) {
                Log.e("FediverseRedirect", "Error", e)
                listOf()
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = domains.isEmpty(),
            modifier = Modifier.matchParentSize(),
            label = "domains_crossfade",
        ) { empty ->
            if (empty) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val listState = rememberLazyListState()

                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState,
                    ) {
                        items(items = filteredDomains, key = { it.host }) {
                            LinkBlocklistItem(
                                domain = it,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    SearchBar(
                        text = filter,
                        onTextChange = { filter = it },
                        onScrollToTop = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun LinkBlocklistItem(
    domain: LinkSelectionData,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val blocked by domain.getLinkBlockedStatusAsState()

    TextSwitch(
        text = domain.host,
        checked = !blocked,
        onCheckedChange = {
            scope.launch {
                with (domain) {
                    context.updateLinkBlockedStatus(!it)
                }
            }
        },
        modifier = modifier,
    )
}
