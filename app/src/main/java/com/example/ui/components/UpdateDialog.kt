package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.update.UpdateState

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onAllowPermission: () -> Unit,
    onRetryInstall: () -> Unit,
    onRetryDownload: () -> Unit
) {
    when (updateState) {
        is UpdateState.Idle -> {
            // Nothing to show
        }
        is UpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Text(
                        text = stringResource(R.string.update_checking),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                },
                confirmButton = { }
            )
        }
        is UpdateState.Available -> {
            val info = updateState.updateInfo
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.update_available),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Anshu Mock v${info.versionName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = stringResource(R.string.update_whats_new),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = info.releaseNotes.ifBlank { "Performance improvements and bug fixes." },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.testTag("update_now_button")
                    ) {
                        Text(stringResource(R.string.update_now))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("update_later_button")
                    ) {
                        Text(stringResource(R.string.update_later))
                    }
                }
            )
        }
        is UpdateState.Downloading -> {
            val info = updateState.updateInfo
            val progress = updateState.progress
            AlertDialog(
                onDismissRequest = { },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloading",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.update_downloading),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Anshu Mock v${info.versionName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$progress%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = { }
            )
        }
        is UpdateState.PermissionRequired -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Permission Required",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = "Permission Required",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.update_allow_install),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = onAllowPermission) {
                        Text(stringResource(R.string.update_allow))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        is UpdateState.Downloaded -> {
            val info = updateState.updateInfo
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = "Ready to Install",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Anshu Mock v${info.versionName} has been downloaded and is ready to install.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = onRetryInstall) {
                        Text("Install Now")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.update_later))
                    }
                }
            )
        }
        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.something_went_wrong),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = updateState.message.ifBlank { stringResource(R.string.update_failed) },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    if (updateState.updateInfo != null) {
                        Button(onClick = onRetryDownload) {
                            Text(stringResource(R.string.update_retry))
                        }
                    } else {
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.close))
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Up to Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.update_current_version),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.update_latest_version),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.update_current_version)}: ${com.example.BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }
    }
}
