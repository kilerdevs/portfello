package com.portfello.data.crypto

import android.content.Context
import android.util.Base64
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2KtResult
import com.lambdapioneer.argon2kt.Argon2Mode
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.SecureRandom
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
