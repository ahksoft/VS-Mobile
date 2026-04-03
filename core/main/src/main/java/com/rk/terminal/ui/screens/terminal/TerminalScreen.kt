package com.rk.terminal.ui.screens.terminal
import com.rk.resources.drawables

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import com.rk.terminal.ui.screens.terminal.virtualkeys.SpecialButton
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.NavController
import com.google.android.material.R
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.libcommons.application
import com.rk.libcommons.child
import com.rk.libcommons.dpToPx
import com.rk.libcommons.localDir
import com.rk.libcommons.pendingCommand
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.routes.MainActivityRoutes
import com.rk.terminal.ui.screens.settings.SettingsCard
import com.rk.terminal.ui.screens.settings.WorkingMode
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysConstants
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysInfo
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysListener
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysView
import com.termux.terminal.TerminalColors
import com.termux.view.TerminalView
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.lang.ref.WeakReference
import java.util.Properties

var terminalView = WeakReference<TerminalView?>(null)
var virtualKeysView = WeakReference<VirtualKeysView?>(null)


var darkText = mutableStateOf(Settings.blackTextColor)
var bitmap = mutableStateOf<ImageBitmap?>(null)

private val file = application!!.filesDir.child("font.ttf")
private var font = (if (file.exists() && file.canRead()){
    Typeface.createFromFile(file)
}else{
    try {
        Typeface.createFromAsset(application!!.assets, "fonts/MesloLGS_NF_Regular.ttf")
    } catch (e: Exception) {
        Typeface.MONOSPACE
    }
})

suspend fun setFont(typeface: Typeface) = withContext(Dispatchers.Main){
    font = typeface
    terminalView.get()?.apply {
        setTypeface(typeface)
        onScreenUpdated()
    }
}

inline fun getViewColor(): Int{
    return if (darkText.value){
        Color.BLACK
    }else{
        Color.WHITE
    }
}

inline fun getComposeColor():androidx.compose.ui.graphics.Color{
    return if (darkText.value){
        androidx.compose.ui.graphics.Color.Black
    }else{
        androidx.compose.ui.graphics.Color.White
    }
}

