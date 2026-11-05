/*
 * LECTAV — Open Learning Avatar
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class Choice(val id: String, @StringRes val label: Int)

private val pathways = listOf(
    Choice("eso", R.string.level_eso),
    Choice("batx", R.string.level_batx),
    Choice("cfgm", R.string.level_cfgm),
    Choice("cfgs", R.string.level_cfgs),
    Choice("cert", R.string.level_cert)
)

private fun coursesFor(pathway: String): List<Choice> = when (pathway) {
    "eso" -> listOf(
        Choice("eso1", R.string.eso_1), Choice("eso2", R.string.eso_2),
        Choice("eso3", R.string.eso_3), Choice("eso4", R.string.eso_4)
    )
    "batx" -> listOf(Choice("batx1", R.string.batx_1), Choice("batx2", R.string.batx_2))
    else -> emptyList() // Will be loaded from the verified official catalogue.
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LectavApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LectavApp() {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) { SettingsRepository(context.applicationContext) }
    val saved by repository.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf("home") }
    var subtitleHeight by remember { mutableFloatStateOf(0.25f) }
    LaunchedEffect(saved?.subtitleHeight) {
        saved?.let { subtitleHeight = it.subtitleHeight }
    }
    val pathway = saved?.pathway ?: ""
    val course = saved?.course ?: ""
    val subject = saved?.subject ?: ""
    val courses = coursesFor(pathway)
    val subjects = catalogueSubjectsFor(course)

    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        if (screen == "home") {
                            TextButton(onClick = { screen = "about" }) {
                                Text(stringResource(R.string.about))
                            }
                            TextButton(onClick = { screen = "help" }) {
                                Text(stringResource(R.string.help))
                            }
                            TextButton(onClick = { screen = "settings" }) {
                                Text(stringResource(R.string.settings))
                            }
                        } else {
                            TextButton(onClick = { screen = "home" }) {
                                Text(stringResource(R.string.back))
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (screen) {
                    "about" -> AboutScreen()
                    "help" -> HelpScreen(onCalibration = { screen = "calibration" })
                    "library" -> saved?.let {
                        SavedCoursesScreen(
                            settings = it,
                            onRoleChange = { role ->
                                scope.launch { repository.setActiveRole(role) }
                            },
                            onOpen = { id ->
                                scope.launch {
                                    repository.openSavedCourse(id)
                                    screen = "home"
                                }
                            },
                            onRemove = { id ->
                                scope.launch { repository.removeSavedCourse(id) }
                            },
                            onPickCourse = { screen = "home" }
                        )
                    }
                    "settings" -> saved?.let {
                        SettingsScreen(
                            settings = it,
                            onClear = { scope.launch { repository.clearSelection() } },
                            onCalibration = { screen = "calibration" }
                        )
                    }
                    "calibration" -> CalibrationScreen(
                        subtitleHeight = subtitleHeight,
                        onSubtitleHeight = { subtitleHeight = it },
                        onFinish = { scope.launch { repository.saveSubtitleHeight(subtitleHeight) } }
                    )
                    else -> {
                        if (saved == null) {
                            CircularProgressIndicator()
                        } else {
                        Text(
                            stringResource(R.string.tagline),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.introduction))
                        Text(stringResource(R.string.catalogue_region), style = MaterialTheme.typography.labelMedium)
                        Button(
                            onClick = { screen = "library" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.library_title)) }

                        ChoiceDropdown(
                            label = stringResource(R.string.select_pathway),
                            choices = pathways,
                            selectedId = pathway,
                            onChoose = { value -> scope.launch { repository.choosePathway(value) } }
                        )
                        ChoiceDropdown(
                            label = stringResource(R.string.select_course),
                            choices = courses,
                            selectedId = course,
                            enabled = courses.isNotEmpty(),
                            onChoose = { value -> scope.launch { repository.chooseCourse(value) } }
                        )
                        SubjectDropdown(
                            label = stringResource(R.string.select_subject),
                            choices = subjects,
                            selectedId = subject,
                            enabled = subjects.isNotEmpty(),
                            onChoose = { value -> scope.launch { repository.chooseSubject(value) } }
                        )
                        if (pathway in listOf("cfgm", "cfgs", "cert")) {
                            Text(
                                stringResource(R.string.catalogue_pending),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                stringResource(R.string.catalogue_scope_note),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (course == "batx2") {
                                Text(stringResource(R.string.catalogue_batx2_note), style = MaterialTheme.typography.bodySmall)
                            }
                            val chosen = subjects.find { it.id == subject }
                            if (chosen != null) {
                                Text(
                                    "${chosen.officialNameCa} · ${stringResource(chosen.kind.labelRes)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(chosen.sourceUrl)))
                                    }
                                }) {
                                    Text(stringResource(R.string.catalogue_source_button))
                                }
                                val alreadySaved = saved?.savedCourses?.any { entry ->
                                    entry.role == saved?.activeRole &&
                                    entry.pathway == pathway &&
                                    entry.course == course &&
                                    entry.subject == subject
                                } == true
                                OutlinedButton(
                                    enabled = !alreadySaved,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { scope.launch { repository.saveCurrentCourse() } }
                                ) {
                                    Text(
                                        stringResource(
                                            if (alreadySaved) R.string.library_saved
                                            else R.string.library_save
                                        )
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        Text(stringResource(R.string.future_features), fontWeight = FontWeight.SemiBold)
                        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.generate_lesson_pdf))
                        }
                        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.generate_exercise_pdf))
                        }
                        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.start_avatar))
                        }
                        Text(stringResource(R.string.not_yet_available), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            onClick = { screen = "calibration" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.calibrate_subtitles)) }
                        Text(stringResource(R.string.saved_selection_message), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceDropdown(
    label: String,
    choices: List<Choice>,
    selectedId: String,
    enabled: Boolean = true,
    onChoose: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = choices.find { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                onClick = { expanded = true }
            ) {
                Text(if (selected != null) stringResource(selected.label) else stringResource(R.string.please_select))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                choices.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(stringResource(item.label)) },
                        onClick = { onChoose(item.id); expanded = false }
                    )
                }
            }
        }
    }
}


@Composable
private fun SubjectDropdown(
    label: String,
    choices: List<SubjectOption>,
    selectedId: String,
    enabled: Boolean = true,
    onChoose: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = choices.find { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                onClick = { expanded = true }
            ) {
                Text(selected?.officialNameCa ?: stringResource(R.string.please_select))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(item.officialNameCa)
                                Text(
                                    stringResource(item.kind.labelRes),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        onClick = { onChoose(item.id); expanded = false }
                    )
                }
            }
        }
    }
}



@Composable
private fun SavedCoursesScreen(
    settings: SavedSettings,
    onRoleChange: (String) -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onPickCourse: () -> Unit
) {
    var pendingRemoval by remember { mutableStateOf<SavedCourse?>(null) }
    // No materials can be generated yet, so every material count is zero.
    // Real generated-file counts will replace the fallback ordering later.
    val entries = settings.savedCourses.filter { it.role == settings.activeRole }
        .sortedWith(
            compareByDescending<SavedCourse> { it.lastOpenedAt }
                .thenBy { it.course }
                .thenBy { it.subject }
        )

    Text(stringResource(R.string.library_title), style = MaterialTheme.typography.titleLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = settings.activeRole == "teacher",
            onClick = { onRoleChange("teacher") },
            label = { Text(stringResource(R.string.library_teacher)) }
        )
        FilterChip(
            selected = settings.activeRole == "student",
            onClick = { onRoleChange("student") },
            label = { Text(stringResource(R.string.library_student)) }
        )
    }
    Text(
        stringResource(R.string.library_intro),
        style = MaterialTheme.typography.bodySmall
    )
    Button(onClick = onPickCourse, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.library_choose_course))
    }
    if (entries.isEmpty()) {
        Text(stringResource(R.string.library_empty))
    } else {
        Text(
            stringResource(R.string.library_sort_note),
            style = MaterialTheme.typography.bodySmall
        )
        entries.forEach { entry ->
            val title = catalogueSubjectsFor(entry.course)
                .firstOrNull { it.id == entry.subject }?.officialNameCa
                ?: entry.subject
            val level = coursesFor(entry.pathway)
                .firstOrNull { it.id == entry.course }?.let { stringResource(it.label) }
                ?: entry.course
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(level, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (settings.activeRole == "teacher")
                            stringResource(R.string.library_no_generated)
                        else stringResource(R.string.library_no_learning),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onOpen(entry.id) }) {
                            Text(stringResource(R.string.library_open))
                        }
                        OutlinedButton(onClick = { pendingRemoval = entry }) {
                            Text(stringResource(R.string.library_remove))
                        }
                    }
                }
            }
        }
    }
    Text(stringResource(R.string.library_offline_note), style = MaterialTheme.typography.bodySmall)
    pendingRemoval?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.library_remove_title)) },
            text = { Text(stringResource(R.string.library_remove_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(entry.id)
                    pendingRemoval = null
                }) { Text(stringResource(R.string.library_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(stringResource(R.string.library_cancel))
                }
            }
        )
    }
}

private data class AppLanguage(val code: String, val name: String)
private val appLanguages = listOf(
    AppLanguage("en", "English"),
    AppLanguage("ca", "Català"),
    AppLanguage("es", "Español"),
    AppLanguage("fr", "Français"),
    AppLanguage("pt", "Português"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("ar", "العربية"),
    AppLanguage("hi", "हिन्दी"),
    AppLanguage("zh-CN", "简体中文"),
    AppLanguage("ja", "日本語")
)

@Composable
private fun SettingsScreen(
    settings: SavedSettings,
    onClear: () -> Unit,
    onCalibration: () -> Unit
) {
    // Subscribe to configuration changes so the picker refreshes after locale changes.
    val config = LocalConfiguration.current
    val currentTag = AppCompatDelegate.getApplicationLocales()
        .toLanguageTags().substringBefore(",")
    val activeLanguage = if (currentTag.startsWith("zh", ignoreCase = true)) "zh-CN"
        else appLanguages.firstOrNull { it.code.equals(currentTag, ignoreCase = true) }?.code
            ?: "system"
    var expanded by remember { mutableStateOf(false) }

    Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.settings_saved_note))
    Text(stringResource(R.string.interface_language), style = MaterialTheme.typography.labelLarge)
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { expanded = true }) {
            Text(appLanguages.firstOrNull { it.code == activeLanguage }?.name
                ?: stringResource(R.string.language_system))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_system)) },
                onClick = {
                    expanded = false
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
            )
            appLanguages.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        expanded = false
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(item.code)
                        )
                    }
                )
            }
        }
    }
    Text(stringResource(R.string.language_help), style = MaterialTheme.typography.bodySmall)
    HorizontalDivider()
    Text(stringResource(R.string.saved_education), style = MaterialTheme.typography.titleMedium)
    Text("${stringResource(R.string.settings_country)}: ${stringResource(R.string.settings_spain)}")
    Text("${stringResource(R.string.settings_territory)}: ${stringResource(R.string.settings_catalonia)}")
    Text(stringResource(R.string.settings_jurisdiction_note), style = MaterialTheme.typography.bodySmall)
    val p = pathways.firstOrNull { it.id == settings.pathway }?.let { stringResource(it.label) }
        ?: stringResource(R.string.please_select)
    val c = coursesFor(settings.pathway).firstOrNull { it.id == settings.course }
        ?.let { stringResource(it.label) } ?: stringResource(R.string.please_select)
    val s = catalogueSubjectsFor(settings.course).firstOrNull { it.id == settings.subject }
        ?.officialNameCa ?: stringResource(R.string.please_select)
    Text("${stringResource(R.string.select_pathway)}: $p")
    Text("${stringResource(R.string.select_course)}: $c")
    Text("${stringResource(R.string.select_subject)}: $s")
    OutlinedButton(
        onClick = onClear, enabled = settings.pathway.isNotEmpty(),
        modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.clear_selection)) }
    OutlinedButton(onClick = onCalibration, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.calibrate_subtitles))
    }
    Text(stringResource(R.string.settings_local_only), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AboutScreen() {
    Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.about_body))
    Text(stringResource(R.string.author))
    Text("https://blasllavero.com")
    Text(stringResource(R.string.open_source_statement))
}

@Composable
private fun HelpScreen(onCalibration: () -> Unit) {
    Text(stringResource(R.string.help_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.help_body))
    Text(stringResource(R.string.help_demo))
    OutlinedButton(onClick = onCalibration, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.calibrate_subtitles))
    }
}

@Composable
private fun CalibrationScreen(
    subtitleHeight: Float,
    onSubtitleHeight: (Float) -> Unit,
    onFinish: () -> Unit
) {
    Text(stringResource(R.string.calibration_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.calibration_instructions))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
    ) {
        Text(
            stringResource(R.string.avatar_preview),
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
                .padding(bottom = (12f + subtitleHeight * 140f).dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.inverseSurface
        ) {
            Text(
                stringResource(R.string.subtitle_preview),
                modifier = Modifier.padding(10.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                textAlign = TextAlign.Center
            )
        }
    }
    Slider(
        value = subtitleHeight,
        onValueChange = onSubtitleHeight,
        onValueChangeFinished = onFinish,
        valueRange = 0f..1f
    )
    Text(stringResource(R.string.calibration_saved), style = MaterialTheme.typography.bodySmall)
}
