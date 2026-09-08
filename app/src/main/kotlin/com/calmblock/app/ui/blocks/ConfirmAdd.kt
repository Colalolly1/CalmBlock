package com.calmblock.app.ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmblock.app.ui.Dim
import com.calmblock.app.ui.FieldRow
import com.calmblock.app.ui.Ink
import com.calmblock.app.ui.OutlineButton
import com.calmblock.app.ui.PrimaryButton
import com.calmblock.app.ui.theme.Type

private const val PHRASE = "BLOCK"

/**
 * Stands between the user and an addition they cannot take back. Cancel is the
 * prominent button, and a forever block also has to be typed out, so no
 * mis-tap can reach the confirm.
 */
@Composable
fun ConfirmAdd(
    what: String,
    forever: Boolean,
    untilText: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var typed by remember(what) { mutableStateOf("") }
    val ready = !forever || typed.trim().equals(PHRASE, ignoreCase = true)

    Column(
        Modifier.fillMaxSize().background(Ink.bg).systemBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(40.dp))
        Text(if (forever) "Block forever?" else "Block until it ends?", style = Type.screenTitle)
        Spacer(Modifier.height(20.dp))
        Text(what, style = Type.hero)
        Spacer(Modifier.height(24.dp))
        Text(
            if (forever) {
                "This cannot be undone. Not tomorrow, not in a year, not by " +
                    "uninstalling this app, not by clearing its data. There is no " +
                    "code in it that can take this back out."
            } else {
                "It cannot be taken back out until the block ends" + (untilText?.let { ", $it" } ?: "") + "."
            },
            style = Type.body,
        )
        if (forever) {
            Spacer(Modifier.height(28.dp))
            FieldRow(
                label = "Type $PHRASE to confirm",
                value = typed,
                onValueChange = { typed = it },
                placeholder = PHRASE,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Cancel", Modifier.fillMaxWidth(), onClick = onCancel)
        Spacer(Modifier.height(12.dp))
        OutlineButton(
            if (forever) "Block $what forever" else "Block $what",
            Modifier.fillMaxWidth(),
            enabled = ready,
            onClick = onConfirm,
        )
        Spacer(Modifier.height(40.dp))
    }
}
