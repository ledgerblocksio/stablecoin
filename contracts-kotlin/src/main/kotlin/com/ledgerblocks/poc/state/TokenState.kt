package com.ledgerblocks.poc.state

import com.ledgerblocks.poc.contract.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

import java.security.PublicKey
import java.util.*


@BelongsToContract(TokenContract::class)
data class TokenState(

        val tokenBalance: Int, // val tokenBalance: Int  val transTokens: Int
        val txAmount: Int,        //val amount of tokens transacted in transaction
        val fromAccountId: UUID? =null,    // replaces accountID and refers to the entity holding tokens before the move/issue
        val toAccountId: UUID? =null,      // represents entity, who is getting tokens
        val purpose:String,
        val date: String,
        val owningKey: PublicKey? = null,
        override val owner: AbstractParty,

        // override val participants1: List<Party> = listOf(sender, recipient)
        override val participants: List<AbstractParty>): OwnableState {

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(TokenContract.Commands.Transfer(), copy(owner = newOwner))
    }
}
