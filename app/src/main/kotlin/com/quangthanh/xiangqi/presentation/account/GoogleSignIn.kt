package com.quangthanh.xiangqi.presentation.account

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.quangthanh.xiangqi.R
import kotlinx.coroutines.launch

/**
 * Mở màn hình chọn tài khoản Google của hệ điều hành (Credential Manager) và trả về ID token.
 * Phần này thuộc về giao diện vì cần Activity để hiện màn hình chọn; xác thực token là việc của AccountService.
 */
@Composable
fun rememberGoogleSignIn(onIdToken: (String) -> Unit, onError: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context) {
        {
            scope.launch {
                try {
                    onIdToken(requestGoogleIdToken(context))
                } catch (e: GetCredentialCancellationException) {
                    // Người dùng tự đóng màn hình chọn tài khoản: không phải lỗi.
                } catch (e: Exception) {
                    Log.w("GoogleSignIn", "Google sign-in failed", e)
                    onError()
                }
            }
        }
    }
}

private suspend fun requestGoogleIdToken(context: Context): String {
    // default_web_client_id do plugin google-services sinh ra từ google-services.json.
    val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(context).getCredential(context, request).credential
    check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        "Unexpected credential type ${credential.type}"
    }
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
