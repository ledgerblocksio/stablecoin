package com.ledgerblocks.poc.state

import com.ledgerblocks.poc.contract.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import java.security.PublicKey
import java.util.*


@BelongsToContract(TokenContract::class)
data class TokenState(
        val amount: Int,
        val accountId: UUID,
        val owningKey: PublicKey? = null,
        override val owner: AbstractParty,

        override val participants: List<AbstractParty>): OwnableState {
    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(TokenContract.Commands.Transfer(), copy(owner = newOwner))
    }
}
