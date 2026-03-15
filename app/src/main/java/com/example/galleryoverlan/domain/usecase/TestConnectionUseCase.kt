package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.core.result.AppResult
import com.example.galleryoverlan.data.smb.SmbRepository
import javax.inject.Inject

class TestConnectionUseCase @Inject constructor(
    private val smbRepository: SmbRepository
) {
    suspend operator fun invoke(
        host: String,
        shareName: String,
        userName: String,
        password: String
    ): AppResult<Unit> {
        if (host.isBlank()) return AppResult.Error(
            IllegalArgumentException("PC名またはIPを入力してください")
        )
        if (shareName.isBlank()) return AppResult.Error(
            IllegalArgumentException("共有名を入力してください")
        )
        if (userName.isBlank()) return AppResult.Error(
            IllegalArgumentException("ユーザー名を入力してください")
        )
        if (password.isBlank()) return AppResult.Error(
            IllegalArgumentException("パスワードを入力してください")
        )
        return smbRepository.testConnection(host, shareName, userName, password)
    }
}
