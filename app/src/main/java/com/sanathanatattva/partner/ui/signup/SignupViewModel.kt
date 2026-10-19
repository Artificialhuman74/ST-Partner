package com.sanathanatattva.partner.ui.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.GoogleSignIn
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.User
import com.sanathanatattva.partner.data.signup.BankLookup
import com.sanathanatattva.partner.data.signup.KycPhoto
import com.sanathanatattva.partner.data.signup.RiderRepository
import com.sanathanatattva.partner.data.signup.SignupDraft
import com.sanathanatattva.partner.ui.login.UiText
import com.sanathanatattva.partner.ui.login.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

class SignupViewModel(
    private val riders: RiderRepository,
    private val google: GoogleSignIn,
    private val banks: BankLookup,
    private val viaGoogle: Boolean,
) : ViewModel() {

    var draft by mutableStateOf(SignupDraft())
        private set
    var step by mutableStateOf(Step.Basics)
        private set
    var loaded by mutableStateOf(false)
        private set
    /** Field key → message, for the current step only. */
    var errors by mutableStateOf<Map<String, UiText>>(emptyMap())
        private set
    /** Came here with Edit from the review step: Next goes straight back to review. */
    var editingFromReview by mutableStateOf(false)
        private set
    var sending by mutableStateOf(false)
        private set
    var sendError by mutableStateOf<UiText?>(null)
        private set

    private val submitted = Channel<User>(Channel.BUFFERED)
    val submittedEvents = submitted.receiveAsFlow()

    private var saveJob: Job? = null
    private var bankJob: Job? = null

    init {
        viewModelScope.launch {
            var start = riders.drafts.load() ?: SignupDraft()
            if (viaGoogle) {
                val (email, name) = google.currentAccount() ?: (null to null)
                start = start.copy(
                    viaGoogle = true,
                    email = email.orEmpty(),
                    password = "",
                    name = start.name.ifBlank { name.orEmpty() },
                )
            } else if (start.viaGoogle) {
                start = start.copy(viaGoogle = false, email = "")
            }
            draft = start
            // Pick up where they left off: the first step that isn't done yet.
            step = SignupRules.firstIncomplete(start) ?: Step.Review
            if (step != Step.Basics && start.name.isBlank()) step = Step.Basics
            loaded = true
        }
    }

    val steps: List<Step> get() = stepsFor(draft)
    val stepNumber: Int get() = steps.indexOf(step) + 1

    fun update(change: (SignupDraft) -> SignupDraft) {
        val before = draft
        draft = change(before)
        if (errors.isNotEmpty()) errors = emptyMap()
        sendError = null
        if (draft.ifsc != before.ifsc) lookUpBank(draft.ifsc)
        persist()
    }

    fun photoTaken(kind: KycPhoto, file: File) {
        draft.photos[kind]?.let { old -> if (old != file.path) File(old).delete() }
        update { it.copy(photos = it.photos + (kind to file.path)) }
    }

    fun newPhotoFile(kind: KycPhoto): File = riders.drafts.newPhotoFile(kind)

    fun next() {
        val problems = SignupRules.check(step, draft)
        if (problems.isNotEmpty()) {
            errors = problems.mapValues { UiText.Res(it.value) }
            return
        }
        errors = emptyMap()
        step = if (editingFromReview) Step.Review else steps[(steps.indexOf(step) + 1).coerceAtMost(steps.lastIndex)]
        if (step == Step.Review) editingFromReview = false
    }

    /** Returns false when already on the first step (the screen then leaves signup). */
    fun back(): Boolean {
        errors = emptyMap()
        if (editingFromReview) {
            editingFromReview = false
            step = Step.Review
            return true
        }
        val i = steps.indexOf(step)
        if (i <= 0) return false
        step = steps[i - 1]
        return true
    }

    fun edit(target: Step) {
        errors = emptyMap()
        editingFromReview = true
        step = target
    }

    fun submit() {
        if (sending) return
        SignupRules.firstIncomplete(draft)?.let { missing ->
            step = missing
            errors = SignupRules.check(missing, draft).mapValues { UiText.Res(it.value) }
            return
        }
        if (!draft.confirmed) return
        sending = true
        sendError = null
        viewModelScope.launch {
            saveJob?.join()
            when (val result = riders.submit(draft)) {
                is ApiResult.Success -> submitted.send(result.value)
                is ApiResult.Failure -> showServerProblem(result.problem)
            }
            sending = false
        }
    }

    /** Server-side checks map back to the step that owns the field. */
    private fun showServerProblem(problem: ApiProblem) {
        val fields = (problem as? ApiProblem.Server)?.fields.orEmpty()
        val owner = fields.keys.firstNotNullOfOrNull(::stepOfField)
        if (owner != null && owner in steps) {
            step = owner
            editingFromReview = true
            errors = fields.mapKeys { localField(it.key) }.mapValues { UiText.Raw(it.value) }
        } else {
            sendError = if (problem is ApiProblem.Server && problem.code == 401 && draft.viaGoogle) {
                UiText.Res(R.string.login_google_failed)
            } else {
                problem.toUiText()
            }
        }
    }

    private fun lookUpBank(ifsc: String) {
        bankJob?.cancel()
        if (!SignupRules.isIfsc(ifsc)) {
            if (draft.bankLabel.isNotEmpty()) draft = draft.copy(bankLabel = "")
            return
        }
        bankJob = viewModelScope.launch {
            val label = banks.describe(ifsc).orEmpty()
            if (draft.ifsc == ifsc) {
                draft = draft.copy(bankLabel = label)
                persist()
            }
        }
    }

    private fun persist() {
        saveJob?.cancel()
        val snapshot = draft
        saveJob = viewModelScope.launch {
            delay(250)
            riders.drafts.save(snapshot)
        }
    }

    companion object {
        fun stepOfField(field: String): Step? = when (field) {
            "name", "phone", "email", "password" -> Step.Basics
            "base_address", "base_lat", "base_lng", "base_label" -> Step.Base
            "selfie" -> Step.Selfie
            "vehicle_type", "vehicle_plate" -> Step.Vehicle
            "licence_number", "licence_front", "licence_back" -> Step.Licence
            "pan", "pan_photo", "bank_account_name", "bank_account_number", "bank_ifsc" -> Step.Bank
            "emergency_name", "emergency_relation", "emergency_phone" -> Step.Emergency
            else -> null
        }

        /** Server field names → the keys the step screens show messages under. */
        fun localField(field: String): String = when (field) {
            "base_address", "base_lat", "base_lng", "base_label" -> "base"
            "vehicle_type" -> "vehicle"
            "vehicle_plate" -> "plate"
            "licence_front", "licence_back" -> "licence_photos"
            "bank_account_name" -> "account_name"
            "bank_account_number" -> "account_number"
            "bank_ifsc" -> "ifsc"
            "emergency_relation" -> "relation"
            else -> field
        }
    }
}
