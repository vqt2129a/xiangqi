package com.quangthanh.xiangqi.application.port

import kotlinx.coroutines.flow.Flow

/** Người đang dùng app. Khách là tài khoản ẩn danh tự tạo, chỉ gắn với lần cài app trên máy này. */
data class Account(
    val uid: String,
    val isGuest: Boolean,
    val displayName: String?,
    val email: String?,
)

/** Port đăng nhập. Tầng use case không biết phía sau là Firebase Auth hay hệ thống nào khác. */
interface AccountService {
    /** null trong lúc chưa xác định được tài khoản (vừa mở app). */
    val account: Flow<Account?>

    /** [idToken] lấy từ màn hình chọn tài khoản Google của hệ điều hành. */
    suspend fun signInWithGoogle(idToken: String): Account

    /** Đăng xuất và quay về tài khoản khách mới. */
    suspend fun signOut()
}
