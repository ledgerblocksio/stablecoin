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
class ExchangeTokenFlow(private val uuid: UUID, private val tokensToExc: Int, private val lbUUID: UUID): FlowLogic<SignedTransaction>(){
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val mUuidTokenStateinfo = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.toAccountId!!.equals(uuid) }
        //for(i in 1..bUuidTokenStateinfo.size) {
        println("mUuidTokenStateinfo=$mUuidTokenStateinfo")
        val mTokenBal = mUuidTokenStateinfo.get(mUuidTokenStateinfo.size - 1).state.data.txAmount

        println("mTokenBal=$mTokenBal")
        //}
        // val bUuidLoanStateinfo=serviceHub.vaultService.queryBy<LoanState>().states.filter {it.state.data.uuid.equals(bUUID)  }
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val mAccountInfo = accountService.accountInfo(uuid)
        val mParty = mAccountInfo!!.state.data.accountHost
        val freshkeyToAccount = subFlow(RequestKeyForAccountFlow(mAccountInfo!!.state.data))
        //  val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount
        val exchange: String
        val updatedAmount: Int
        //var resultMoveTokenInfo:SignedTransaction ? = null
        if (mTokenBal >= tokensToExc) {
            exchange = "success"
            updatedAmount = mTokenBal - tokensToExc

            // resultMoveTokenInfo=subFlow(MoveTokensBetweenAccounts(uuid,lbUUID,tokensToExc,"exchange"))
        } else {
            exchange = "fail"
            updatedAmount = mTokenBal
            // @todo why calling subflow here, when the condition fails
            //resultMoveTokenInfo=subFlow(MoveTokensBetweenAccounts(uuid,lbUUID,tokensToExc))
            //resultMoveTokenInfo=bUuidTokenStateinfo.get(0)
        }
        //  val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        //  val lbAccountInfo=accountService.accountInfo(lbUUID)
        // val purchaseState=PurchaseState(uuid,lbUUID,exchange, mAccountInfo!!.state.data.accountHost)
        val tokenState = TokenState(updatedAmount, tokensToExc, uuid, lbUUID, "Exchange", LocalDate.now().toString(), freshkeyToAccount.owningKey, mAccountInfo!!.state.data.accountHost, listOf(mAccountInfo!!.state.data.accountHost))

        // val tokenState = TokenState(mParty,mParty,updatedAmount,uuid,uuid,exchange,freshkeyToAccount.owningKey, mAccountInfo!!.state.data.accountHost, listOf(mAccountInfo!!.state.data.accountHost))

        val transactionBuilder = TransactionBuilder(notary)
                //.addInputState(resultMoveTokenInfo!!.coreTransaction.outRefsOfType<TokenState>().single())
                .addOutputState(tokenState)
                .addCommand(TokenContract.Commands.Exchange(), serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
        val accountSession = initiateFlow(mAccountInfo!!.state.data.accountHost)
        val sessions = if (!serviceHub.myInfo.isLegalIdentity(mAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession)
        else
            Collections.emptyList()
        //return subFlow(FinalityFlow(signedTransaction, sessions)).coreTransaction.outRefsOfType<TokenState>().single().state.data.purpose
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
    }
}
@InitiatedBy(ExchangeTokenFlow::class)
class ExchangeTokenFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){
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
