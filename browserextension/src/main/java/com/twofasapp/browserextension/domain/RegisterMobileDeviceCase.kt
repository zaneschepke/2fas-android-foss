package com.twofasapp.browserextension.domain

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.twofasapp.browserextension.domain.model.MobileDevice
import com.twofasapp.browserextension.domain.repository.BrowserExtensionRepository
import com.twofasapp.core.encoding.encodeBase64ToString
import com.twofasapp.environment.AppConfig
import com.twofasapp.push.domain.GetFcmTokenCase
import kotlinx.coroutines.flow.first
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore

internal class RegisterMobileDeviceCase(
    private val browserExtensionRepository: BrowserExtensionRepository,
    private val observeMobileDeviceCase: ObserveMobileDeviceCase,
    private val getFcmTokenCase: GetFcmTokenCase,
    private val appConfig: AppConfig,
) {

    companion object {
        private const val MOBILE_DEVICE_ALIAS = "mobileDeviceRsaKey"
    }

    suspend operator fun invoke(): MobileDevice {
        val fcmToken = getFcmTokenCase()
        val mobileDevice = observeMobileDeviceCase().first()

        return if (mobileDevice.id.isNotBlank()) {
            mobileDevice
        } else {
            browserExtensionRepository.registerMobileDevice(
                deviceName = mobileDevice.name.ifBlank { appConfig.deviceName },
                devicePublicKey = createDevicePublicKey(),
                fcmToken = fcmToken,
            )
        }
    }

    private fun createDevicePublicKey(): String {
        return createRsaKey().public.encoded.encodeBase64ToString()
    }

    private fun createRsaKey(): KeyPair {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry(MOBILE_DEVICE_ALIAS)
        } catch (e: Exception) {
        }

        val generator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(MOBILE_DEVICE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setDigests(KeyProperties.DIGEST_SHA512)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setKeySize(2048)

        generator.initialize(builder.build())

        return generator.generateKeyPair()
    }
}