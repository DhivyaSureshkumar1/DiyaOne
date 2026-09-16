package com.naminfo.ui.call.model

import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.R
import org.linphone.core.Call
import org.linphone.core.MediaEncryption
import org.linphone.core.StreamType
import com.naminfo.utils.AppUtils

class CallMediaEncryptionModel
    @WorkerThread
    constructor(
    private val showZrtpSasValidationDialog: () -> Unit
) {
    val mediaEncryption = MutableLiveData<String>()

    val isMediaEncryptionZrtp = MutableLiveData<Boolean>()
    val zrtpCipher = MutableLiveData<String>()
    val zrtpKeyAgreement = MutableLiveData<String>()
    val zrtpHash = MutableLiveData<String>()
    val zrtpAuthTag = MutableLiveData<String>()
    val zrtpAuthSas = MutableLiveData<String>()

    @WorkerThread
    fun update(call: Call) {
        isMediaEncryptionZrtp.postValue(false)

        val stats = call.getStats(StreamType.Audio)
        if (stats != null) {
            // ZRTP stats are only available when authentication token isn't null !
            if (call.currentParams.mediaEncryption == MediaEncryption.ZRTP && call.authenticationToken != null) {
                isMediaEncryptionZrtp.postValue(true)

                if (stats.isZrtpKeyAgreementAlgoPostQuantum) {
                    mediaEncryption.postValue(
                        AppUtils.getFormattedString(
                            R.string.call_stats_media_encryption,
                            AppUtils.getString(
                                R.string.call_stats_media_encryption_zrtp_post_quantum
                            )
                        )
                    )
                } else {
                    mediaEncryption.postValue(
                        AppUtils.getFormattedString(
                            R.string.call_stats_media_encryption,
                            call.currentParams.mediaEncryption.name
                        )
                    )
                }

                zrtpCipher.postValue(
                    AppUtils.getFormattedString(
                        R.string.call_stats_zrtp_cipher_algo,
                        stats.zrtpCipherAlgo
                    )
                )
                zrtpKeyAgreement.postValue(
                    AppUtils.getFormattedString(
                        R.string.call_stats_zrtp_key_agreement_algo,
                        stats.zrtpKeyAgreementAlgo
                    )
                )
                zrtpHash.postValue(
                    AppUtils.getFormattedString(
                        R.string.call_stats_zrtp_hash_algo,
                        stats.zrtpHashAlgo
                    )
                )
                zrtpAuthTag.postValue(
                    AppUtils.getFormattedString(
                        R.string.call_stats_zrtp_auth_tag_algo,
                        stats.zrtpAuthTagAlgo
                    )
                )
                zrtpAuthSas.postValue(
                    AppUtils.getFormattedString(
                        R.string.call_stats_zrtp_sas_algo,
                        stats.zrtpSasAlgo
                    )
                )
            } else {
                mediaEncryption.postValue(
                    AppUtils.getFormattedString(
                        R.string.call_stats_media_encryption,
                        call.currentParams.mediaEncryption.name
                    )
                )
            }
        }
    }

    fun showSasValidationDialog() {
        showZrtpSasValidationDialog.invoke()
    }
}
