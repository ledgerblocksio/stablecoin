package com.ledgerblocks.poc.contract

import com.ledgerblocks.poc.state.IdentityState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction

class IdentityContract : Contract {

    data class IdentityCommands(private val step: String) : CommandData

    companion object {
        val OPEN = IdentityCommands("OPEN")

    }

    override fun verify(tx: LedgerTransaction) {
        val identityCommand = tx.commands.requireSingleCommand(IdentityCommands::class.java)
        if (identityCommand.value == OPEN) {
            require(tx.outputStates.size == 1) { "There should only ever be one output account state" }
            val identityState = tx.outputsOfType(IdentityState::class.java).single()
            val requiredSigners = identityCommand.signers
            require(requiredSigners.size == 1) { "There should only be one required signer for opening an account " }
            require(requiredSigners.single() == identityState.host.owningKey) { "Only the hosting node should be able to sign" }
        } else {
            throw NotImplementedError()
        }
    }

}
