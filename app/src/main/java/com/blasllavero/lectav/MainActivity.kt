/*
 * LECTAV — Open Learning Avatar
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
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

private fun sampleSubjectsFor(course: String): List<Choice> = when {
    course.startsWith("eso") || course.startsWith("batx") ->
        listOf(Choice("math", R.string.sample_mathematics))
    else -> emptyList()
}

class MainActivity : ComponentActivity() {
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
    val preferences = remember(context) {
        context.getSharedPreferences("lectav_preferences", Context.MODE_PRIVATE)
    }
    var screen by rememberSaveable { mutableStateOf("home") }
    var pathway by rememberSaveable { mutableStateOf("") }
    var course by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var subtitleHeight by remember { mutableFloatStateOf(preferences.getFloat("subtitle_height", 0.25f)) }
    val courses = coursesFor(pathway)
    val subjects = sampleSubjectsFor(course)

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
                    "calibration" -> CalibrationScreen(
                        subtitleHeight = subtitleHeight,
                        onSubtitleHeight = { height ->
                            subtitleHeight = height
                            preferences.edit().putFloat("subtitle_height", height).apply()
                        }
                    )
                    else -> {
                        Text(
                            stringResource(R.string.tagline),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.introduction))
                        ChoiceDropdown(
                            label = stringResource(R.string.select_pathway),
                            choices = pathways,
                            selectedId = pathway,
                            onChoose = { pathway = it; course = ""; subject = "" }
                        )
                        ChoiceDropdown(
                            label = stringResource(R.string.select_course),
                            choices = courses,
                            selectedId = course,
                            enabled = courses.isNotEmpty(),
                            onChoose = { course = it; subject = "" }
                        )
                        ChoiceDropdown(
                            label = stringResource(R.string.select_subject),
                            choices = subjects,
                            selectedId = subject,
                            enabled = subjects.isNotEmpty(),
                            onChoose = { subject = it }
                        )
                        if (pathway in listOf("cfgm", "cfgs", "cert")) {
                            Text(
                                stringResource(R.string.catalogue_pending),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (subject.isNotEmpty()) {
                            Text(stringResource(R.string.demo_notice))
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
private fun CalibrationScreen(subtitleHeight: Float, onSubtitleHeight: (Float) -> Unit) {
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
    Slider(value = subtitleHeight, onValueChange = onSubtitleHeight, valueRange = 0f..1f)
    Text(stringResource(R.string.calibration_saved), style = MaterialTheme.typography.bodySmall)
}
