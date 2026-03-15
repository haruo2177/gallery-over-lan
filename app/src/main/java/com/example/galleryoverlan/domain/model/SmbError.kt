package com.example.galleryoverlan.domain.model

sealed class SmbError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    class AuthenticationFailed(cause: Throwable? = null) :
        SmbError("ユーザー名またはパスワードを確認してください", cause)

    class ShareNotFound(val shareName: String, cause: Throwable? = null) :
        SmbError("PCに接続できません。PC名またはネットワークを確認してください", cause)

    class PathNotFound(val path: String, cause: Throwable? = null) :
        SmbError("指定フォルダが見つかりません: $path", cause)

    class PermissionDenied(cause: Throwable? = null) :
        SmbError("アクセス権限がありません", cause)

    class NetworkUnreachable(cause: Throwable? = null) :
        SmbError("PCに接続できません。PC名またはネットワークを確認してください", cause)

    class Timeout(cause: Throwable? = null) :
        SmbError("接続がタイムアウトしました", cause)

    class Unknown(cause: Throwable) :
        SmbError("予期しないエラーが発生しました: ${cause.message}", cause)
}
