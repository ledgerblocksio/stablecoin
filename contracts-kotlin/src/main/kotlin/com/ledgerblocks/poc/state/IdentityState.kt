package com.ledgerblocks.poc.state

import com.ledgerblocks.poc.contract.IdentityContract
import net.corda.accounts.contracts.AccountInfoContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(IdentityContract::class)
data class IdentityState(
        val name: String,
        val IMEI: String,
        val uuid: UUID,
        val host: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) :
        LinearState {

    override val participants: List<AbstractParty> get() = listOf(host)
}