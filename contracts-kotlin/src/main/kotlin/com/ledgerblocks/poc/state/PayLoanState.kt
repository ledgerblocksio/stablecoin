package com.ledgerblocks.poc.state

import com.ledgerblocks.poc.contract.LoanContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract( LoanContract::class)
data class PayLoanState(
        val bUuid: UUID,
        val lbUuid: UUID,
        val payment: String,
        val host: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) :
        LinearState {

    override val participants: List<AbstractParty> get() = listOf(host)
}