var showToolbar = mutableStateOf(Settings.toolbar)
var showVirtualKeys = mutableStateOf(Settings.virtualKeys)
var showHorizontalToolbar = mutableStateOf(Settings.toolbar)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    mainActivityActivity: MainActivity,
    navController: NavController
) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    var webViewReloadTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit){
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){
            if (context.filesDir.child("background").exists().not()){
                darkText.value = !isDarkMode
            }else if (bitmap.value == null){
                val fullBitmap = BitmapFactory.decodeFile(context.filesDir.child("background").absolutePath)?.asImageBitmap()
                if (fullBitmap != null) bitmap.value = fullBitmap
            }
        }


        scope.launch(Dispatchers.Main){
            virtualKeysView.get()?.apply {
                virtualKeysViewClient =
                    terminalView.get()?.mTermSession?.let {
                        VirtualKeysListener(
                            it
                        )
                    }

                buttonTextColor = getViewColor()


                reload(
                    VirtualKeysInfo(
                        VIRTUAL_KEYS,
                        "",
                        VirtualKeysConstants.CONTROL_CHARS_ALIASES
                    )
                )
            }

            terminalView.get()?.apply {
                onScreenUpdated()


                mEmulator?.mColors?.mCurrentColors?.apply {
                    set(256, getViewColor())
                    set(258, getViewColor())
                }
            }
        }


    }

    Box {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        val drawerWidth = (screenWidthDp * 0.84).dp
        var showAddDialog by remember { mutableStateOf(false) }

        BackHandler(enabled = drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }
        
        BackHandler(enabled = drawerState.isClosed) {
            scope.launch {
                drawerState.open()
            }
        }

        if (drawerState.isClosed){
            SetStatusBarTextColor(isDarkIcons = darkText.value)
        }else{
            SetStatusBarTextColor(isDarkIcons = !isDarkMode)
        }

        if (showAddDialog){
            BasicAlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                }
            ) {

                fun createSession(workingMode:Int){
                    fun generateUniqueString(existingStrings: List<String>): String {
                        var index = 1
                        var newString: String

                        do {
                            newString = "main$index"
                            index++
                        } while (newString in existingStrings)

                        return newString
                    }

                    val sessionId = generateUniqueString(mainActivityActivity.sessionBinder!!.getService().sessionList.keys.toList())

                    terminalView.get()
                        ?.let {
                            val client = TerminalBackEnd(it, mainActivityActivity)
                            mainActivityActivity.sessionBinder!!.createSession(
                                sessionId,
                                client,
                                mainActivityActivity, workingMode = workingMode
                            )
                        }


                    changeSession(mainActivityActivity, session_id = sessionId)
                }


                PreferenceGroup {
                    SettingsCard(
                        title = { Text("Ubuntu") },
                        description = {Text("Ubuntu 24.04 LTS")},
                        onClick = {
                           createSession(workingMode = WorkingMode.UBUNTU)
                            showAddDialog = false
                        })

                    SettingsCard(
                        title = { Text("Android") },
                        description = {Text("VS Mobile Android shell")},
                        onClick = {
                            createSession(workingMode = WorkingMode.ANDROID)
                            showAddDialog = false
                        })
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen || !(showToolbar.value && (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE || showHorizontalToolbar.value)),
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(drawerWidth)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Session",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Row {
                                val keyboardController = LocalSoftwareKeyboardController.current
                                
                                IconButton(onClick = {
                                    val intent = android.content.Intent(mainActivityActivity, com.rk.terminal.ui.activities.BrowserActivity::class.java)
                                    intent.putExtra("url", "https://github.com/ahksoft")
                                    mainActivityActivity.startActivity(intent)
                                }) {
                                    Icon(
                                        painter = painterResource(id = com.rk.terminal.R.drawable.ic_web),
                                        contentDescription = "Browser"
                                    )
                                }
                                
                                IconButton(onClick = {
                                    navController.navigate(MainActivityRoutes.Settings.route)
                                    keyboardController?.hide()
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = null
                                    )
                                }

                                IconButton(onClick = {
                                    showAddDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null
                                    )
                                }

                            }


                        }

                        mainActivityActivity.sessionBinder?.getService()?.sessionList?.keys?.toList()?.let { sessions ->
                            // Sort so webview is always first, desktop second
                            val sortedSessions = sessions.sortedBy { when(it) { "webview" -> 0; "desktop" -> 1; else -> 2 } }
                            LazyColumn {
                                items(sortedSessions) { session_id ->
                                    SelectableCard(
                                        selected = session_id == mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.first,
                                        onSelect = {
                                            changeSession(
                                                mainActivityActivity,
                                                session_id
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (session_id) {
                                                    "webview" -> "VS Code"
                                                    "desktop" -> "Desktop"
                                                    else -> session_id
                                                },
                                                style = MaterialTheme.typography.bodyLarge
                                            )

                                            Spacer(modifier = Modifier.weight(1f))

                                            // Show reload button for webview and desktop
                                            if (session_id == "webview" || session_id == "desktop") {
                                                IconButton(
                                                    onClick = {
                                                        webViewReloadTrigger++
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Refresh,
                                                        contentDescription = "Reload",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            // Hide delete button for webview, desktop and current session
                                            if (session_id != "webview" && session_id != "desktop" && session_id != mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.first) {

                                                IconButton(
                                                    onClick = {
                                                        println(session_id)
                                                        mainActivityActivity.sessionBinder?.terminateSession(
                                                            session_id
                                                        )
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }

                    }
                }

            },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                        BackgroundImage()
                        val color = getComposeColor()
                        Column {

                            fun getNameOfWorkingMode(workingMode:Int?):String{
                                return when(workingMode){
                                    0 -> "UBUNTU".lowercase()
                                    1 -> "ANDROID".lowercase()
                                    null -> "null"
                                    else -> "unknown"
                                }
                            }


                            if (false) {
                                // TopAppBar removed
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                                    ),
                                    title = {
                                        Column {
                                            Text(text = "Terminal",color = color)
                                            Text(style = MaterialTheme.typography.bodySmall,text = mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.first + " (${getNameOfWorkingMode(mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.second)})",color = color)
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            scope.launch { drawerState.open() }
                                        }) {
                                            Icon(Icons.Default.Menu, null, tint = color)
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = {
                                            val intent = android.content.Intent(mainActivityActivity, com.rk.terminal.ui.activities.webview.WebViewActivity::class.java)
                                            intent.putExtra("url", "http://${com.rk.settings.Settings.webview_url}:${com.rk.settings.Settings.webview_port}")
                                            mainActivityActivity.startActivity(intent)
                                        }) {
                                            Icon(
                                                painter = painterResource(id = com.rk.terminal.R.drawable.ic_web),
                                                contentDescription = "Open WebView",
                                                tint = color
                                            )
                                        }
                                        IconButton(onClick = {
                                            showAddDialog = true
                                        }) {
                                            Icon(Icons.Default.Add,null, tint = color)
                                        }
                                    }
                                )
                            }

                            val density = LocalDensity.current
                            Column(modifier = Modifier.imePadding().navigationBarsPadding().padding(top = if (showToolbar.value){0.dp}else{
                                with(density){
                                    TopAppBarDefaults.windowInsets.getTop(density).toDp()
                                }
                            })) {
                                // Check if current session is webview or desktop
                                val isWebViewSession = mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.first == "webview"
                                val isDesktopSession = mainActivityActivity.sessionBinder?.getService()?.currentSession?.value?.first == "desktop"

                                // Launch DesktopActivity when desktop session is selected
                                LaunchedEffect(isDesktopSession) {
                                    if (isDesktopSession) {
                                        mainActivityActivity.startActivity(
                                            android.content.Intent(
                                                mainActivityActivity,
                                                com.rk.terminal.ui.activities.desktop.DesktopActivity::class.java
                                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }

                                // Always keep WebView in composition, just hide it
                                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                    // VS Code WebView
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .alpha(if (isWebViewSession) 1f else 0f)
                                            .zIndex(if (isWebViewSession) 1f else -1f)
                                    ) {
                                        androidx.compose.runtime.key("webview_session") {
                                            WebViewSession(
                                                modifier = Modifier.fillMaxSize(), 
                                                mainActivity = mainActivityActivity,
                                                reloadTrigger = webViewReloadTrigger
                                            )
                                        }
                                    }

                                    // Terminal - only rendered when active
                                    if (!isWebViewSession) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { context ->
                                        TerminalView(context, null).apply {
                                            terminalView = WeakReference(this)
                                            
                                            // Intercept Android keyboard to apply virtual modifiers
                                            setOnKeyListener { _, keyCode, event ->
                                                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                                                    var metaState = event.metaState
                                                    var modified = false
                                                    
                                                    virtualKeysView.get()?.let { vkView ->
                                                        if (vkView.readSpecialButton(SpecialButton.CTRL, false) == true) {
                                                            metaState = metaState or android.view.KeyEvent.META_CTRL_ON or android.view.KeyEvent.META_CTRL_LEFT_ON
                                                            modified = true
                                                        }
                                                        if (vkView.readSpecialButton(SpecialButton.ALT, false) == true) {
                                                            metaState = metaState or android.view.KeyEvent.META_ALT_ON or android.view.KeyEvent.META_ALT_LEFT_ON
                                                            modified = true
                                                        }
                                                        if (vkView.readSpecialButton(SpecialButton.SHIFT, false) == true) {
                                                            metaState = metaState or android.view.KeyEvent.META_SHIFT_ON or android.view.KeyEvent.META_SHIFT_LEFT_ON
                                                            modified = true
                                                        }
                                                        if (vkView.readSpecialButton(SpecialButton.FN, false) == true) {
                                                            metaState = metaState or android.view.KeyEvent.META_META_ON or android.view.KeyEvent.META_META_LEFT_ON
                                                            modified = true
                                                        }
                                                    }
                                                    
                                                    if (modified) {
                                                        val newEvent = android.view.KeyEvent(
                                                            event.downTime,
                                                            event.eventTime,
                                                            event.action,
                                                            event.keyCode,
                                                            event.repeatCount,
                                                            metaState
                                                        )
                                                        this@apply.dispatchKeyEvent(newEvent)
                                                        return@setOnKeyListener true
                                                    }
                                                }
                                                false
                                            }
                                            
                                            setTextSize(
                                                dpToPx(
                                                    Settings.terminal_font_size.toFloat(),
                                                    context
                                                )
                                            )
                                            val client = TerminalBackEnd(this, mainActivityActivity)

                                            // Check if this is first-time setup
                                            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                            val openTerminalForSetup = prefs.getBoolean("open_terminal_for_setup", false)
                                            
                                            val session = if (openTerminalForSetup) {
                                                // Clear the flag
                                                prefs.edit().putBoolean("open_terminal_for_setup", false).apply()
                                                // Create Ubuntu terminal session for setup
                                                val sessionId = "setup-${System.currentTimeMillis()}"
                                                mainActivityActivity.sessionBinder!!.getService().currentSession.value = Pair(sessionId, WorkingMode.UBUNTU)
                                                mainActivityActivity.sessionBinder!!.createSession(
                                                    sessionId,
                                                    client,
                                                    mainActivityActivity,
                                                    WorkingMode.UBUNTU
                                                )
                                            } else if (pendingCommand != null) {
                                                mainActivityActivity.sessionBinder!!.getService().currentSession.value = Pair(
                                                    pendingCommand!!.id, pendingCommand!!.workingMode)
                                                mainActivityActivity.sessionBinder!!.getSession(
                                                    pendingCommand!!.id
                                                )
                                                    ?: mainActivityActivity.sessionBinder!!.createSession(
                                                        pendingCommand!!.id,
                                                        client,
                                                        mainActivityActivity, workingMode = Settings.working_Mode
                                                    )
                                            } else {
                                                mainActivityActivity.sessionBinder!!.getSession(
                                                    mainActivityActivity.sessionBinder!!.getService().currentSession.value.first
                                                )
                                                    ?: mainActivityActivity.sessionBinder!!.createSession(
                                                        mainActivityActivity.sessionBinder!!.getService().currentSession.value.first,
                                                        client,
                                                        mainActivityActivity,workingMode = Settings.working_Mode
                                                    )
                                            }

                                            session.updateTerminalSessionClient(client)
                                            attachSession(session)
                                            setTerminalViewClient(client)
                                            setTypeface(font)

                                            post {
                                                val color = getViewColor()

                                                keepScreenOn = true
                                                requestFocus()
                                                isFocusableInTouchMode = true

                                                mEmulator?.mColors?.mCurrentColors?.apply {
                                                    set(256, color)
                                                    set(258, color)
                                                }

                                                val colorsFile = localDir().child("colors.properties")
                                                if (colorsFile.exists() && colorsFile.isFile){
                                                    val props = Properties()
                                                    FileInputStream(colorsFile).use { input ->
                                                        props.load(input)
                                                    }
                                                    TerminalColors.COLOR_SCHEME.updateWith(props)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    update = { terminalView ->
                                        terminalView.onScreenUpdated()
                                       val color = getViewColor()

                                        terminalView.mEmulator?.mColors?.mCurrentColors?.apply {
                                            set(256, color)
                                            set(258, color)
                                        }
                                    },
                                )

                                } // end Column for terminal
                                } // end if (!isWebViewSession)
                                } // end Box
                                
                                if (showVirtualKeys.value && !isWebViewSession){
                                    val pagerState = rememberPagerState(pageCount = { 2 })
                                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(75.dp)
                                    ) { page ->
                                        when (page) {
                                            0 -> {
                                                terminalView.get()?.requestFocus()
                                                //terminalView.get()?.requestFocusFromTouch()
                                                AndroidView(
                                                    factory = { context ->
                                                        VirtualKeysView(context, null).apply {
                                                            virtualKeysView = WeakReference(this)
                                                            virtualKeysViewClient =
                                                                terminalView.get()?.mTermSession?.let {
                                                                    VirtualKeysListener(
                                                                        it
                                                                    )
                                                                }

                                                            buttonTextColor = onSurfaceColor!!

                                                            reload(
                                                                VirtualKeysInfo(
                                                                    VIRTUAL_KEYS,
                                                                    "",
                                                                    VirtualKeysConstants.CONTROL_CHARS_ALIASES
                                                                )
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(75.dp)
                                                )
                                            }

                                            1 -> {
                                                var text by rememberSaveable { mutableStateOf("") }

                                                AndroidView(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(75.dp),
                                                    factory = { ctx ->
                                                        EditText(ctx).apply {
                                                            maxLines = 1
                                                            isSingleLine = true
                                                            imeOptions = EditorInfo.IME_ACTION_DONE

                                                            // Listen for text changes to update Compose state
                                                            doOnTextChanged { textInput, _, _, _ ->
                                                                text = textInput.toString()
                                                            }

                                                            setOnEditorActionListener { v, actionId, event ->
                                                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                                                    if (text.isEmpty()) {
                                                                        // Dispatch enter key events if text is empty
                                                                        val eventDown = KeyEvent(
                                                                            KeyEvent.ACTION_DOWN,
                                                                            KeyEvent.KEYCODE_ENTER
                                                                        )
                                                                        val eventUp = KeyEvent(
                                                                            KeyEvent.ACTION_UP,
                                                                            KeyEvent.KEYCODE_ENTER
                                                                        )
                                                                        terminalView.get()
                                                                            ?.dispatchKeyEvent(eventDown)
                                                                        terminalView.get()
                                                                            ?.dispatchKeyEvent(eventUp)
                                                                    } else {
                                                                        terminalView.get()?.currentSession?.write(
                                                                            text
                                                                        )
                                                                        setText("")
                                                                    }
                                                                    true
                                                                } else {
                                                                    false
                                                                }
                                                            }
                                                        }
                                                    },
                                                    update = { editText ->
                                                        // Keep EditText's text in sync with Compose state, avoid infinite loop by only updating if different
                                                        if (editText.text.toString() != text) {
                                                            editText.setText(text)
                                                            editText.setSelection(text.length)
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                    }
                                }else{
                                    virtualKeysView = WeakReference(null)
                                }

                            } // end outer Column
                        }



                }

            })
    }
}

var wallAlpha by mutableFloatStateOf(Settings.wallTransparency)

@Composable
fun BackgroundImage() {
    bitmap.value?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(wallAlpha)
                .zIndex(-1f)
        )
    }
}

@Composable
fun SetStatusBarTextColor(isDarkIcons: Boolean) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return

    SideEffect {
        WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = isDarkIcons
    }
}



@Composable
fun SelectableCard(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "containerColor"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 8.dp else 2.dp
        ),
        enabled = enabled,
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}


fun changeSession(mainActivityActivity: MainActivity, session_id: String) {
    // If webview session, update current session but don't change terminal view
    if (session_id == "webview") {
        mainActivityActivity.sessionBinder?.getService()?.currentSession?.value = Pair(session_id, -1)
        return
    }
    
    terminalView.get()?.apply {
        val client = TerminalBackEnd(this, mainActivityActivity)
        val session =
            mainActivityActivity.sessionBinder!!.getSession(session_id)
                ?: mainActivityActivity.sessionBinder!!.createSession(
                    session_id,
                    client,
                    mainActivityActivity,workingMode = Settings.working_Mode
                )
        session.updateTerminalSessionClient(client)
        attachSession(session)
        setTerminalViewClient(client)
        post {
            val typedValue = TypedValue()

            context.theme.resolveAttribute(
                R.attr.colorOnSurface,
                typedValue,
                true
            )
            keepScreenOn = true
            requestFocus()
            isFocusableInTouchMode = true

            mEmulator?.mColors?.mCurrentColors?.apply {
                set(256, typedValue.data)
                set(258, typedValue.data)
            }
        }
        virtualKeysView.get()?.apply {
            virtualKeysViewClient =
                terminalView.get()?.mTermSession?.let { VirtualKeysListener(it) }
        }

    }
    val workingMode = mainActivityActivity.sessionBinder!!.getService().sessionList[session_id] ?: Settings.working_Mode
    mainActivityActivity.sessionBinder!!.getService().currentSession.value = Pair(session_id, workingMode)

}


const val VIRTUAL_KEYS =
    ("[" + "\n  [" + "\n    \"ESC\"," + "\n    {" + "\n      \"key\": \"/\"," + "\n      \"popup\": \"\\\\\"" + "\n    }," + "\n    {" + "\n      \"key\": \"-\"," + "\n      \"popup\": \"|\"" + "\n    }," + "\n    \"HOME\"," + "\n    \"END\"," + "\n    \"PGUP\"," + "\n    {" + "\n      \"key\": \"~\"," + "\n      \"popup\": \"`\"" + "\n    }," + "\n    \"UP\"," + "\n    {" + "\n      \"key\": \">\"," + "\n      \"popup\": \"<\"" + "\n    }" + "\n  ]," + "\n  [" + "\n    \"TAB\"," + "\n    \"CTRL\"," + "\n    \"ALT\"," + "\n    \"SHIFT\"," + "\n    \"META\"," + "\n    \"PGDN\"," + "\n    \"LEFT\"," + "\n    \"DOWN\"," + "\n    \"RIGHT\"" + "\n  ]" + "\n]")

const val WEBVIEW_VIRTUAL_KEYS =
    ("[" + "\n  [" + "\n    \"ESC\"," + "\n    {" + "\n      \"key\": \"/\"," + "\n      \"popup\": \"\\\\\"" + "\n    }," + "\n    {" + "\n      \"key\": \"-\"," + "\n      \"popup\": \"|\"" + "\n    }," + "\n    \"HOME\"," + "\n    \"END\"," + "\n    \"PGUP\"," + "\n    {" + "\n      \"key\": \"~\"," + "\n      \"popup\": \"`\"" + "\n    }," + "\n    \"UP\"," + "\n    \"BACK\"" + "\n  ]," + "\n  [" + "\n    \"TAB\"," + "\n    \"CTRL\"," + "\n    \"ALT\"," + "\n    \"SHIFT\"," + "\n    \"MOUSE\"," + "\n    \"PGDN\"," + "\n    \"LEFT\"," + "\n    \"DOWN\"," + "\n    \"RIGHT\"" + "\n  ]" + "\n]")