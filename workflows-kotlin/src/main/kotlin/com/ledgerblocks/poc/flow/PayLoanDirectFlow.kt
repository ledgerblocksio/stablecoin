package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable

import com.ledgerblocks.poc.contract.LoanContract
import com.ledgerblocks.poc.state.LoanState

import com.ledgerblocks.poc.state.PayLoanState
import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.flows.RequestKeyForAccountFlow


import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy


import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC//private val name: String
class PayLoanDirectFlow(private val uuid: UUID, private val lbUUID: UUID, private val amtToPay: Int, private val cardNumber: Int): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val payment:String

        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

        val lbAccountInfo=accountService.accountInfo(lbUUID)
        //val resultMoveTokenInfo:StateAndRef<TokenState>
        val inputLoanStateReference = serviceHub.vaultService.queryBy<LoanState>().states.filter { it.state.data.uuid.equals(uuid) }

      //  resultMoveTokenInfo=subFlow(MoveTokensBetweenAccounts(uuid,lbUUID,amtToPay))
      //  val resultMoveTokenInfo:StateAndRef<LoanState>

        var resultMoveTokenInfo:SignedTransaction ? = null

       // resultMoveTokenInfo.coreTransaction.

        //resultMoveTokenInfo=subFlow(MoveLoanAmountBetweenAccounts(uuid,lbUUID,amtToPay)).coreTransaction.outRefsOfType(LoanState::class.java).filter {it.state.data.uuid.equals(uuid)}.get(inputLoanStateReference.size-1)

       // resultMoveTokenInfo.coreTransaction.outRefsOfType(LoanState::class.java).filter {it.state.data.uuid.equals(uuid)}.get(inputLoanStateReference.size-1)
        if(!(amtToPay.equals(0))) {
            payment = "success"
            val bAccountInfo=accountService.accountInfo(uuid)
            val paymentState=PayLoanState(uuid,lbUUID,payment, bAccountInfo!!.state.data.accountHost)


            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
           // val bAccountInfo=accountService.accountInfo(uuid)
            val mAccountInfo=accountService.accountInfo(lbUUID)
            val freshkeyForbAccountInfo= subFlow(RequestKeyForAccountFlow(bAccountInfo!!.state.data))
            val freshkeyFormAccountInfo= subFlow(RequestKeyForAccountFlow(mAccountInfo!!.state.data))

            val owningKeyForbAccount =accountService.accountInfo(freshkeyForbAccountInfo.owningKey)
            val owningKeyFormAccount =accountService.accountInfo(freshkeyFormAccountInfo.owningKey)

            val signingAccounts = listOfNotNull(owningKeyForbAccount, owningKeyFormAccount)

            val requiredSigners =
                    signingAccounts.map { it.state.data.accountHost.owningKey } + listOfNotNull(
                            freshkeyForbAccountInfo.owningKey, freshkeyFormAccountInfo.owningKey,ourIdentity.owningKey)
            val inputLoanStateReference = serviceHub.vaultService.queryBy<LoanState>().states.filter { it.state.data.uuid.equals(uuid) }

            val size=  inputLoanStateReference.size

            println("1inputLoanStateReference=$size")

            val borrowerLoan= inputLoanStateReference.get(inputLoanStateReference.size-1).state.data
            println("borrowerToken-Amount=${borrowerLoan.loanAmount}")
            val updatedAmtToPay = borrowerLoan.loanAmount-amtToPay
            println("updatedloanAmount=$amtToPay")
            val loanState = LoanState(uuid,updatedAmtToPay, borrowerLoan.loanPeriod, borrowerLoan.loanPurpose,borrowerLoan.loanDecision,borrowerLoan.interestRate, borrowerLoan.emi, bAccountInfo!!.state.data.accountHost)

            println("updated loanState=$loanState")
            val transactionBuilder = TransactionBuilder(notary)
                    .addOutputState(loanState)
                    .addOutputState(paymentState)
                    .addCommand(LoanContract.Commands.Loan(),serviceHub.myInfo.legalIdentities.first().owningKey)
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
            val size1=  inputLoanStateReference.size
            println("1inputLoanStateReference=$size1")

            return  subFlow(FinalityFlow(fullySignedTransaction, sessions)).also {

                        val broadcastToParties =
                                serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() }
                                        .minus(serviceHub.networkMapCache.notaryIdentities)
                        subFlow(
                                BroadcastTransactionFlow(
                                        fullySignedTransaction, broadcastToParties
                                )
                        )
                    }
        }



      //  else {
            payment = "failure"
        //}
        val bAccountInfo=accountService.accountInfo(uuid)
        val payLoanState= PayLoanState(uuid,lbUUID,payment, bAccountInfo!!.state.data.accountHost)
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(resultMoveTokenInfo!!.coreTransaction.outRefsOfType<LoanState>().filter { it.state.data.loanAmount!!.equals(uuid) }.get(0))
                .addOutputState(payLoanState)
                .addCommand(LoanContract.Commands.Pay(),serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
        val accountSession = initiateFlow(lbAccountInfo!!.state.data.accountHost)
        val sessions = if (!serviceHub.myInfo.isLegalIdentity(lbAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession)
        else
            Collections.emptyList()
        return subFlow(FinalityFlow(signedTransaction, sessions))//.coreTransaction.outRefsOfType<PayLoanState>().single().state.data.payment

    }

}

@InitiatedBy(PayLoanDirectFlow::class)
class PayLoanDirectFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

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
