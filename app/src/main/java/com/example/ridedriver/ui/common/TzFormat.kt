package com.example.ridedriver.ui.common

/** Single source of truth for Tanzanian phone, money, email, OTP rules. Pure, unit-testable. */

object TzPhone {
    private val PREFIX_TO_PROVIDER = mapOf(
        "65" to "Tigo Pesa", "67" to "Tigo Pesa", "71" to "Tigo Pesa",
        "61" to "Airtel Money", "68" to "Airtel Money", "78" to "Airtel Money",
        "74" to "M-Pesa", "75" to "M-Pesa", "76" to "M-Pesa", "77" to "M-Pesa",
        "62" to "Halopesa", "63" to "Halopesa", "79" to "Halopesa", "64" to "Halopesa"
    )

    /** 0712... / 712... / +255712... -> +255712... or null when invalid. */
    fun normalize(raw: String): String? {
        if (raw.any { !it.isDigit() && !it.isWhitespace() && it !in "+()-" }) return null
        val digits = raw.mapNotNull { it.digitToIntOrNull()?.digitToChar() }.joinToString("")
        val local = when {
            digits.startsWith("255") && digits.length == 12 -> digits.drop(3)
            digits.startsWith("0") && digits.length == 10 -> digits.drop(1)
            digits.length == 9 -> digits
            else -> return null
        }
        if (local.length != 9 || !local.startsWith("6") && !local.startsWith("7")) return null
        return "+255$local"
    }

    fun providerOf(normalized: String): String? {
        val local = normalized.removePrefix("+255")
        if (local.length < 2) return null
        return PREFIX_TO_PROVIDER[local.take(2)]
    }

    fun validateForProvider(raw: String, provider: String): String? {
        val n = normalize(raw) ?: return "Namba si sahihi"
        val actual = providerOf(n)
        if (actual != null && actual != provider) return "Namba hii ni $actual, si $provider"
        return null
    }

    fun mask(normalized: String): String {
        val local = normalized.removePrefix("+255")
        if (local.length != 9) return "+255 *** *** ***"
        return "+255 ${local.take(1)}** *** ${local.takeLast(3)}"
    }
}

object TzMoney {
    fun format(amountTzs: Int): String {
        if (amountTzs < 0) return "TSh 0"
        val s = amountTzs.toString().reversed().chunked(3).joinToString(",").reversed()
        return "TSh $s"
    }
}

object TzEmail {
    private val RE = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    fun validate(raw: String): String? {
        val v = raw.trim()
        if (v.isEmpty()) return "Weka barua pepe"
        if (!RE.matches(v)) return "Barua pepe si sahihi"
        return null
    }

    fun mask(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "****"
        val domain = parts[1]
        return "****@$domain"
    }
}

object Masking {
    fun maskPhone(normalized: String) = TzPhone.mask(normalized)
    fun maskEmail(email: String) = TzEmail.mask(email)
}

/** OTP: 6 boxes, attempts, lockout. Pure logic for tests. */
data class OtpState(
    val code: String = "",
    val attemptsLeft: Int = 5,
    val locked: Boolean = false,
    val resendInSec: Int = 0,
    val error: String? = null
) {
    fun canResend() = resendInSec <= 0 && !locked
}

fun otpEnter(current: OtpState, digit: Char): OtpState {
    if (current.locked || current.code.length >= 6 || !digit.isDigit()) return current
    return current.copy(code = current.code + digit, error = null)
}

fun otpClear(current: OtpState) = current.copy(code = "", error = null)

/** Returns new state after submitting; onOk invoked by caller when code matches expected. */
fun otpSubmit(current: OtpState, expected: String): OtpState {
    if (current.locked) return current.copy(error = "Umefungiwa. Jaribu baadaye")
    if (current.code != expected) {
        val left = current.attemptsLeft - 1
        return if (left <= 0) current.copy(attemptsLeft = 0, locked = true, error = "Majaribio yameisha")
        else current.copy(attemptsLeft = left, error = "Kodi si sahihi")
    }
    return current.copy(error = null)
}

fun otpTick(current: OtpState): OtpState =
    if (current.resendInSec > 0) current.copy(resendInSec = current.resendInSec - 1) else current

/** Support form validation. Pure. */
object SupportFormRules {
    const val MIN_DESC = 10
    const val MAX_DESC = 1000
    const val MAX_ATTACH = 3

    fun subjectError(v: String): String? = if (v.trim().length < 3) "Weka kichwa" else null
    fun descError(v: String): String? = when {
        v.trim().length < MIN_DESC -> "Eleza zaidi (herufi $MIN_DESC+)"
        v.trim().length > MAX_DESC -> "Maelezo marefu sana"
        else -> null
    }
}

object NameRules {
    fun error(v: String): String? {
        val t = v.trim()
        if (t.length < 2) return "Jina fupi sana"
        if (t.length > 50) return "Jina refu sana"
        return null
    }
}
