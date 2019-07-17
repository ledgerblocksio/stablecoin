package com.ledgerblocks.poc.flow
import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.LoanContract
import com.ledgerblocks.poc.state.LoanState
import com.ledgerblocks.poc.state.PurchaseState
import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*
@InitiatingFlow
@StartableByRPC
class GoodsPurchaseFlow(private val bUuid: UUID, private val mUuid: UUID, private val purchaseAmt: Int, private val goodsDesc: String): FlowLogic<String>(){
    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val bUuidTokenStateinfo=serviceHub.vaultService.queryBy<TokenState>().states.filter {it.state.data.accountId.equals(bUuid)}
        val borrowerTokenBal= bUuidTokenStateinfo.get(0).state.data.amount
        val bUuidLoanStateinfo=serviceHub.vaultService.queryBy<LoanState>().states.filter {it.state.data.uuid.equals(bUuid)  }
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val mAccountInfo=accountService.accountInfo(mUuid)
        val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount
        val purchase:String
        val resultMoveTokenInfo:StateAndRef<TokenState>
        if(borrowerTokenBal>=purchaseAmt) {
            purchase = "Yes"
            resultMoveTokenInfo=subFlow(MoveTokensBetweenAccounts(bUuid,mUuid,purchaseAmt))
        }
        else {
            purchase = "No"
            resultMoveTokenInfo=bUuidTokenStateinfo.get(0)
        }
        //  val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val bAccountInfo=accountService.accountInfo(bUuid)
        val purchaseState=PurchaseState(bUuid,mUuid,purchase, bAccountInfo!!.state.data.accountHost)
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(resultMoveTokenInfo)
                .addOutputState(purchaseState)
                .addCommand(LoanContract.Commands.Purchase(),serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
        val accountSession = initiateFlow(mAccountInfo!!.state.data.accountHost)
        val sessions = if (!serviceHub.myInfo.isLegalIdentity(mAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession)
        else
            Collections.emptyList()
        return subFlow(FinalityFlow(signedTransaction, sessions)).coreTransaction.outRefsOfType<PurchaseState>().single().state.data.purchase
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
