package net.pantasystem.milktea.data.infrastructure.account

import net.pantasystem.milktea.app_store.account.AccountStore
import net.pantasystem.milktea.common.mapCancellableCatching
import net.pantasystem.milktea.common.runCancellableCatching
import net.pantasystem.milktea.data.streaming.SocketWithAccountProvider
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.account.AccountRepository
import net.pantasystem.milktea.model.account.SignOutUseCase
import net.pantasystem.milktea.model.sw.register.SubscriptionUnRegistration
import javax.inject.Inject


class SignOutUseCaseImpl @Inject constructor(
    private val accountRepository: AccountRepository,
    private val subscriptionUnRegistration: SubscriptionUnRegistration,
    private val socketWithAccountProvider: SocketWithAccountProvider,
    private val accountStore: AccountStore,
): SignOutUseCase {

    override suspend fun invoke(account: Account): Result<Unit> {
        return runCancellableCatching {
            subscriptionUnRegistration
                .unregister(account.accountId)
        }.mapCancellableCatching {
            accountRepository.delete(account)
        }.mapCancellableCatching {
            socketWithAccountProvider.get(account.accountId)?.disconnect()
        }.mapCancellableCatching {
            accountStore.initialize()
        }
    }
}