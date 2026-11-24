package com.example.student_wallet.feature.security.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.student_wallet.core.domain.model.Category
import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.TransactionType
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickAddUiState(
    val amountDigits: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val availableCategories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val note: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val currency: CurrencyOption = CurrencyOption.VND,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class QuickAddWidgetViewModel(
    private val transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalQuickAddInput())

    val uiState: StateFlow<QuickAddUiState> = combine(
        transactionRepository.observeCategories(),
        settingsRepository.observeSettings(),
        localState,
    ) { allCategories, settings, local ->
        val filtered = allCategories.filter { it.type == local.type }
        val resolvedSelectedId = local.selectedCategoryId
            ?.takeIf { id -> filtered.any { it.id == id } }
            ?: filtered.firstOrNull()?.id

        QuickAddUiState(
            amountDigits = local.amountDigits,
            type = local.type,
            availableCategories = filtered,
            selectedCategoryId = resolvedSelectedId,
            note = local.note,
            paymentMethod = local.paymentMethod,
            currency = settings.currency,
            isSaving = local.isSaving,
            errorMessage = local.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = QuickAddUiState(),
    )

    fun onAmountInputChange(raw: String) {
        localState.update {
            it.copy(
                amountDigits = QuickAddValidator.sanitizeDigits(raw),
                errorMessage = null,
            )
        }
    }

    fun onAddQuickPreset(deltaAmount: Long) {
        localState.update { current ->
            val currentAmount = current.amountDigits.toLongOrNull() ?: 0L
            val next = (currentAmount + deltaAmount).coerceAtMost(QuickAddValidator.MAX_AMOUNT)
            current.copy(
                amountDigits = next.toString(),
                errorMessage = null,
            )
        }
    }

    fun onTypeChange(type: TransactionType) {
        localState.update {
            it.copy(
                type = type,
                selectedCategoryId = null,
                errorMessage = null,
            )
        }
    }

    fun onCategorySelected(categoryId: Long) {
        localState.update { it.copy(selectedCategoryId = categoryId, errorMessage = null) }
    }

    fun onNoteChange(note: String) {
        localState.update { it.copy(note = note.take(80)) }
    }

    fun onPaymentMethodSelected(method: PaymentMethod) {
        localState.update { it.copy(paymentMethod = method) }
    }

    fun submit(onSaved: suspend (savedAmount: Long, currency: CurrencyOption) -> Unit) {
        val state = uiState.value
        val validAmount = QuickAddValidator.parseAndValidateAmount(state.amountDigits)
        if (validAmount == null) {
            localState.update {
                it.copy(errorMessage = "Nhập số tiền hợp lệ (từ 1 đến 999.000.000).")
            }
            return
        }

        val selectedCategory = state.availableCategories.firstOrNull { it.id == state.selectedCategoryId }
        val tx = QuickAddValidator.buildTransaction(
            amount = validAmount,
            type = state.type,
            category = selectedCategory,
            note = state.note,
            paymentMethod = state.paymentMethod,
        )

        viewModelScope.launch {
            localState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                transactionRepository.addTransaction(tx)
                onSaved(validAmount, state.currency)
            } catch (e: Exception) {
                localState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Không thể lưu giao dịch: ${e.message ?: "Lỗi DB"}",
                    )
                }
            }
        }
    }

    private data class LocalQuickAddInput(
        val amountDigits: String = "",
        val type: TransactionType = TransactionType.EXPENSE,
        val selectedCategoryId: Long? = null,
        val note: String = "",
        val paymentMethod: PaymentMethod = PaymentMethod.CASH,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
    )
}
