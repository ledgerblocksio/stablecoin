package com.ledgerblocks.poc.flow
import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.LoanContract
import com.ledgerblocks.poc.state.LoanState
import com.ledgerblocks.poc.state.PurchaseState
import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.service.KeyManagementBackedAccountService

import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*
@InitiatingFlow
@StartableByRPC
class GoodsPurchaseFlow(private val bUUID: UUID, private val mUUID: UUID, private val purchaseAmount: Int, private val goodsDesc: String): FlowLogic<SignedTransaction>(){
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val bUuidTokenStateinfo=serviceHub.vaultService.queryBy<TokenState>().states.filter {it.state.data.fromAccountId.equals(bUUID)}
        //for(i in 1..bUuidTokenStateinfo.size) {
        val borrowerTokenBal = bUuidTokenStateinfo.get(bUuidTokenStateinfo.size-1).state.data.amount
        //}
        val bUuidLoanStateinfo=serviceHub.vaultService.queryBy<LoanState>().states.filter {it.state.data.uuid.equals(bUUID)  }
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val mAccountInfo=accountService.accountInfo(mUUID)
        val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount
        val purchase:String
        var resultMoveTokenInfo:SignedTransaction ? = null
        if(borrowerTokenBal>=purchaseAmount) {
            purchase = "yes"
            resultMoveTokenInfo=subFlow(MoveTokensBetweenAccounts(bUUID,mUUID,purchaseAmount, "purchase"))
        }
        else {
            purchase = "no"
            //resultMoveTokenInfo=subFlow(MoveTokensBetweenAccounts(bUUID,mUUID,purchaseAmount))
            // @todo why calling subflow here, when the condition fails
            //resultMoveTokenInfo=bUuidTokenStateinfo.get(0)
        }
        //  val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val bAccountInfo=accountService.accountInfo(bUUID)
        val purchaseState=PurchaseState(bUUID,mUUID,purchase, bAccountInfo!!.state.data.accountHost)
        val mParty = mAccountInfo!!.state.data.accountHost
        val bParty = bAccountInfo!!.state.data.accountHost
        val transactionBuilder = TransactionBuilder(notary)
                //  .addInputState(resultMoveTokenInfo.coreTransaction.outRefsOfType<TokenState>().single())
                .addInputState(resultMoveTokenInfo!!.coreTransaction.outRefsOfType<TokenState>().filter { it.state.data.fromAccountId.equals(bUUID) }.get(0))
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
                            .minus(mParty)
                            .minus(bParty)
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
