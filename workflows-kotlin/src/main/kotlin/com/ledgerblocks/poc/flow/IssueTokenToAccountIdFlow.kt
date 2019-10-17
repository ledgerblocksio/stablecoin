package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.TokenContract

import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

import net.corda.core.transactions.TransactionBuilder
import java.io.FileInputStream
import java.time.LocalDate
import java.util.*

@InitiatingFlow
@StartableByRPC
class IssueTokenToAccountIdFlow(private val uuid: UUID, private val amount: Int): FlowLogic<Int>(){

    @Suspendable
    override fun call(): Int {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val accountInfo=accountService.accountInfo(uuid)
        val bParty = accountInfo!!.state.data.accountHost
        println("Borrower Party: ${bParty}")

        /*
        val currentDirectory = System.getProperty("user.dir")
        val fis = FileInputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
        val properties = Properties()
        properties.load(fis)
        val lbUUID =  properties.getProperty("lbuuid")
        val lbUUID1 = UUID.fromString(lbUUID)
        val lbAccountInfo = accountService.accountInfo(lbUUID1)
        val lbParty = lbAccountInfo!!.state.data.accountHost
        println("LedgerBlocks Party: ${lbParty}")


         */
        val freshkeyToAccount = subFlow(RequestKeyForAccountFlow(accountInfo!!.state.data))
        var date = LocalDate.now().toString()
        println("Date : ${date}")

       // val tokenState = TokenState(bParty,bParty,amount,uuid,uuid,"Issue",freshkeyToAccount.owningKey, accountInfo!!.state.data.accountHost, listOf(accountInfo!!.state.data.accountHost))

        val tokenState = TokenState(amount,amount,uuid,uuid,"Issue", date,freshkeyToAccount.owningKey, accountInfo!!.state.data.accountHost, listOf(accountInfo!!.state.data.accountHost))
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(tokenState, TokenContract.ID)
                .addCommand(TokenContract.Commands.Issue(),serviceHub.myInfo.legalIdentities.first().owningKey)

        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        transactionBuilder.verify(serviceHub)
        return subFlow(FinalityFlow(signedTransaction, emptyList())).coreTransaction.outRefsOfType<TokenState>().single().state.data.txAmount

             //   also {

           // val broadcastToParties =
              //      serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() }
             //               .minus(serviceHub.networkMapCache.notaryIdentities)
            //.minus(mParty)
            //  .minus(bParty)
           // subFlow(
              //      BroadcastTransactionFlow(
             //               it, broadcastToParties
            //        )
         //   )
        //}
                //.coreTransaction.outRefsOfType<TokenState>().single().state.data.amount
    }

}
