package com.git.log.common.git.model

import kotlinx.serialization.Serializable

// 仓库数据源类型
enum class SourceRegister(val displayName: String) {
    LOCAL("本地仓库"),
    GITHUB("GitHub"),
    GITLAB("GitLab"),
    BITBUCKET("Bitbucket")
}

@Serializable
data class SourceRegistrationData(
    val id: String = "",
    val register: SourceRegister = SourceRegister.LOCAL,
    val repos: List<RepoElement> = emptyList(),
    val displayPath: String = ""
) {
    @Serializable
    data class RepoElement(
        val represented: Map<String, String> = emptyMap()
    ) {
        object RepresentedKey {
            const val LOCAL_PATH = "localPath"
            const val REMOTE_URL = "remoteUrl"
            const val USERNAME = "username"
            const val TOKEN = "token"
            const val IDENTIFIER = "identifier"
        }

        companion object {
            fun localRepo(path: String) = RepoElement(
                mapOf(RepresentedKey.LOCAL_PATH to path)
            )

            fun remoteRepo(remoteUrl: String, username: String, token: String) = RepoElement(
                mapOf(
                    RepresentedKey.REMOTE_URL to remoteUrl,
                    RepresentedKey.USERNAME to username,
                    RepresentedKey.TOKEN to token,
                    RepresentedKey.IDENTIFIER to remoteUrl.hashCode().toString()
                )
            )
        }
    }
}