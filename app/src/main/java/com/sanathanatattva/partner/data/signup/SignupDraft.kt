package com.sanathanatattva.partner.data.signup

import android.content.Context
import com.sanathanatattva.partner.data.api.apiJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

enum class Vehicle(val wire: String, val motorised: Boolean) {
    Walk("walk", false), Cycle("cycle", false), Scooter("scooter", true), Bike("bike", true);

    companion object {
        fun from(wire: String?) = entries.firstOrNull { it.wire == wire }
    }
}

enum class BaseLabel(val wire: String) { Home("home"), Shop("shop"), Other("other") }

enum class Relation(val wire: String) {
    Mother("mother"), Father("father"), Wife("wife"), Husband("husband"),
    Brother("brother"), Sister("sister"), Friend("friend"), Other("other");

    companion object {
        fun from(wire: String?) = entries.firstOrNull { it.wire == wire }
    }
}

/** The four KYC photos; [field] is the multipart name the backend expects. */
enum class KycPhoto(val field: String) { Selfie("selfie"), LicenceFront("licence_front"), LicenceBack("licence_back"), Pan("pan_photo") }

/**
 * Everything the 8 signup steps collect. Saved on the phone after every change so a
 * rider who gets a call mid-signup comes back to where they were (low-tech users,
 * cheap phones that kill apps in the background).
 */
@Serializable
data class SignupDraft(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    /** Joining with a Google account: email comes from Google and there's no password. */
    val viaGoogle: Boolean = false,
    val baseLabel: BaseLabel = BaseLabel.Home,
    val baseAddress: String = "",
    val baseLat: Double? = null,
    val baseLng: Double? = null,
    val vehicle: Vehicle? = null,
    val plate: String = "",
    val licenceNumber: String = "",
    val pan: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val accountNumberAgain: String = "",
    val ifsc: String = "",
    val bankLabel: String = "",
    val emergencyName: String = "",
    val emergencyRelation: Relation? = null,
    val emergencyPhone: String = "",
    /** Absolute paths of photos taken in the app (inside app storage). */
    val photos: Map<KycPhoto, String> = emptyMap(),
    val confirmed: Boolean = false,
) {
    val motorised: Boolean get() = vehicle?.motorised == true
    fun photo(kind: KycPhoto): File? = photos[kind]?.let(::File)?.takeIf { it.exists() }
}

/** One draft at a time, plus its photos, under filesDir/signup. */
class SignupDraftStore(context: Context) {
    val dir = File(context.filesDir, "signup").apply { mkdirs() }
    private val file = File(dir, "draft.json")

    suspend fun load(): SignupDraft? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        runCatching { apiJson.decodeFromString(SignupDraft.serializer(), file.readText()) }.getOrNull()
    }

    suspend fun save(draft: SignupDraft) = withContext(Dispatchers.IO) {
        val tmp = File(dir, "draft.json.tmp")
        tmp.writeText(apiJson.encodeToString(SignupDraft.serializer(), draft))
        tmp.renameTo(file)
    }

    /** A fresh file for a new photo of [kind]; the previous one is removed once replaced. */
    fun newPhotoFile(kind: KycPhoto): File = File(dir, "${kind.field}-${System.currentTimeMillis()}.jpg")

    suspend fun clear() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
    }
}
