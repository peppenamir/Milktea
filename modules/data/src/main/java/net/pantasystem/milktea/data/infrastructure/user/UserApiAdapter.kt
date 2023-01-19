package net.pantasystem.milktea.data.infrastructure.user

import net.pantasystem.milktea.api.misskey.users.RequestUser
import net.pantasystem.milktea.common.throwIfHasError
import net.pantasystem.milktea.data.api.mastodon.MastodonAPIProvider
import net.pantasystem.milktea.data.api.misskey.MisskeyAPIProvider
import net.pantasystem.milktea.data.infrastructure.toUser
import net.pantasystem.milktea.data.infrastructure.toUserRelated
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.account.AccountRepository
import net.pantasystem.milktea.model.user.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserApiAdapter @Inject constructor(
    val accountRepository: AccountRepository,
    val misskeyAPIProvider: MisskeyAPIProvider,
    val mastodonAPIProvider: MastodonAPIProvider,
){

    suspend fun show(userId: User.Id, detail: Boolean): User {
        val account = accountRepository.get(userId.accountId).getOrThrow()
        return when(account.instanceType) {
            Account.InstanceType.MISSKEY -> {
                val res = misskeyAPIProvider.get(account).showUser(
                    RequestUser(
                        i = account.token,
                        userId = userId.id,
                        detail = detail,
                    )
                )
                requireNotNull(res.throwIfHasError().body()).toUser(account, detail)
            }
            Account.InstanceType.MASTODON -> {
                val res = mastodonAPIProvider.get(account).getAccount(userId.id)
                    .throwIfHasError()
                    .body()
                requireNotNull(res)
                if (detail) {
                    val relationship = mastodonAPIProvider.get(account).getAccountRelationships(
                        listOf(res.id)
                    ).throwIfHasError().body()
                    val related = requireNotNull(relationship).first().toUserRelated()
                    res.toModel(account, related)
                } else {
                    res.toModel(account)
                }
            }
        }
    }

    suspend fun follow(userId: User.Id): Boolean {
        val account = accountRepository.get(userId.accountId).getOrThrow()
        return when(account.instanceType) {
            Account.InstanceType.MISSKEY -> {
                misskeyAPIProvider.get(account).followUser(
                    RequestUser(userId = userId.id, i = account.token)
                )
            }
            Account.InstanceType.MASTODON -> {
                mastodonAPIProvider.get(account).follow(userId.id)
            }
        }.throwIfHasError().isSuccessful
    }

    suspend fun unfollow(userId: User.Id): Boolean {
        val account = accountRepository.get(userId.accountId).getOrThrow()
        return when(account.instanceType) {
            Account.InstanceType.MISSKEY -> misskeyAPIProvider.get(account)
                .unFollowUser(RequestUser(userId = userId.id, i = account.token))
                .throwIfHasError()
                .isSuccessful
            Account.InstanceType.MASTODON -> mastodonAPIProvider.get(account).unfollow(userId.id)
                .throwIfHasError()
                .isSuccessful
        }
    }
}