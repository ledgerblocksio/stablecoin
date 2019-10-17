package com.ledgerblocks.poc.flow
import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.LoanContract
import com.ledgerblocks.poc.contract.TokenContract
import com.ledgerblocks.poc.state.LoanState
import com.ledgerblocks.poc.state.PurchaseState
import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.contracts.StateAndRef

import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.LocalDate
import java.util.*
@InitiatingFlow
@StartableByRPC
class GoodsPurchaseFlow(private val bUUID: UUID, private val mUUID: UUID, private val purchaseAmount: Int, private val goodsDesc: String): FlowLogic<SignedTransaction>(){
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val bUuidTokenStateinfo=serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(bUUID)}
        //for(i in 1..bUuidTokenStateinfo.size) {
        val borrowerTokenBal = bUuidTokenStateinfo.get(bUuidTokenStateinfo.size-1).state.data.txAmount
        //}
        val bUuidLoanStateinfo=serviceHub.vaultService.queryBy<LoanState>().states.filter {it.state.data.uuid.equals(bUUID)  }
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val mAccountInfo=accountService.accountInfo(mUUID)
        val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount
        val mTokenStateInfo = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.toAccountId!!.equals(mUUID)}
        var mTokenBalance =0
        mTokenStateInfo.forEach(){
            mTokenBalance+= it.state.data.txAmount
        }
        // if(mTokenStateInfo.size>=1)
        //     mTokenBalance = mTokenStateInfo.get(mTokenStateInfo.size-1).state.data.tokenBalance
        val purpose="purchase"
        val purchase:String
        var date = LocalDate.now().toString()
        //println("Date : ${date}")
        var resultMoveTokenInfo:SignedTransaction ? = null
        if(borrowerTokenBal>=purchaseAmount){

            purchase = "yes"
            val bAccountInfo=accountService.accountInfo(bUUID)
            val purchaseState=PurchaseState(bUUID,mUUID,purpose, bAccountInfo!!.state.data.accountHost)
            // resultMoveTokenInfo=subFlow(MoveTokensBetweenAccounts(fUUID,mUUID,purchaseAmount, "purchase"))
            var inputTokenStateReference1: List<StateAndRef<TokenState>>? = null
            val accountInfo1=accountService.accountInfo(bUUID)
            val accountInfo2=accountService.accountInfo(mUUID)
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

            val tokenStateRef2 = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(mUUID) }

            val farmerToken= bUuidTokenStateinfo!!.get(bUuidTokenStateinfo.size-1).state.data
            val party2 = accountInfo2!!.state.data.accountHost

            val updatedInputTokenState = farmerToken.copy(participants = listOf(accountInfo1.state.data.accountHost), tokenBalance = farmerToken.tokenBalance - purchaseAmount, txAmount = purchaseAmount, toAccountId = mUUID, purpose = purpose, date=date)
            println("updatedInputTokenState: ${updatedInputTokenState}")
            val updatedOutputTokenState = farmerToken.copy(participants = listOf(accountInfo2.state.data.accountHost), tokenBalance = mTokenBalance+purchaseAmount,fromAccountId = bUUID, toAccountId = mUUID,owner = party2,owningKey = freshkeyFormAccountInfo.owningKey, txAmount = purchaseAmount, purpose =purpose, date = date)
            val mOutputTokenState = farmerToken.copy(participants = listOf(accountInfo2.state.data.accountHost), tokenBalance = mTokenBalance+purchaseAmount, txAmount = purchaseAmount, fromAccountId = bUUID, toAccountId = mUUID, owningKey = freshkeyFormAccountInfo.owningKey, purpose = purpose)
            //@todo tokenBalane this didnot work as expected ie tokenbalance did not sumup
            println("Farmer Updated Output Token State : ${updatedOutputTokenState}")
            println("Merchant Update Output Token State : ${mOutputTokenState}")
            val (command, updatedOwnerState) = updatedOutputTokenState.withNewOwner(freshkeyFormAccountInfo)
            val transactionBuilder = TransactionBuilder(notary)
                    .addOutputState(updatedOwnerState, TokenContract.ID)
                    .addOutputState(purchaseState)
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

            //val fullySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(accountSession,accountSession1)))

            val sessions = if (!serviceHub.myInfo.isLegalIdentity(accountInfo2!!.state.data.accountHost))
                Collections.singletonList(accountSession)
            else
                Collections.emptyList()
            val sessions1 = if (!serviceHub.myInfo.isLegalIdentity(accountInfo1!!.state.data.accountHost))
                Collections.singletonList(accountSession1)
            else
                Collections.emptyList()

            return  subFlow(FinalityFlow(fullySignedTransaction, sessions))


        }
        // else {
        purchase = "no"
        //resultMoveTokenInfo=subFlow(MoveTokensBetweenAccounts(bUUID,mUUID,purchaseAmount))
        // @todo why calling subflow here, when the condition fails
        //resultMoveTokenInfo=bUuidTokenStateinfo.get(0)
        // }
        //  val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val bAccountInfo=accountService.accountInfo(bUUID)
        val purchaseState=PurchaseState(bUUID,mUUID,purpose, bAccountInfo!!.state.data.accountHost)
        val mParty = mAccountInfo!!.state.data.accountHost
        val bParty = bAccountInfo!!.state.data.accountHost
        val transactionBuilder = TransactionBuilder(notary)
                //  .addInputState(resultMoveTokenInfo.coreTransaction.outRefsOfType<TokenState>().single())
                .addInputState(resultMoveTokenInfo!!.coreTransaction.outRefsOfType<TokenState>().filter { it.state.data.fromAccountId!!.equals(bUUID) }.get(0))
                .addOutputState(purchaseState)
                .addCommand(LoanContract.Commands.Purchase(),serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
        val accountSession = initiateFlow(mAccountInfo!!.state.data.accountHost)
        val sessions = if (!serviceHub.myInfo.isLegalIdentity(mAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession)
        else
            Collections.emptyList()
        return subFlow(FinalityFlow(signedTransaction, sessions)).also {

            val broadcastToParties =
                    serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() }
                            .minus(serviceHub.networkMapCache.notaryIdentities)
            //.minus(mParty)
            //  .minus(bParty)
            subFlow(
                    BroadcastTransactionFlow(
                            it, broadcastToParties
                    )
            )
        }



        // coreTransaction.outRefsOfType<PurchaseState>().single().state.data.purchase
    }
}
@InitiatedBy(GoodsPurchaseFlow::class)
class GoodsPurchaseFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){
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
