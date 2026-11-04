package su.afk.yummy.tv.source.anistar.data.torrent

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the endpoint separately and encrypts credentials with an Android Keystore AES key.
 * If encrypted credentials cannot be decrypted (for example after key invalidation), they are
 * discarded rather than silently falling back to plaintext storage.
 */
interface TorrServerConfigurationStore {
    fun load(): TorrServerConfiguration
    fun save(configuration: TorrServerConfiguration)
}

@Singleton
class AndroidTorrServerConfigurationStore @Inject constructor(
    @param:ApplicationContext context: Context,
) : TorrServerConfigurationStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): TorrServerConfiguration {
        val baseUrl = preferences.getString(BASE_URL_KEY, null)
            ?.takeIf(String::isNotBlank)
            ?: DEFAULT_BASE_URL
        val credentials = preferences.getString(CREDENTIALS_KEY, null)
            ?.let(::decrypt)
            ?.split('\n', limit = 2)
        return TorrServerConfiguration(
            baseUrl = baseUrl,
            username = credentials?.getOrNull(0).orEmpty(),
            password = credentials?.getOrNull(1).orEmpty(),
        )
    }

    override fun save(configuration: TorrServerConfiguration) {
        val editor = preferences.edit().putString(BASE_URL_KEY, configuration.baseUrl)
        if (configuration.username.isBlank() && configuration.password.isBlank()) {
            editor.remove(CREDENTIALS_KEY)
        } else {
            editor.putString(CREDENTIALS_KEY, encrypt(configuration.username + "\n" + configuration.password))
        }
        editor.apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? = runCatching {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size > GCM_IV_BYTES)
        val iv = bytes.copyOfRange(0, GCM_IV_BYTES)
        val encrypted = bytes.copyOfRange(GCM_IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "anistar_torrserver"
        const val BASE_URL_KEY = "base_url"
        const val CREDENTIALS_KEY = "credentials_encrypted"
        const val DEFAULT_BASE_URL = "http://127.0.0.1:8090"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "yummytv.anistar.torrserver.credentials"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
