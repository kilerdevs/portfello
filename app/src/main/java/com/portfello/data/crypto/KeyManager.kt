package com.portfello.data.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import com.lambdapioneer.argon2kt.Argon2Mode
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val moshi = Moshi.Builder().build()
    private val configAdapter = moshi.adapter(CryptoConfig::class.java)
    private val argon2 = Argon2Kt()

    private val configFile: File
        get() = File(context.filesDir, "crypto_config.json")

    @Volatile
    private var cachedDbKey: ByteArray? = null

    val isSetUp: Boolean
        get() = configFile.exists()

    val isUnlocked: Boolean
        get() = cachedDbKey != null

    fun setupPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = deriveKey(pin, salt)
        val key = hash.rawHashAsByteArray()
        val config = CryptoConfig(
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            pinHash = Base64.encodeToString(sha256(key), Base64.NO_WRAP),
            version = 2
        )
        configFile.writeText(configAdapter.toJson(config))
        cachedDbKey = key
    }

    fun verifyAndUnlock(pin: String): Boolean {
        val config = loadConfig() ?: return false
        val salt = Base64.decode(config.salt, Base64.NO_WRAP)
        val key = deriveKey(pin, salt, config).rawHashAsByteArray()
        val expected = Base64.decode(config.pinHash, Base64.NO_WRAP)
        // v1 stored the raw key as pinHash (plaintext DB key on disk); v2 stores SHA-256 of it
        val matches = if (config.version >= 2) {
            sha256(key).contentEquals(expected)
        } else {
            key.contentEquals(expected)
        }
        if (matches) {
            cachedDbKey = key
            if (config.version < 2) {
                // migrate legacy config: replace stored key with its digest
                configFile.writeText(configAdapter.toJson(config.copy(
                    pinHash = Base64.encodeToString(sha256(key), Base64.NO_WRAP),
                    version = 2
                )))
            }
        }
        return matches
    }

    private fun sha256(data: ByteArray): ByteArray =
        java.security.MessageDigest.getInstance("SHA-256").digest(data)

    // --- Biometric unlock: DB key wrapped by a biometric-gated Keystore AES-GCM key ---

    val biometricEnabled: Boolean
        get() = loadConfig()?.biometricWrappedKey != null

    /** Cipher for wrapping the DB key; must be authorized via BiometricPrompt.CryptoObject. */
    fun getBiometricEncryptCipher(): Cipher? = try {
        Cipher.getInstance(CIPHER).apply { init(Cipher.ENCRYPT_MODE, getOrCreateBiometricKey()) }
    } catch (_: Exception) {
        null
    }

    /** Persists the wrapped DB key; call with the cipher returned by a successful ENCRYPT prompt. */
    fun enableBiometric(cipher: Cipher): Boolean {
        val key = cachedDbKey ?: return false
        val config = loadConfig() ?: return false
        return try {
            val wrapped = cipher.doFinal(key)
            configFile.writeText(configAdapter.toJson(config.copy(
                biometricWrappedKey = Base64.encodeToString(wrapped, Base64.NO_WRAP),
                biometricIv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            )))
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Cipher for unwrapping; null when biometrics are off or the Keystore key was invalidated. */
    fun getBiometricDecryptCipher(): Cipher? {
        val config = loadConfig() ?: return null
        val iv = config.biometricIv ?: return null
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val key = ks.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey ?: return null
            Cipher.getInstance(CIPHER).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            }
        } catch (_: KeyPermanentlyInvalidatedException) {
            disableBiometric() // fingerprint enrollment changed — require PIN and re-enable
            null
        } catch (_: Exception) {
            null
        }
    }

    /** Unwraps the DB key with the cipher returned by a successful DECRYPT prompt. */
    fun unlockWithBiometricCipher(cipher: Cipher): Boolean {
        val wrappedB64 = loadConfig()?.biometricWrappedKey ?: return false
        return try {
            cachedDbKey = cipher.doFinal(Base64.decode(wrappedB64, Base64.NO_WRAP))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun disableBiometric() {
        loadConfig()?.let {
            configFile.writeText(configAdapter.toJson(it.copy(biometricWrappedKey = null, biometricIv = null)))
        }
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(BIOMETRIC_KEY_ALIAS)
        } catch (_: Exception) {
        }
    }

    private fun getOrCreateBiometricKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(-1)
                }
            }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val BIOMETRIC_KEY_ALIAS = "portfello_biometric"
        private const val CIPHER = "AES/GCM/NoPadding"
    }

    fun getDatabaseKey(): ByteArray =
        cachedDbKey ?: throw IllegalStateException("Database not unlocked")

    fun lock() {
        cachedDbKey?.fill(0)
        cachedDbKey = null
    }

    fun changePin(oldPin: String, newPin: String): ByteArray? {
        if (!verifyAndUnlock(oldPin)) return null
        val oldKey = cachedDbKey!!.copyOf()
        setupPin(newPin)
        return oldKey
    }

    private fun loadConfig(): CryptoConfig? {
        if (!configFile.exists()) return null
        return configAdapter.fromJson(configFile.readText())
    }

    private fun deriveKey(
        pin: String,
        salt: ByteArray,
        config: CryptoConfig? = null
    ): Argon2KtResult {
        return argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = pin.toByteArray(Charsets.UTF_8),
            salt = salt,
            tCostInIterations = config?.argonTimeCost ?: 3,
            mCostInKibibyte = config?.argonMemoryCost ?: 65536,
            parallelism = config?.argonParallelism ?: 2,
            hashLengthInBytes = 32 // AES-256
        )
    }
}
