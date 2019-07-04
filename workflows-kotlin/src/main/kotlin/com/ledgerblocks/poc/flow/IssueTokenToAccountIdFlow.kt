package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.TokenContract
import com.ledgerblocks.poc.state.LoanState
import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC
class IssueTokenToAccountIdFlow(private val uuid: UUID,private val amount: Int): FlowLogic<Int>(){

    @Suspendable
    override fun call(): Int {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val accountInfo=accountService.accountInfo(uuid)

        val freshkeyToAccount = subFlow(RequestKeyForAccountFlow(accountInfo!!.state.data))

        val tokenState = TokenState(amount,freshkeyToAccount.owningKey, accountInfo!!.state.data.accountHost, listOf(accountInfo!!.state.data.accountHost))
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(tokenState, TokenContract.ID)
                .addCommand(TokenContract.Commands.Issue(),serviceHub.myInfo.legalIdentities.first().owningKey)

        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        transactionBuilder.verify(serviceHub)
        return subFlow(FinalityFlow(signedTransaction, emptyList())).coreTransaction.outRefsOfType<TokenState>().single().state.data.amount
    }

}