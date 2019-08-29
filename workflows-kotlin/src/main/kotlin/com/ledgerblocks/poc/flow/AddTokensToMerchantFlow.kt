package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.LoanContract
import com.ledgerblocks.poc.contract.TokenContract
import com.ledgerblocks.poc.state.IdentityState
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
class AddTokensToMerchantFlow(private val mUUID: UUID,private val amtToPay: Int): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val mAccountInfo=accountService.accountInfo(mUUID)
        val freshkeyFormAccountInfo= subFlow(RequestKeyForAccountFlow(mAccountInfo!!.state.data))
        val owningKeyFormAccount =accountService.accountInfo(freshkeyFormAccountInfo.owningKey)
        val inputTokenStateReference = serviceHub.vaultService.queryBy<TokenState>().states
        val mToken= inputTokenStateReference.get(inputTokenStateReference.size-1).state.data
        println("borrowerToken-Amount=${mToken.amount}")
        inputTokenStateReference.forEach{tokenState:StateAndRef<TokenState> ->
            val stateAmount = tokenState.state.data.amount
            println("amount=$stateAmount")
        }
        val updatedInputTokenState = mToken.copy(participants = listOf(mAccountInfo.state.data.accountHost), amount = mToken.amount + amtToPay)
        val updatedOutputTokenState = mToken.copy(participants = listOf(mAccountInfo.state.data.accountHost), amount = amtToPay)
        val (command, updatedOwnerState) = updatedOutputTokenState.withNewOwner(freshkeyFormAccountInfo)
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(updatedOwnerState, TokenContract.ID)
                .addOutputState(updatedInputTokenState, TokenContract.ID)
                .addReferenceState(owningKeyFormAccount!!.referenced())
                .addCommand(LoanContract.Commands.Loan(),serviceHub.myInfo.legalIdentities.first().owningKey)
        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder,listOfNotNull(
                ourIdentity.owningKey
        ))
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
        val accountSession = initiateFlow(mAccountInfo!!.state.data.accountHost)
        val sessions = if (!serviceHub.myInfo.isLegalIdentity(mAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession)
        else
            Collections.emptyList()

        return subFlow(FinalityFlow(signedTransaction, sessions))//.coreTransaction.outRefsOfType(TokenState::class.java).filter {it.state.data.toAccountId.equals(mUUID)}.get(0)
    }
}

@InitiatedBy(AddTokensToMerchantFlow::class)
class AddTokensToMerchantFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

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
