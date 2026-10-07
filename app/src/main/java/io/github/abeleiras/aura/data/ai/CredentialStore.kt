package io.github.abeleiras.aura.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import io.github.abeleiras.aura.domain.secrets.SecretBox
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * API keys sealed with a non-exportable Android Keystore key (FR-002-05). The preferences file is called
 * `secrets` because the backup rules exclude exactly that name; and even if it were copied, the Keystore key
 * doesn't travel, so the blob would be useless. Nothing here is ever logged.
 */
class CredentialStore(context: Context) {
    private val prefs = context.getSharedPreferences("secrets", Context.MODE_PRIVATE)
    private val box: SecretBox by lazy { SecretBox(keystoreKey()) }

    fun get(name: String): String? {
        val stored = prefs.getString(name, null) ?: return null
        return try {
            val plain = box.open(Base64.decode(stored, Base64.NO_WRAP))
            if (plain == null) prefs.edit { remove(name) } // unreadable (e.g. restored on another phone): ask again
            plain?.toString(Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Keystore unavailable: ${e.javaClass.simpleName}")
            null
        } catch (e: IllegalArgumentException) {
            prefs.edit { remove(name) }
            null
        }
    }

    /** False if the Keystore refused; the caller must tell the user rather than pretend it was saved. */
    fun put(name: String, value: String): Boolean = try {
        val blob = box.seal(value.toByteArray(Charsets.UTF_8))
        prefs.edit { putString(name, Base64.encodeToString(blob, Base64.NO_WRAP)) }
        true
    } catch (e: GeneralSecurityException) {
        Log.w(TAG, "Keystore unavailable: ${e.javaClass.simpleName}")
        false
    }

    fun has(name: String): Boolean = prefs.contains(name)

    fun remove(name: String) = prefs.edit { remove(name) }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val GEMINI_KEY = "gemini_api_key"
        private const val TAG = "CredentialStore"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "aura_credentials_v1"
    }
}
