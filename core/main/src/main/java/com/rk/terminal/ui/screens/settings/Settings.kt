package com.rk.terminal.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.components.compose.preferences.base.PreferenceLayout
import com.rk.components.compose.preferences.base.PreferenceTemplate
import com.rk.resources.strings
import com.rk.libcommons.ShizukuHelper
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.components.SettingsToggle
import com.rk.terminal.ui.routes.MainActivityRoutes
import androidx.core.net.toUri


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    title: @Composable () -> Unit,
    description: @Composable () -> Unit = {},
    startWidget: (@Composable () -> Unit)? = null,
    endWidget: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    PreferenceTemplate(
        modifier = modifier
            .combinedClickable(
                enabled = isEnabled,
                indication = ripple(),
                interactionSource = interactionSource,
                onClick = onClick
            ),
        contentModifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp)
            .padding(start = 16.dp),
        title = title,
        description = description,
        startWidget = startWidget,
        endWidget = endWidget,
        applyPaddings = false
    )

}


object WorkingMode{
    const val UBUNTU = 0
    const val ANDROID = 1
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(modifier: Modifier = Modifier,navController: NavController,mainActivity: MainActivity) {
    val context = LocalContext.current
    var selectedOption by remember { mutableIntStateOf(Settings.working_Mode) }

    PreferenceLayout(label = stringResource(strings.settings)) {
        PreferenceGroup(heading = "Default Working mode") {

            SettingsCard(
                title = { Text("Ubuntu") },
                description = {Text("Ubuntu 24.04 LTS")},
                startWidget = {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selectedOption == WorkingMode.UBUNTU,
                        onClick = {
                            selectedOption = WorkingMode.UBUNTU
                            Settings.working_Mode = selectedOption
                        })
                },
                onClick = {
                    selectedOption = WorkingMode.UBUNTU
                    Settings.working_Mode = selectedOption
                })


            SettingsCard(
                title = { Text("Android") },
                description = {Text("VS Mobile Android shell")},
                startWidget = {
                    RadioButton(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            ,
                        selected = selectedOption == WorkingMode.ANDROID,
                        onClick = {
                            selectedOption = WorkingMode.ANDROID
                            Settings.working_Mode = selectedOption
                        })
                },
                onClick = {
                    selectedOption = WorkingMode.ANDROID
                    Settings.working_Mode = selectedOption
                })
        }

        PreferenceGroup(heading = "Default Shell") {
            var selectedShell by remember { mutableIntStateOf(Settings.default_shell) }

            SettingsCard(
                title = { Text("Bash") },
                description = {Text("Bourne Again Shell")},
                startWidget = {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selectedShell == 0,
                        onClick = {
                            selectedShell = 0
                            Settings.default_shell = selectedShell
                        })
                },
                onClick = {
                    selectedShell = 0
                    Settings.default_shell = selectedShell
                })

            SettingsCard(
                title = { Text("Zsh") },
                description = {Text("Z Shell with Oh My Zsh")},
                startWidget = {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selectedShell == 1,
                        onClick = {
                            selectedShell = 1
                            Settings.default_shell = selectedShell
                        })
                },
                onClick = {
                    selectedShell = 1
                    Settings.default_shell = selectedShell
                })
        }




        PreferenceGroup {
            SettingsToggle(
                label = "Customizations",
                showSwitch = false,
                default = false,
                sideEffect = {
                   navController.navigate(MainActivityRoutes.Customization.route)
            }, endWidget = {
                Icon(imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null,modifier = Modifier.padding(16.dp))
            })
        }

        PreferenceGroup(heading = "WebView Settings") {
            var virtualMouseEnabled by remember { mutableStateOf(Settings.webview_virtual_mouse_enabled) }
            var cursorScale by remember { mutableFloatStateOf(Settings.webview_cursor_scale) }
            var zoomLevel by remember { mutableIntStateOf(Settings.webview_zoom_level) }
            var webviewUrl by remember { mutableStateOf(Settings.webview_url) }
            var webviewPort by remember { mutableIntStateOf(Settings.webview_port) }
            var showUrlDialog by remember { mutableStateOf(false) }
            var showPortDialog by remember { mutableStateOf(false) }
            
            SettingsCard(
                title = { Text("Virtual Mouse") },
                description = { Text(if (virtualMouseEnabled) "Enabled" else "Disabled") },
                onClick = {
                    virtualMouseEnabled = !virtualMouseEnabled
                    Settings.webview_virtual_mouse_enabled = virtualMouseEnabled
                }
            )
            
            SettingsCard(
                title = { Text("Server URL") },
                description = { Text("Current: $webviewUrl") },
                onClick = { showUrlDialog = true }
            )
            
            if (showUrlDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showUrlDialog = false },
                    title = { Text("Server URL") },
                    text = {
                        androidx.compose.foundation.layout.Column {
                            Text("Enter server URL (without http://)")
                            androidx.compose.material3.OutlinedTextField(
                                value = webviewUrl,
                                onValueChange = { webviewUrl = it },
                                placeholder = { Text("localhost") }
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            Settings.webview_url = webviewUrl
                            showUrlDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { 
                            webviewUrl = Settings.webview_url
                            showUrlDialog = false 
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            SettingsCard(
                title = { Text("Server Port") },
                description = { Text("Current: $webviewPort") },
                onClick = { showPortDialog = true }
            )
            
            if (showPortDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showPortDialog = false },
                    title = { Text("Server Port") },
                    text = {
                        androidx.compose.foundation.layout.Column {
                            Text("Enter server port number")
                            androidx.compose.material3.OutlinedTextField(
                                value = webviewPort.toString(),
                                onValueChange = { 
                                    it.toIntOrNull()?.let { port -> webviewPort = port }
                                },
                                placeholder = { Text("6862") }
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            Settings.webview_port = webviewPort
                            showPortDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { 
                            webviewPort = Settings.webview_port
                            showPortDialog = false 
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            SettingsCard(
                title = { Text("Cursor Scale") },
                description = { Text("Current: ${String.format("%.1f", cursorScale)}x") },
                onClick = {}
            )
            
            androidx.compose.material3.Slider(
                value = cursorScale,
                onValueChange = { 
                    cursorScale = it
                    Settings.webview_cursor_scale = it
                },
                valueRange = 0.5f..3.0f,
                steps = 24,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            SettingsCard(
                title = { Text("Zoom Level") },
                description = { Text("Current: ${zoomLevel}%") },
                onClick = {}
            )
            
            androidx.compose.material3.Slider(
                value = zoomLevel.toFloat(),
                onValueChange = { 
                    zoomLevel = it.toInt()
                    Settings.webview_zoom_level = zoomLevel
                },
                valueRange = 100f..300f,
                steps = 39,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Desktop Environment settings
        var desktopEnabled by remember { mutableStateOf(Settings.desktop_enabled) }
        PreferenceGroup(heading = "Desktop Environment") {
            SettingsToggle(
                label = "Enable Desktop Environment",
                description = "Show Desktop session tab (requires Xfce4 + x11vnc)",
                showSwitch = true,
                default = desktopEnabled,
                sideEffect = {
                    desktopEnabled = it
                    Settings.desktop_enabled = it
                    // Ask to restart app
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Restart Required")
                        .setMessage("Please restart VS Mobile for changes to take effect.")
                        .setPositiveButton("Restart") { _, _ ->
                            val pm = context.packageManager
                            val intent = pm.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            context.startActivity(intent)
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                        .setNegativeButton("Later", null)
                        .show()
                }
            )
            if (desktopEnabled) {
                var vncHost by remember { mutableStateOf(Settings.vnc_host) }
                var vncPort by remember { mutableStateOf(Settings.vnc_port.toString()) }
                var vncPass by remember { mutableStateOf(Settings.vnc_password) }
                var showDialog by remember { mutableStateOf<String?>(null) }
                var dialogValue by remember { mutableStateOf("") }

                // Dialog for editing text values
                if (showDialog != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showDialog = null },
                        title = { Text(showDialog!!) },
                        text = {
                            androidx.compose.material3.OutlinedTextField(
                                value = dialogValue,
                                onValueChange = { dialogValue = it },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                when (showDialog) {
                                    "VNC Host" -> { vncHost = dialogValue; Settings.vnc_host = dialogValue }
                                    "VNC Port" -> { vncPort = dialogValue; Settings.vnc_port = dialogValue.toIntOrNull() ?: 5905 }
                                    "VNC Password" -> { vncPass = dialogValue; Settings.vnc_password = dialogValue }
                                }
                                showDialog = null
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showDialog = null }) { Text("Cancel") }
                        }
                    )
                }

                SettingsToggle(label = "VNC Host", description = vncHost, showSwitch = false, default = false,
                    sideEffect = { dialogValue = vncHost; showDialog = "VNC Host" })
                SettingsToggle(label = "VNC Port", description = vncPort, showSwitch = false, default = false,
                    sideEffect = { dialogValue = vncPort; showDialog = "VNC Port" })
                SettingsToggle(label = "VNC Password", description = "••••••", showSwitch = false, default = false,
                    sideEffect = { dialogValue = vncPass; showDialog = "VNC Password" })
            }
        }

        PreferenceGroup {
            SettingsToggle(
                label = "Shizuku Integration",
                description = "Tap to request elevated permissions",
                showSwitch = false,
                default = false,
                sideEffect = {
                    ShizukuHelper.requestPermission()
                })

            SettingsToggle(
                label = "SECCOMP",
                description = "fix operation not permitted error",
                showSwitch = true,
                default = Settings.seccomp,
                sideEffect = {
                    Settings.seccomp = it
                })

            SettingsToggle(
                label = "All file access",
                description = "enable access to /sdcard and /storage",
                showSwitch = false,
                default = false,
                sideEffect = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        runCatching {
                            val intent = Intent(
                                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                            context.startActivity(intent)
                        }.onFailure {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        }
                    }else{
                        val intent = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri()
                        )
                        context.startActivity(intent)
                    }

                })

        }
    }
}