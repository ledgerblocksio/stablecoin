package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.TokenContract
import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*


@StartableByRPC
@InitiatingFlow
class MoveTokensBetweenAccounts(private val bUUID: UUID, private val mUUID: UUID,private val purchaseAmount: Int): FlowLogic<StateAndRef<TokenState>>() {

    @Suspendable
    override fun call(): StateAndRef<TokenState> {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
       // subFlow(CombineTokensFlow())
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val bAccountInfo=accountService.accountInfo(bUUID)
        val allAccs=accountService.allAccounts()
        val myAccs=accountService.myAccounts()
        val mAccountInfo=accountService.accountInfo(mUUID)
        val freshkeyForbAccountInfo= subFlow(RequestKeyForAccountFlow(bAccountInfo!!.state.data))
        val freshkeyFormAccountInfo= subFlow(RequestKeyForAccountFlow(mAccountInfo!!.state.data))
        val owningKeyForbAccount =accountService.accountInfo(freshkeyForbAccountInfo.owningKey)
        val owningKeyFormAccount =accountService.accountInfo(freshkeyFormAccountInfo.owningKey)
        val signingAccounts = listOfNotNull(owningKeyForbAccount, owningKeyFormAccount)
        val requiredSigners =
                signingAccounts.map { it.state.data.accountHost.owningKey } + listOfNotNull(
                        freshkeyForbAccountInfo.owningKey, freshkeyFormAccountInfo.owningKey,ourIdentity.owningKey)
        val inputTokenStateReference = serviceHub.vaultService.queryBy<TokenState>().states
        val borrowerToken= inputTokenStateReference.get(inputTokenStateReference.size-1).state.data
        println("borrowerToken-Amount=${borrowerToken.amount}")
        inputTokenStateReference.forEach{tokenState:StateAndRef<TokenState> ->
            val stateAmount = tokenState.state.data.amount
            println("amount=$stateAmount")
        }
        val updatedInputTokenState = borrowerToken.copy(participants = listOf(bAccountInfo.state.data.accountHost), amount = borrowerToken.amount - purchaseAmount)
        val updatedOutputTokenState = borrowerToken.copy(participants = listOf(mAccountInfo.state.data.accountHost), amount = purchaseAmount)
        val (command, updatedOwnerState) = updatedOutputTokenState.withNewOwner(freshkeyFormAccountInfo)
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(updatedOwnerState, TokenContract.ID)
                .addOutputState(updatedInputTokenState, TokenContract.ID)
                .addCommand(command, requiredSigners)
                .addReferenceState(owningKeyForbAccount!!.referenced())
                .addReferenceState(owningKeyFormAccount!!.referenced())
        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder,listOfNotNull(
                ourIdentity.owningKey
        ))
        val accountSession1 = initiateFlow(bAccountInfo.state.data.accountHost)
        val accountSession = initiateFlow(mAccountInfo.state.data.accountHost)
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(accountSession,accountSession1)))
        val sessions = if (!serviceHub.myInfo.isLegalIdentity(mAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession)
        else
            Collections.emptyList()
        val sessions1 = if (!serviceHub.myInfo.isLegalIdentity(bAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession1)
        else
            Collections.emptyList()

        return  subFlow(FinalityFlow(fullySignedTransaction, sessions)).coreTransaction.outRefsOfType(TokenState::class.java).filter {it.state.data.accountId.equals(bUUID)}.get(0)
    }
}

@InitiatedBy(MoveTokensBetweenAccounts::class)
class MoveTokensBetweenAccountsResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

    @Suspendable
    final override fun call(): SignedTransaction {

        val stx = subFlow(object : SignTransactionFlow(otherPartySession) {

            override fun checkTransaction(stx: SignedTransaction) {
                //extraTransactionValidation(stx)
            }
        })
        val tx = subFlow(
                ReceiveFinalityFlow(
                        otherSideSession = otherPartySession,
                        expectedTxId = stx.id
                )
        )
        logger.info("Received transaction from finality")
        return tx
    }
}
