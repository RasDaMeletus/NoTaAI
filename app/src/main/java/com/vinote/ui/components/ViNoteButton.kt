package com.vinote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.ui.theme.ViNoteOnPrimary
import com.vinote.ui.theme.ViNotePrimary

enum class ViNoteButtonType {
    PRIMARY,
    GHOST,
    SECONDARY
}

@Composable
fun ViNoteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ViNoteButtonType = ViNoteButtonType.PRIMARY,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "vinote_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(150),
        label = "btn_scale"
    )

    when (type) {
        ViNoteButtonType.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ViNotePrimary,
                    contentColor = ViNoteOnPrimary,
                    disabledContainerColor = ViNotePrimary.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                interactionSource = interactionSource,
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .scale(scale)
                    .testTag(testTag)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.invoke()
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    trailingIcon?.invoke()
                }
            }
        }
        ViNoteButtonType.GHOST -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.5.dp, ViNotePrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ViNotePrimary
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                interactionSource = interactionSource,
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .scale(scale)
                    .testTag(testTag)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.invoke()
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    trailingIcon?.invoke()
                }
            }
        }
        ViNoteButtonType.SECONDARY -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF3F3F5),
                    contentColor = Color(0xFF171827)
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                interactionSource = interactionSource,
                modifier = modifier
                    .heightIn(min = 48.dp)
                    .scale(scale)
                    .testTag(testTag)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.invoke()
                    Text(
                        text = text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    trailingIcon?.invoke()
                }
            }
        }
    }
}
