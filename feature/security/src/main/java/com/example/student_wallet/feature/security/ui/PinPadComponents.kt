package com.example.student_wallet.feature.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PinDotsIndicator(
    pinLength: Int,
    maxLength: Int = 6,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxLength) { index ->
            val filled = index < pinLength
            val fillColor = when {
                isError -> MaterialTheme.colorScheme.error
                filled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
            val borderColor = when {
                isError -> MaterialTheme.colorScheme.error
                filled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(fillColor)
                    .border(width = 1.5.dp, color = borderColor, shape = CircleShape),
            )
        }
    }
}

@Composable
internal fun NumericPinKeypad(
    onDigitClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showBiometricKey: Boolean = false,
    onBiometricClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )

    Column(
        modifier = modifier.widthIn(max = 312.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { rowDigits ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowDigits.forEach { digit ->
                    PinKeyButton(
                        label = digit,
                        enabled = enabled,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDigitClick(digit.first())
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBiometricKey && onBiometricClick != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(enabled = enabled, onClick = onBiometricClick),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = "Mở khóa vân tay",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).height(60.dp))
            }

            PinKeyButton(
                label = "0",
                enabled = enabled,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDigitClick('0')
                },
                modifier = Modifier.weight(1f),
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = enabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackspaceClick()
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Xóa số",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(4.dp),
        ) {
            Text(
                text = label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
            )
        }
    }
}
