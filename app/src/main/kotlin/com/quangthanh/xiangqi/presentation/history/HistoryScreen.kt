package com.quangthanh.xiangqi.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quangthanh.xiangqi.R
import com.quangthanh.xiangqi.application.dto.GameSummary
import com.quangthanh.xiangqi.application.dto.Outcome
import com.quangthanh.xiangqi.application.dto.Record
import com.quangthanh.xiangqi.domain.model.Difficulty
import com.quangthanh.xiangqi.domain.model.GameId
import com.quangthanh.xiangqi.presentation.common.difficultyText
import com.quangthanh.xiangqi.presentation.common.modeText
import com.quangthanh.xiangqi.presentation.common.resultReason
import com.quangthanh.xiangqi.presentation.common.resultTitle
import com.quangthanh.xiangqi.presentation.common.woodBackground
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Parchment = Color(0xFFF3DDB3)
private val Ink = Color(0xFF2B1A0E)
private val LightText = Color(0xFFFFE9C4)

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onOpenGame: (GameId) -> Unit, onBack: () -> Unit) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize().woodBackground().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back), tint = LightText)
            }
            Text(stringResource(R.string.history_title), color = LightText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        val current = history
        when {
            current == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = LightText) }
            current.games.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.history_empty), color = LightText, fontSize = 18.sp)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (current.recordByDifficulty.values.any { it.played > 0 }) {
                    item { RecordCard(current.recordByDifficulty) }
                }
                items(current.games, key = { it.gameId.value }) { game ->
                    GameRow(game, onClick = { onOpenGame(game.gameId) })
                }
            }
        }
    }
}

@Composable
private fun RecordCard(records: Map<Difficulty, Record>) {
    Column(
        Modifier.fillMaxWidth().background(Parchment, RoundedCornerShape(8.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.home_vs_computer), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        records.filterValues { it.played > 0 }.forEach { (difficulty, record) ->
            Text(
                "${difficultyText(difficulty)}: " + stringResource(R.string.history_record, record.wins, record.losses, record.draws),
                color = Ink,
            )
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
private fun GameRow(game: GameSummary, onClick: () -> Unit) {
    val result = game.result
    val titleColor = when (game.outcomeForHuman) {
        Outcome.WIN -> Color(0xFF2E7D32)
        Outcome.LOSS -> Color(0xFFC62828)
        else -> Ink
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Parchment, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (result == null) stringResource(R.string.history_in_progress) else resultTitle(result, game.participants),
                color = titleColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            val details = listOfNotNull(
                modeText(game.participants),
                result?.let { resultReason(it) },
                stringResource(R.string.move_count, game.moveCount),
            )
            Text(details.joinToString(" · "), color = Ink.copy(alpha = 0.75f), fontSize = 14.sp)
        }
        Text(dateFormat.format(game.startedAt.atZone(ZoneId.systemDefault())), color = Ink.copy(alpha = 0.75f), fontSize = 13.sp)
    }
}
