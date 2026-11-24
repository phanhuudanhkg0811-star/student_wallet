package com.example.student_wallet.feature.security.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.student_wallet.core.domain.budget.BudgetLevel
import com.example.student_wallet.core.domain.format.MoneyFormatter
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

class WalletGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SIZE_2X2, SIZE_4X1, SIZE_4X2),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.getOrNull()
        val transactions = koin?.let { runCatching { it.get<TransactionRepository>().observeTransactions().first() }.getOrNull() }
            .orEmpty()
        val settings = koin?.let { runCatching { it.get<SettingsRepository>().observeSettings().first() }.getOrNull() }
            ?: UserSettings()

        val summary = WidgetSummaryCalculator.compute(
            transactions = transactions,
            settings = settings,
        )

        val quickAddIntent = Intent(context, QuickAddWidgetActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val quickAddAction = actionStartActivity(quickAddIntent)
        val openAppAction = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }
            ?.let(::actionStartActivity)

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                when {
                    size.height < 90.dp -> WidgetLayout4x1(
                        summary = summary,
                        onOpenApp = openAppAction,
                        onQuickAdd = quickAddAction,
                    )
                    size.width < 220.dp -> WidgetLayout2x2(
                        summary = summary,
                        onOpenApp = openAppAction,
                        onQuickAdd = quickAddAction,
                    )
                    else -> WidgetLayout4x2(
                        summary = summary,
                        onOpenApp = openAppAction,
                        onQuickAdd = quickAddAction,
                    )
                }
            }
        }
    }

    companion object {
        private val SIZE_2X2 = DpSize(110.dp, 110.dp)
        private val SIZE_4X1 = DpSize(250.dp, 56.dp)
        private val SIZE_4X2 = DpSize(250.dp, 110.dp)
    }
}

private val WidgetSurfaceColor = ColorProvider(Color(0xFFF6FAF8))
private val WidgetCardBorderTint = ColorProvider(Color(0xFFE3ECE7))
private val WidgetPrimaryColor = ColorProvider(Color(0xFF006C51))
private val WidgetPrimaryTextColor = ColorProvider(Color(0xFFFFFFFF))
private val WidgetTitleTextColor = ColorProvider(Color(0xFF15201B))
private val WidgetMutedTextColor = ColorProvider(Color(0xFF53635B))
private val WidgetOkColor = ColorProvider(Color(0xFF00855F))
private val WidgetWarnColor = ColorProvider(Color(0xFFE08A00))
private val WidgetBadColor = ColorProvider(Color(0xFFBA1A1A))

private fun budgetLevelColor(level: BudgetLevel): ColorProvider = when (level) {
    BudgetLevel.OK -> WidgetOkColor
    BudgetLevel.WARN -> WidgetWarnColor
    BudgetLevel.BAD -> WidgetBadColor
}

@Composable
private fun WidgetLayout2x2(
    summary: WidgetSummary,
    onOpenApp: Action?,
    onQuickAdd: Action,
) {
    val rootModifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(20.dp)
        .background(WidgetSurfaceColor)
        .padding(12.dp)
        .let { if (onOpenApp != null) it.clickable(onOpenApp) else it }

    val statusColor = budgetLevelColor(summary.budgetLevel)

    Column(
        modifier = rootModifier,
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "StudentWallet",
                style = TextStyle(
                    color = WidgetPrimaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            QuickAddPillButton(label = "+", action = onQuickAdd)
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        Text(
            text = "Số dư ví",
            style = TextStyle(color = WidgetMutedTextColor, fontSize = 11.sp),
        )
        Text(
            text = MoneyFormatter.format(summary.currentBalance, summary.currency),
            style = TextStyle(
                color = WidgetTitleTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        Text(
            text = "Hôm nay: ${MoneyFormatter.format(summary.todayExpense, summary.currency)}",
            style = TextStyle(color = WidgetMutedTextColor, fontSize = 11.sp),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "${WidgetSummaryCalculator.budgetStatusLabel(summary.budgetLevel)} (${summary.budgetPercent}%)",
            style = TextStyle(
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun WidgetLayout4x1(
    summary: WidgetSummary,
    onOpenApp: Action?,
    onQuickAdd: Action,
) {
    val rootModifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(18.dp)
        .background(WidgetSurfaceColor)
        .padding(horizontal = 14.dp, vertical = 10.dp)
        .let { if (onOpenApp != null) it.clickable(onOpenApp) else it }

    val statusColor = budgetLevelColor(summary.budgetLevel)

    Row(
        modifier = rootModifier,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = MoneyFormatter.format(summary.currentBalance, summary.currency),
                style = TextStyle(
                    color = WidgetTitleTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = "Hôm nay: ${MoneyFormatter.format(summary.todayExpense, summary.currency)}",
                    style = TextStyle(color = WidgetMutedTextColor, fontSize = 11.sp),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "• ${summary.budgetPercent}%",
                    style = TextStyle(
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(8.dp))
        QuickAddPillButton(label = "+ Thêm", action = onQuickAdd)
    }
}

@Composable
private fun WidgetLayout4x2(
    summary: WidgetSummary,
    onOpenApp: Action?,
    onQuickAdd: Action,
) {
    val rootModifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(22.dp)
        .background(WidgetSurfaceColor)
        .padding(14.dp)
        .let { if (onOpenApp != null) it.clickable(onOpenApp) else it }

    val statusColor = budgetLevelColor(summary.budgetLevel)

    Column(modifier = rootModifier) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "StudentWallet · Số dư hiện tại",
                    style = TextStyle(
                        color = WidgetPrimaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = MoneyFormatter.format(summary.currentBalance, summary.currency),
                    style = TextStyle(
                        color = WidgetTitleTextColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            QuickAddPillButton(label = "+ Chi nhanh", action = onQuickAdd)
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .background(WidgetCardBorderTint)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Chi hôm nay",
                    style = TextStyle(color = WidgetMutedTextColor, fontSize = 10.sp),
                )
                Text(
                    text = MoneyFormatter.format(summary.todayExpense, summary.currency),
                    style = TextStyle(
                        color = WidgetTitleTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.Horizontal.End) {
                Text(
                    text = if (summary.isReferenceBudget) "Chi tháng / Tham chiếu" else "Chi tháng / Hạn mức",
                    style = TextStyle(color = WidgetMutedTextColor, fontSize = 10.sp),
                )
                Text(
                    text = "${MoneyFormatter.format(summary.monthExpense, summary.currency)} / ${MoneyFormatter.format(summary.budgetLimit, summary.currency)}",
                    style = TextStyle(
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = WidgetSummaryCalculator.budgetStatusLabel(summary.budgetLevel),
                style = TextStyle(
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "${summary.budgetPercent}%",
                style = TextStyle(
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        LinearProgressIndicator(
            progress = summary.budgetProgressFraction,
            modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(99.dp),
            color = statusColor,
            backgroundColor = WidgetCardBorderTint,
        )
    }
}

@Composable
private fun QuickAddPillButton(
    label: String,
    action: Action,
) {
    Box(
        modifier = GlanceModifier
            .cornerRadius(12.dp)
            .background(WidgetPrimaryColor)
            .clickable(action)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = WidgetPrimaryTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}
