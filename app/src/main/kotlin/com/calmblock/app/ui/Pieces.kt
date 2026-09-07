package com.calmblock.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmblock.app.ui.theme.Type

object Ink {
    val bg = Color.White
    val card = Color(0xFFF4F4F4)
    val cardBorder = Color(0xFFC9C9C9)
    val line = Color(0xFFE4E4E4)
    val text = Color.Black
    val textSecondary = Color(0xFF555555)
    val textTertiary = Color(0xFF8A8A8A)
    val fill = Color.Black
    val onFill = Color.White
    val track = Color(0xFFDEDEDE)
}

object Dim {
    val screenPad = 16.dp
    val cardPad = 16.dp
    val cardRadius = 16.dp
    val cardBorderW = 1.5.dp
    val gap = 12.dp
    val tap = 52.dp
}

@Composable
fun Hairline(color: Color = Ink.line) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = Type.sectionHeader, modifier = Modifier.padding(start = Dim.screenPad, top = 18.dp, bottom = 8.dp))
}

@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Type.body.copy(color = Ink.textSecondary), modifier = modifier.padding(horizontal = Dim.screenPad, vertical = 4.dp))
}

@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .heightIn(min = Dim.tap)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Ink.fill else Ink.track)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Type.button.copy(color = if (enabled) Ink.onFill else Ink.textTertiary))
    }
}

@Composable
fun OutlineButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .heightIn(min = Dim.tap)
            .clip(RoundedCornerShape(14.dp))
            .border(Dim.cardBorderW, if (enabled) Ink.cardBorder else Ink.line, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Type.button.copy(color = if (enabled) Ink.text else Ink.textTertiary))
    }
}

@Composable
fun CalmCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    var m = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(Dim.cardRadius))
        .border(Dim.cardBorderW, Ink.cardBorder, RoundedCornerShape(Dim.cardRadius))
        .background(Ink.card)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m.padding(Dim.cardPad)) { content() }
}

@Composable
fun Chip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(if (selected) Modifier.background(Ink.fill) else Modifier.border(1.5.dp, Ink.cardBorder, RoundedCornerShape(50)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = Type.chip.copy(color = if (selected) Ink.onFill else if (enabled) Ink.text else Ink.textTertiary),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun FieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    Column(modifier) {
        Text(label, style = Type.meta)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(Dim.cardBorderW, if (enabled) Ink.cardBorder else Ink.line, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                textStyle = Type.fieldInput.copy(color = if (enabled) Ink.text else Ink.textSecondary),
                cursorBrush = SolidColor(Ink.text),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = Type.fieldInput.copy(color = Ink.textTertiary))
                    inner()
                },
            )
        }
    }
}

/** Screen header. The back control is a word, not an arrow. */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dim.screenPad, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Text(
                    "Back",
                    style = Type.button,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onBack).padding(vertical = 8.dp, horizontal = 4.dp),
                )
                Spacer(Modifier.width(16.dp))
            }
            Text(title, style = Type.screenTitle, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Hairline()
    }
}

data class TabItem(val label: String, val selected: Boolean, val onClick: () -> Unit)

@Composable
fun BottomTabBar(items: List<TabItem>) {
    Column {
        Hairline(Ink.cardBorder)
        Row(
            modifier = Modifier.fillMaxWidth().background(Ink.bg).navigationBarsPadding().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = item.onClick).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.label,
                        style = Type.tab.copy(
                            color = if (item.selected) Ink.text else Ink.textTertiary,
                            fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}

/** A list row with a label, a meta line, and an optional trailing word. */
@Composable
fun ListRow(title: String, meta: String? = null, trailing: String? = null, onTrailing: (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    var m = Modifier.fillMaxWidth()
    if (onClick != null) m = m.clickable(onClick = onClick)
    Row(m.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.row)
            if (meta != null) {
                Spacer(Modifier.height(2.dp))
                Text(meta, style = Type.meta)
            }
        }
        if (trailing != null) {
            var t = Modifier.padding(start = 12.dp)
            if (onTrailing != null) t = t.clip(RoundedCornerShape(8.dp)).clickable(onClick = onTrailing).padding(6.dp)
            Text(trailing, style = if (onTrailing != null) Type.button else Type.figure, modifier = t)
        }
    }
}
