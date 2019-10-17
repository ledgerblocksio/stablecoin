//package com.ledgerblocks.poc.flow
//
//import co.paralleluniverse.fibers.Suspendable
//import com.ledgerblocks.poc.contract.LoanContract
//import com.ledgerblocks.poc.contract.TokenContract
//import com.ledgerblocks.poc.state.IdentityState
//import com.ledgerblocks.poc.state.TokenState
//import net.corda.accounts.flows.RequestKeyForAccountFlow
//import net.corda.accounts.service.KeyManagementBackedAccountService
//import net.corda.core.contracts.StateAndRef
//import net.corda.core.flows.*
//import net.corda.core.node.services.queryBy
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import java.time.LocalDate
//import java.util.*
//
//
//@StartableByRPC
//@InitiatingFlow
//class AddTokensToMerchantFlow(private val bUUID: UUID,private val mUUID: UUID,private val amtToPay: Int): FlowLogic<SignedTransaction>() {
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
//        val bAccountInfo=accountService.accountInfo(bUUID)
//        val mAccountInfo=accountService.accountInfo(mUUID)
//        val purpose:String
//
//        purpose = "addtokens"
//        var date = LocalDate.now().toString()
//        println("Date : ${date}")
//
//        val freshkeyForbAccountInfo= subFlow(RequestKeyForAccountFlow(bAccountInfo!!.state.data))
//        val owningKeyForbAccount =accountService.accountInfo(freshkeyForbAccountInfo.owningKey)
//        val freshkeyFormAccountInfo= subFlow(RequestKeyForAccountFlow(mAccountInfo!!.state.data))
//        val owningKeyFormAccount =accountService.accountInfo(freshkeyFormAccountInfo.owningKey)
//        //val binputTokenStateReference = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(bUUID)}
//        val binputTokenStateReference = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.toAccountId!!.equals(bUUID)}
//       // val minputTokenStateReference = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.toAccountId!!.equals(mUUID)}
//
//       // val minputTokenStateReference = serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(mUUID)}
//
//       // println("minputTokenStateReference=${minputTokenStateReference}")
//
//        println("binputTokenStateReference=${binputTokenStateReference}")
//        val party2 = mAccountInfo!!.state.data.accountHost
//        val bToken= binputTokenStateReference.get(binputTokenStateReference.size-1).state.data
//       // val mToken= minputTokenStateReference.get(minputTokenStateReference.size-1).state.data
//        //println("borrowerToken-Amount=${mToken.txAmount}")
//        //inputTokenStateReference.forEach{tokenState:StateAndRef<TokenState> ->
//            //val stateAmount = tokenState.state.data.txAmount
//          //  println("amount=$stateAmount")
//      //  }
//       // val updatedInputTokenState = mToken.copy(participants = listOf(mAccountInfo.state.data.accountHost), txAmount = mToken.txAmount + amtToPay)
//        //val updatedOutputTokenState = mToken.copy(participants = listOf(mAccountInfo.state.data.accountHost), txAmount = amtToPay)
//       // val (command, updatedOwnerState) = updatedOutputTokenState.withNewOwner(freshkeyFormAccountInfo)
//        val signingAccounts = listOfNotNull(owningKeyForbAccount, owningKeyFormAccount)
//        val requiredSigners =
//                signingAccounts.map { it.state.data.accountHost.owningKey } + listOfNotNull(
//                        freshkeyForbAccountInfo.owningKey, freshkeyFormAccountInfo.owningKey,ourIdentity.owningKey)
//        val updatedInputTokenState = bToken.copy(participants = listOf(bAccountInfo.state.data.accountHost),tokenBalance = bToken.tokenBalance - amtToPay,txAmount = amtToPay, toAccountId = bUUID, purpose = purpose, date=date)
//        val updatedOutputTokenState = mToken.copy(participants = listOf(mAccountInfo.state.data.accountHost), tokenBalance = mToken.tokenBalance+amtToPay,fromAccountId = bUUID, toAccountId = mUUID,owner = party2,owningKey = freshkeyFormAccountInfo.owningKey, txAmount = amtToPay, purpose =purpose, date = date)
//        val mOutputTokenState = bToken.copy(participants = listOf(mAccountInfo.state.data.accountHost), tokenBalance = mToken.tokenBalance+amtToPay, txAmount = amtToPay, fromAccountId = bUUID, toAccountId = mUUID, owningKey = freshkeyFormAccountInfo.owningKey, purpose = purpose)
//
//        val (command, updatedOwnerState) = updatedInputTokenState.withNewOwner(freshkeyFormAccountInfo)
//
//        val transactionBuilder = TransactionBuilder(notary)
//                .addOutputState(updatedOwnerState, TokenContract.ID)
//                .addOutputState(updatedInputTokenState, TokenContract.ID)
//                .addCommand(command, requiredSigners)
//                .addReferenceState(owningKeyForbAccount!!.referenced())
//                .addReferenceState(owningKeyFormAccount!!.referenced())
//              //  .addReferenceState(owningKeyFormAccount!!.referenced())
//               // .addCommand(LoanContract.Commands.Loan(),serviceHub.myInfo.legalIdentities.first().owningKey)
//        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder,listOfNotNull(
//                ourIdentity.owningKey
//        ))
//        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
//        transactionBuilder.verify(serviceHub)
//        val accountSession = initiateFlow(mAccountInfo!!.state.data.accountHost)
//        val sessions = if (!serviceHub.myInfo.isLegalIdentity(mAccountInfo.state.data.accountHost))
//            Collections.singletonList(accountSession)
//        else
//            Collections.emptyList()
//
//        return subFlow(FinalityFlow(signedTransaction, sessions))//.coreTransaction.outRefsOfType(TokenState::class.java).filter {it.state.data.toAccountId.equals(mUUID)}.get(0)
//    }
//}
//
//@InitiatedBy(AddTokensToMerchantFlow::class)
//class AddTokensToMerchantFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){
//
//    @Suspendable
//    final override fun call(): SignedTransaction {
//
//        val stx = subFlow(object : SignTransactionFlow(otherPartySession) {
//
//            override fun checkTransaction(stx: SignedTransaction) {
//                //extraTransactionValidation(stx)
//            }
//        })
//        val tx = subFlow(
//                ReceiveFinalityFlow(
//                        otherSideSession = otherPartySession,
//                        expectedTxId = stx.id
//                )
//        )
//        logger.info("Received transaction from finality")
//        return tx
//    }
//}
