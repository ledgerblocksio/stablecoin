package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.TokenContract
import com.ledgerblocks.poc.state.IdentityState
import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*


@StartableByRPC
@InitiatingFlow
class MoveTokensBetweenAccounts(private val bUUID: UUID, private val mUUID: UUID, private val purchaseAmount: Int, private val purpose: String): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // subFlow(CombineTokensFlow())
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

        val accountInfo1=accountService.accountInfo(bUUID)
        // println("b acc test =$bAccountInfo")

        val party1 = accountInfo1!!.state.data.accountHost
        println("Borrower Party: ${party1}")
        val allAccs=accountService.allAccounts()
        // println("all test=$allAccs")

        val myAccs=accountService.myAccounts()
        //println("myAccs test =$myAccs")
        val accountInfo2=accountService.accountInfo(mUUID)
        val party2 = accountInfo2!!.state.data.accountHost
        println("Merchant Party: ${party2}")

        // println("m acc test =$mAccountInfo")

        //val purpose = ""

        val freshkeyForbAccountInfo= subFlow(RequestKeyForAccountFlow(accountInfo1!!.state.data))

        val freshkeyFormAccountInfo= subFlow(RequestKeyForAccountFlow(accountInfo2!!.state.data))
        //val accountInfoToMoveTo = accountService.accountInfo(mAccountInfo.state.data.accountId)

        val owningKeyForbAccount =accountService.accountInfo(freshkeyForbAccountInfo.owningKey)

        val owningKeyFormAccount =accountService.accountInfo(freshkeyFormAccountInfo.owningKey)
        //val bParty = accountService.accountInfo((freshkeyForbAccountInfo.))

        val signingAccounts = listOfNotNull(owningKeyForbAccount, owningKeyFormAccount)

        val requiredSigners =
                signingAccounts.map { it.state.data.accountHost.owningKey } + listOfNotNull(
                        freshkeyForbAccountInfo.owningKey, freshkeyFormAccountInfo.owningKey,ourIdentity.owningKey)

        //val inputTokenStateReference = serviceHub.vaultService.queryBy<TokenState>().states.first()

        //val minputTokenStateReference = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.accountId.equals(mUUID) }

        // val merchantToken= minputTokenStateReference.get(minputTokenStateReference.size-1).state.data

        var inputTokenStateReference1: List<StateAndRef<TokenState>>? = null

        if (purpose.equals("purchase"))
            inputTokenStateReference1 = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.fromAccountId.equals(bUUID) }
        else if (purpose.equals("exchange"))
            inputTokenStateReference1 = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.toAccountId.equals(bUUID) }

        val tokenStateRef2 = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.fromAccountId.equals(mUUID) }
        println("Line 71 : $tokenStateRef2")

        val borrowerToken= inputTokenStateReference1!!.get(inputTokenStateReference1.size-1).state.data
        println("Line 74")
        //val mToken = mTokenStateRef.get(binputTokenStateReference.size-1).state.data

        println("borrowerToken-Amount=${borrowerToken.amount}")

        inputTokenStateReference1.forEach{tokenState:StateAndRef<TokenState> ->
            val stateAmount = tokenState.state.data.amount

            println("amount=$stateAmount")

            //println("testtesttest="+it)bye

        }

        // val inputTokenState = inputTokenStateReference.state.data
        //println("m acc test =${inputTokenState.amount}")
        /* val id = type+name + newImei
         val storedAccountInfo = accountService.accountInfo(id)
         val identityState = IdentityState(name, imei, storedAccountInfo!!.state.data.accountId, storedAccountInfo.state.data.accountHost)

 */
        // val tokenState = TokenState(sender,receiver,amount,uuid,freshkeyFormAccountInfo.owningKey, accountInfo!!.state.data.accountHost, listOf(accountInfo!!.state.data.accountHost))
        // val updatedInputTokenState = borrowerToken.copy(participants = listOf(mAccountInfo.state.data.accountHost), amount = borrowerToken.amount - purchaseAmount,sender = sender,recipient = receiver,accountId = mUUID,owner = receiver,owningKey = freshkeyFormAccountInfo.owningKey)
        val updatedInputTokenState = borrowerToken.copy(participants = listOf(accountInfo1.state.data.accountHost), amount = borrowerToken.amount - purchaseAmount)

        val updatedOutputTokenState = borrowerToken.copy(participants = listOf(accountInfo2.state.data.accountHost), amount = purchaseAmount,sender = party1,recipient = party2,fromAccountId = bUUID, toAccountId = mUUID,owner = party2,owningKey = freshkeyFormAccountInfo.owningKey)
        val mOutputTokenState = borrowerToken.copy(participants = listOf(accountInfo2.state.data.accountHost), sender = party1, recipient = party2, amount = purchaseAmount, fromAccountId = bUUID, toAccountId = mUUID, owningKey = freshkeyFormAccountInfo.owningKey)
        println("Borrower Updated Output Token State : ${updatedOutputTokenState}")
        println("Merchant Update Output Token State : ${mOutputTokenState}")
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
        val accountSession1 = initiateFlow(accountInfo1.state.data.accountHost)
        val accountSession = initiateFlow(accountInfo2.state.data.accountHost)
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(accountSession,accountSession1)))

        val sessions = if (!serviceHub.myInfo.isLegalIdentity(accountInfo2.state.data.accountHost))
            Collections.singletonList(accountSession)
        else
            Collections.emptyList()
        val sessions1 = if (!serviceHub.myInfo.isLegalIdentity(accountInfo1.state.data.accountHost))
            Collections.singletonList(accountSession1)
        else
            Collections.emptyList()

        return  subFlow(FinalityFlow(fullySignedTransaction, sessions)).also {

            val broadcastToParties =
                    serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() }
                            .minus(serviceHub.networkMapCache.notaryIdentities)
                            .minus(party2)
                            .minus(party1)
            subFlow(
                    BroadcastTransactionFlow(
                            it, broadcastToParties
                    )
            )
        }
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
