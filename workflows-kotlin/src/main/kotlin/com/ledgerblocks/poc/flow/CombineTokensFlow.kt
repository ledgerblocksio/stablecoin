package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.TokenContract
import com.ledgerblocks.poc.state.TokenState
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder


/*
@InitiatingFlow
@StartableByRPC
class CombineTokensFlow: FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)

        val myTokens = serviceHub.vaultService.queryBy<TokenState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).states
        var totalAmount = 0

        if(!myTokens.isEmpty()){
            for(states in myTokens){
                transactionBuilder.addInputState(states)
                totalAmount += states.state.data.amount
            }
        }

        val outputState = TokenState(totalAmount,null, ourIdentity, listOf(ourIdentity))

        transactionBuilder.addOutputState(outputState, TokenContract.ID)
                .addCommand(TokenContract.Commands.Combine(), listOf(ourIdentity.owningKey))

        val signedTx = serviceHub.signInitialTransaction(transactionBuilder)

        transactionBuilder.verify(serviceHub)

        //return subFlow(FinalityFlow(signedTx))

        return subFlow(FinalityFlow(signedTx, emptyList()))
    }
}*/
