package com.salahtimesonly.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahtimesonly.R

@Composable
fun SubHeader(title: String?, onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onBack)
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_back), null, tint = p.accent, modifier = Modifier.size(22.dp))
        Text(stringResource(R.string.back), color = p.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
    if (title != null) {
        Spacer(Modifier.height(6.dp))
        ScreenTitle(title)
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, color = LocalPalette.current.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
}

@Composable
fun StepLabel(n: Int) {
    Text(stringResource(R.string.step_of, n), color = LocalPalette.current.muted, fontSize = 13.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = LocalPalette.current.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp))
}

@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Text(text, color = LocalPalette.current.muted, fontSize = 13.sp, lineHeight = 18.sp,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp))
}

@Composable
fun Group(content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(14.dp)),
        content = content,
    )
}

@Composable
fun Divider() = HorizontalDivider(color = LocalPalette.current.line, thickness = 1.dp)

@Composable
fun NavRow(label: String, value: String? = null, sub: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = p.text, fontSize = 16.sp)
            if (sub != null) Text(sub, color = p.muted, fontSize = 13.sp)
        }
        if (value != null) Text(value, color = p.muted, fontSize = 15.sp, textAlign = TextAlign.End)
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = p.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = p.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.onAccent, checkedTrackColor = p.accent, checkedBorderColor = p.accent,
                uncheckedThumbColor = p.muted, uncheckedTrackColor = p.bg, uncheckedBorderColor = p.line,
                disabledCheckedTrackColor = p.accent, disabledCheckedThumbColor = p.onAccent,
                disabledUncheckedTrackColor = p.bg, disabledUncheckedThumbColor = p.muted,
            ),
        )
    }
}

@Composable
fun RadioRow(
    label: String,
    sub: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = p.text, fontSize = 16.sp)
            if (sub != null) Text(sub, color = p.muted, fontSize = 13.sp)
        }
        if (trailing != null) trailing()
        RadioButton(selected = selected, onClick = null, enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = p.accent, unselectedColor = p.faint,
                disabledSelectedColor = p.accent, disabledUnselectedColor = p.faint))
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(p.bg)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for ((value, label) in options) {
            val on = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) p.surface else p.bg)
                    .then(if (on) Modifier.border(1.dp, p.line, RoundedCornerShape(8.dp)) else Modifier)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) p.text else p.muted, fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(p.accent)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = p.onAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun QuietButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = p.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun IconLabelButton(@DrawableRes icon: Int, text: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), null, tint = p.accent, modifier = Modifier.size(20.dp))
        Text(text, color = p.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
