package com.ledgerblocks.poc.server

import com.google.firebase.messaging.FirebaseMessagingException
import com.ledgerblocks.poc.flow.*


import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.FileInputStream
import java.io.FileOutputStream

import java.util.*

import javax.servlet.http.HttpServletRequest

import com.ledgerblocks.poc.state.*
import java.io.*
import java.time.Instant
import java.time.format.DateTimeFormatter

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */

@RestController
@RequestMapping("/api/lb/") // The paths for GET and POST requests are relative to this base path.
class MainController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy


    /**
     * Returns the node's name.
     */
    @GetMapping(value = ["me"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Displays all identity states that exist in the node's vault.
     */
    @GetMapping(value = ["identityStates"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getidentityStates(): ResponseEntity<Any> {
        var response = ""
        proxy.vaultQueryBy<IdentityState>().states.forEach {
            response = response + "Name: " + it.state.data.name + " FCM TOKEN: " + it.state.data.fcmToken + " UUID: " + it.state.data.uuid + "\n"
        }
        return ResponseEntity.ok("${response}")
    }

    /**
     * Displays all loan states that exist in the node's vault.
     */
    @GetMapping(value = ["loanStates"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getloanStates(): ResponseEntity<Any> {
        var response = ""
        proxy.vaultQueryBy<LoanState>().states.forEach {
            response = response + "UUID: " + it.state.data.uuid + " Loan Amount: " + it.state.data.loanAmount + "\n"
        }
        return ResponseEntity.ok("${response}")
    }

    /**
     * Displays all token states that exist in the node's vault.
     */
    @GetMapping(value = ["tokenStates"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun gettokenStates(): ResponseEntity<Any> {
        var response = ""
        proxy.vaultQueryBy<TokenState>().states.forEach {
            response = response + "From Acc: " + it.state.data.fromAccountId + " To Acc: "+ it.state.data.toAccountId + " Token balance : " + it.state.data.amount + "\n"
        }
        return ResponseEntity.ok("${response}")
    }

    /**
     * Displays all PayLoan states that exist in the node's vault.
     */
    @GetMapping(value = ["payLoanStates"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getpayLoanStates(): ResponseEntity<Any> {
        var response = ""
        proxy.vaultQueryBy<PayLoanState>().states.forEach {
            response = response + "From Acc: " + it.state.data.bUuid + " Payment : " + it.state.data.payment + "\n"
        }
        return ResponseEntity.ok("${response}")
    }

    /**
     * Displays all purchase states that exist in the node's vault.
     */
    @GetMapping(value = ["purchaseStates"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getpurchaseStates(): ResponseEntity<Any> {
        var response = ""
        proxy.vaultQueryBy<PurchaseState>().states.forEach {
            response = response + "From Acc: " + it.state.data.bUuid + " To Acc: "+ it.state.data.mUuid + " Purchase Amount : " + it.state.data.purchase + "\n"
        }
        return ResponseEntity.ok("${response}")
    }

    /**
     * createIdentity
     */
    @PostMapping(value = ["identity"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createIdentity(request: HttpServletRequest): ResponseEntity<Any?> {
        val name = request.getParameter("name")
        val mobileToken = request.getParameter("fcmToken")
        val imei = request.getParameter("imei")
        val type = request.getParameter("type")

        if (name == null) {
            return ResponseEntity.badRequest().body("Query parameter 'name' must not be null.\n")
        }
        if (imei == null) {
            return ResponseEntity.badRequest().body("Query parameter 'imei' must not be null.\n")
        }
        if (type == null) {
            return ResponseEntity.badRequest().body("Query parameter 'type' must not be null.\n")
        }
        return try {
            val sign = proxy.startTrackedFlow(::IdentityStateFlow, name , mobileToken, imei, type).returnValue.get()
            val accountId=  sign.coreTransaction.outRefsOfType<IdentityState>().single().state.data.uuid

            val bUuidIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states
            println("test=$bUuidIdentityStateinfo")
            ResponseEntity.status(HttpStatus.CREATED).body("uuid:${accountId}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    /**
     * UpdatedAccountInfo
     */
    @PostMapping(value = ["updateAccount"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updatedAccountInfo(request: HttpServletRequest): ResponseEntity<Any?> {
        val uuid = request.getParameter("uuid")
        val uuid1 = UUID.fromString(uuid)
        val mobileToken = request.getParameter("fcmToken")


        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'name' must not be null.\n")
        }
        if (mobileToken == null) {
            return ResponseEntity.badRequest().body("Query parameter 'fcmToken' must not be null.\n")
        }

        return try {
            val sign = proxy.startTrackedFlow(::UpdateAccountInfoFlow, uuid1 , mobileToken).returnValue.get()
            //val accountId=  sign.coreTransaction.outRefsOfType<IdentityState>().single().state.data.uuid

            val identityStateinfo=proxy.vaultQueryBy<IdentityState>().states
            println("test=$identityStateinfo")
            ResponseEntity.status(HttpStatus.CREATED).body("updated:yes")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    /**
     * createLoan
     */

    @PostMapping(value = ["loan"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createLoan(request: HttpServletRequest): ResponseEntity<String> {
        val uuid = request.getParameter("uuid")
        val uuid1 = UUID.fromString(uuid)
        //  val lbUUID = request.getParameter("lbUUID")
        // val lbUUID1 = UUID.fromString(lbUUID)
        val loanAmount = request.getParameter("loanAmount").toInt()
        val loanPeriod = request.getParameter("loanPeriod").toInt()
        val loanPurpose = request.getParameter("loanPurpose")
        val interestRate = request.getParameter("interestRate").toInt()
        val emi = request.getParameter("emi").toInt()


        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        /*if (lbUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'lbUUID' must not be null.\n")
        }*/
        if (loanAmount <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' loanAmount' must be non-negative..\n")
        }
        if (loanPeriod <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' loanPeriod' must be non-negative..\n")
        }
        if (loanPurpose == null) {
            return ResponseEntity.badRequest().body("Query parameter 'loanPurpose' must not be null.\n")
        }
        if (interestRate <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' interestRate' must be non-negative..\n")
        }
        if (emi <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' emi' must be non-negative..\n")
        }
        return try {
            val loan = proxy.startTrackedFlow(::LoanStateFlow, uuid1, loanAmount, loanPeriod, interestRate, emi, loanPurpose).returnValue.get()


            ResponseEntity.status(HttpStatus.CREATED).body("decision:${loan}")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    /**
     * createPurchase
     */
    @PostMapping(value = ["purchase"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createPurchase(request: HttpServletRequest): ResponseEntity<String> {
        val bUuid = request.getParameter("bUUID")
        val bUuid1 = UUID.fromString(bUuid)
        val mUuid = request.getParameter("mUUID")
        val mUuid1 = UUID.fromString(mUuid)
        val purchaseAmt = request.getParameter("purchaseAmount").toInt()
        val goodsDesc = request.getParameter("goodsDesc")

        if (bUuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'bUUID' must not be null.\n")
        }
        if (mUuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'mUUID' must not be null.\n")
        }
        if (purchaseAmt <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' purchaseAmount' must be non-negative..\n")
        }
        if (goodsDesc == null) {
            return ResponseEntity.badRequest().body("Query parameter 'goodsDesc' must not be null.\n")
        }
        return try {
            val purchase = proxy.startTrackedFlow(::GoodsPurchaseFlow, bUuid1, mUuid1, purchaseAmt, goodsDesc).returnValue.get()
            val bUuidTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.fromAccountId.equals(bUuid1)}
            bUuidTokenStateinfo.forEach{tokenState:StateAndRef<TokenState> ->
                val stateAmount = tokenState.state.data.amount
                println("amount=$stateAmount")
            }
            val borrowerTokenBal= bUuidTokenStateinfo.get(bUuidTokenStateinfo.size-1).state.data.amount
            val bUuidLoanStateinfo=proxy.vaultQueryBy<LoanState>().states.filter { it.state.data.uuid.equals(bUuid1)}
            val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount

            ResponseEntity.status(HttpStatus.CREATED).body("purchase:yes+loanAmount:${bInitialLoanAmount}+avaBlance:${borrowerTokenBal}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
        // This finally block sends a push notification; implement the following to enable this functionality
        // 0. Send this only on successful purchase
        // 1. get mobileToken from identity state of respective merchant
        // 2. get name of the borrower
        // 3. Purchase amount


        finally {
            try {
                val bIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(bUuid1)}
                val borrowrName = bIdentityStateinfo.get(0).state.data.name
                val mIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(mUuid1)}
                println("Merchan Identity State : ${mIdentityStateinfo}")
                println("M FCM Token : ${mIdentityStateinfo.get(mIdentityStateinfo.size-1).state.data.fcmToken}")
                val mFcmToken = mIdentityStateinfo.get(0).state.data.fcmToken

                var message = "$purchaseAmt tokens credited from $borrowrName"
                println("FCM Borrower: {$borrowrName}")
                println("FCM Token: {$mFcmToken}")


                var notification = SendNotification.Message(mFcmToken,message)
                println("Push Notificaton: $notification")

            } catch (e: FirebaseMessagingException){
                logger.error(e.message, e)
            }
        }

    }



    /**
     * Dashboard
     */

    @GetMapping(value = ["dashboard"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun dashBoardApi(request: HttpServletRequest): ResponseEntity<Any> {
        val uuid = request.getParameter("uuid")
        val Uuid1 = UUID.fromString(uuid)
        val type = request.getParameter("type")
        var tokenBalance = 0
        var loanAmount = 0
        var avaBalance = 0
        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        var result:String? = null
        if (type == null) {
            return ResponseEntity.badRequest().body("Query parameter 'type' must not be null.\n")
        }

        if(type.equals("m")){
            val mIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(Uuid1)}
            val mTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter {it.state.data.toAccountId.equals(Uuid1)}
            if (mTokenStateinfo.isNotEmpty()){
                tokenBalance = mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.amount
            }
            result = "name:${mIdentityStateinfo.get(0).state.data.name}+tokenBalance:${tokenBalance}"
        }
        if(type.equals("b")) {
            val bIdentityStateinfo = proxy.vaultQueryBy<IdentityState>().states.filter { it.state.data.uuid.equals(Uuid1) }
            val bTokenStateinfo = proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.fromAccountId.equals(Uuid1) }
            if (bTokenStateinfo.isNotEmpty()){
                tokenBalance = bTokenStateinfo.get(bTokenStateinfo.size-1).state.data.amount
            }
            val bLoanStateinfo = proxy.vaultQueryBy<LoanState>().states.filter { it.state.data.uuid.equals(Uuid1) }
            if (bLoanStateinfo.isNotEmpty()){
                loanAmount = bLoanStateinfo.get(bLoanStateinfo.size-1).state.data.loanAmount
            }
            result="name:${bIdentityStateinfo.get(bIdentityStateinfo.size-1).state.data.name}+loanAmount:${loanAmount}+avaBlance:${tokenBalance}"
        }
        //result=if(type.equals('m')) "mName:${mIdentityStateinfo.get(0).state.data.name}+tokenBalance:${75000}" else  "name:${bIdentityStateinfo.get(bIdentityStateinfo.size-1).state.data.name}+loanAmount:${bLoanStateinfo.get(0).state.data.loanAmount}+avaBlance:${bTokenStateinfo.get(bTokenStateinfo.size-1).state.data.amount}"
        return ResponseEntity.ok(result)
    }

    /**
     * lbUuid
     */

    @PostMapping(value = ["lbUUID"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createLbUuid(request: HttpServletRequest): ResponseEntity<Any?> {
        val name = request.getParameter("name")
        // val mobileToken = request.getParameter("mobileToken")
        val imei = request.getParameter("imei")
        val type = request.getParameter("type")
        if (name == null) {
            return ResponseEntity.badRequest().body("Query parameter 'name' must not be null.\n")
        }
        if (imei == null) {
            return ResponseEntity.badRequest().body("Query parameter 'imei' must not be null.\n")
        }
        if (type == null) {
            return ResponseEntity.badRequest().body("Query parameter 'type' must not be null.\n")
        }
        return try {
            val accountId = proxy.startTrackedFlow(::LeadgerBlocksAccountFlow, name , imei, type).returnValue.get()
            val currentDirectory = System.getProperty("user.dir")
            val fis = FileOutputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
            val prop = Properties()
            prop.setProperty("lbuuid", "${accountId}")
            prop.store(fis,"save")
            ResponseEntity.status(HttpStatus.CREATED).body("uuid:${accountId}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    /**
     * pay-loan-direct
     */
    @PostMapping(value = ["payLoanDirect"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun payLoanDirect(request: HttpServletRequest): ResponseEntity<String> {
        val uuid = request.getParameter("uuid")
        val uuid1 = UUID.fromString(uuid)
        val amtToPay = request.getParameter("amtToPay").toInt()
        val cardNumber = request.getParameter("cardNumber").toInt()
        // val lbUUID = request.getParameter("lbUUID")
        //   val lbUUID1 = UUID.fromString(lbUUID)
        val currentDirectory = System.getProperty("user.dir")
        val fis = FileInputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
        val properties = Properties()
        properties.load(fis)
        val lbUUID =  properties.getProperty("lbuuid")
        val lbUUID1 = UUID.fromString(lbUUID)
        fis.close()
        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        if (lbUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'lbUUID' must not be null.\n")
        }
        if (amtToPay <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' amtToPay' must be non-negative..\n")
        }
        if (cardNumber == null) {
            return ResponseEntity.badRequest().body("Query parameter 'cardNumber' must not be null.\n")
        }
        return try {
            val payment = proxy.startTrackedFlow(::PayLoanDirectFlow, uuid1,lbUUID1, amtToPay, cardNumber).returnValue.get()
            val buuidLoanStateinfo=proxy.vaultQueryBy<LoanState>().states.filter { it.state.data.uuid.equals(uuid1)}
            println("loanstates=$buuidLoanStateinfo")
            val bBalLoanAmount= buuidLoanStateinfo.get(buuidLoanStateinfo.size-1).state.data.loanAmount
            ResponseEntity.status(HttpStatus.CREATED).body("payment:${payment}+balLoanAmt:${bBalLoanAmount}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }

    }


    @PostMapping(value = ["addEmail"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun addEmail (request: HttpServletRequest): ResponseEntity<String> {
        val email = request.getParameter("email")
        if (email == null){
            return ResponseEntity.badRequest().body("Email must not be empty")
        }
        //val emailFile = System.getProperty("user.dir")+"/src/main/resources/email.csv"
        val emailFile = System.getProperty("user.home")+"/access/email.csv"
        //val emailFile = "~/access/email.csv"
        return try {
            File(emailFile).appendText(email)
            File(emailFile).appendText(System.getProperty("line.separator"))
            ResponseEntity.status(HttpStatus.CREATED).body("added:yes")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    @PostMapping(value = ["verifyEmail"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun verifyEmail (request: HttpServletRequest): ResponseEntity<String> {
        val email = request.getParameter("email")
        var allowed = "no"
        var fileReader: BufferedReader? = null
        if (email == null){
            return ResponseEntity.badRequest().body("Email must not be empty")
        }
        return try {
            //fileReader = BufferedReader(FileReader(System.getProperty("user.dir")+"/src/main/resources/email.csv"))
            fileReader = BufferedReader(FileReader(System.getProperty("user.home")+"/access/email.csv"))
            var emailLine = fileReader.readLine()
            while (emailLine != null){
                if(emailLine.equals(email)){
                    allowed="yes"
                    break
                }
                emailLine =fileReader.readLine()
            }
            ResponseEntity.status(HttpStatus.CREATED).body("verified:${allowed}")
        } catch (ex: Throwable){
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }finally {
            try {
                fileReader!!.close()
            } catch (e: IOException) {
                println("Closing fileReader Error!")
                e.printStackTrace()
            }
        }
    }

    /**
     * pay-loan-merchant
     */
    @PostMapping(value = ["payLoanMerchant"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun payLoanMerchant(request: HttpServletRequest): ResponseEntity<String> {
        val bUUID = request.getParameter("bUUID")
        val bUUID1 = UUID.fromString(bUUID)
        val mUUID = request.getParameter("mUUID")
        val mUUID1 = UUID.fromString(mUUID)
        val amtToPay = request.getParameter("amtToPay").toInt()

        // val lbUUID = request.getParameter("lbUUID")
        //   val lbUUID1 = UUID.fromString(lbUUID)
        val currentDirectory = System.getProperty("user.dir")
        val fis = FileInputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
        val properties = Properties()
        properties.load(fis)
        val lbUUID =  properties.getProperty("lbuuid")
        val lbUUID1 = UUID.fromString(lbUUID)
        fis.close()
        if (bUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'bUUID' must not be null.\n")
        }
        if (mUUID1 == null) {
            return ResponseEntity.badRequest().body("Query parameter 'mUUID1' must not be null.\n")
        }
        if (lbUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'lbUUID' must not be null.\n")
        }
        if (amtToPay <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' amtToPay' must be non-negative..\n")
        }

        return try {
            val payment = proxy.startTrackedFlow(::PayLoanDirectThroughMerchantFlow, bUUID1,mUUID1,lbUUID1, amtToPay).returnValue.get()
            val mIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(mUUID1)}

            val mTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter {it.state.data.toAccountId.equals(mUUID1)}

           // println("balance:${mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.amount}")

            ResponseEntity.status(HttpStatus.CREATED).body("payment:${payment}+balance:${mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.amount}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        } finally {
            try {
                val bIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(bUUID1)}
                val bFcmToken = bIdentityStateinfo.get(0).state.data.fcmToken

                var message = "$amtToPay paid towards your loan"
                println("FCM Token: {$bFcmToken}")


                var notification = SendNotification.Message(bFcmToken,message)
                println("Push Notificaton: $notification")

            } catch (e: FirebaseMessagingException){
                logger.error(e.message, e)
            }
        }

    }



    /**
     * deductLoan
     */
    @PostMapping(value = ["deductLoan"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deductLoan(request: HttpServletRequest): ResponseEntity<String> {
        val bUUID = request.getParameter("bUUID")
        val bUUID1 = UUID.fromString(bUUID)
        // val mUUID = request.getParameter("mUUID")
        // val mUUID1 = UUID.fromString(mUUID)
        val amtToPay = request.getParameter("amtToPay").toInt()

        // val lbUUID = request.getParameter("lbUUID")
        //   val lbUUID1 = UUID.fromString(lbUUID)
        //  val currentDirectory = System.getProperty("user.dir")
        //   val fis = FileInputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
        //  val properties = Properties()
        //  properties.load(fis)
        //  val lbUUID =  properties.getProperty("lbuuid")
        //  val lbUUID1 = UUID.fromString(lbUUID)
        if (bUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'bUUID' must not be null.\n")
        }
        /*if (mUUID1 == null) {
            return ResponseEntity.badRequest().body("Query parameter 'mUUID1' must not be null.\n")
        }*/
        /*if (lbUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'lbUUID' must not be null.\n")
        }*/
        if (amtToPay <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' amtToPay' must be non-negative..\n")
        }

        return try {
            val payment = proxy.startTrackedFlow(::DeductLoanAmountFlow, bUUID1, amtToPay).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("deducted")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }

    }



    /**
     * AddTokensToMerchantFlow
     */
    @PostMapping(value = ["addTokens"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun addTokens(request: HttpServletRequest): ResponseEntity<String> {
        val mUUID = request.getParameter("mUUID")
        val mUUID1 = UUID.fromString(mUUID)
        // val mUUID = request.getParameter("mUUID")
        // val mUUID1 = UUID.fromString(mUUID)
        val amtToPay = request.getParameter("amtToPay").toInt()

        // val lbUUID = request.getParameter("lbUUID")
        //   val lbUUID1 = UUID.fromString(lbUUID)
        //  val currentDirectory = System.getProperty("user.dir")
        //   val fis = FileInputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
        //  val properties = Properties()
        //  properties.load(fis)
        //  val lbUUID =  properties.getProperty("lbuuid")
        //  val lbUUID1 = UUID.fromString(lbUUID)
        if (mUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'mUUID' must not be null.\n")
        }
        /*if (mUUID1 == null) {
            return ResponseEntity.badRequest().body("Query parameter 'mUUID1' must not be null.\n")
        }*/
        /*if (lbUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'lbUUID' must not be null.\n")
        }*/
        if (amtToPay <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' amtToPay' must be non-negative..\n")
        }

        return try {
            val payment = proxy.startTrackedFlow(::AddTokensToMerchantFlow, mUUID1, amtToPay).returnValue.get()
            // val mIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(mUUID1)}

            //val mTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter {it.state.data.accountId.equals(mUUID1)}

            // println("balance:${mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.amount}")

            ResponseEntity.status(HttpStatus.CREATED).body("deducted")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }

    }


    /**
     * exchangeToken
     */
    @PostMapping(value = ["exchange"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun exchangeToken(request: HttpServletRequest): ResponseEntity<String> {
        val uuid = request.getParameter("uuid")
        val uuid1 = UUID.fromString(uuid)
        val tokensToExc = request.getParameter("tokensToExc").toInt()


        val currentDirectory = System.getProperty("user.dir")
        val fis = FileInputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
        val properties = Properties()
        properties.load(fis)
        val lbUUID =  properties.getProperty("lbuuid")
        val lbUUID1 = UUID.fromString(lbUUID)
        fis.close()
        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        if (lbUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'lbUUID' must not be null.\n")
        }
        if (tokensToExc <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' tokensToExc' must be non-negative..\n")
        }

        return try {
            val purpose = proxy.startTrackedFlow(::ExchangeTokenFlow, uuid1, tokensToExc, lbUUID1).returnValue.get()
            val mUuidTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.fromAccountId.equals(uuid1)}
            mUuidTokenStateinfo.forEach{tokenState:StateAndRef<TokenState> ->
                val stateAmount = tokenState.state.data.amount
                println("amount=$stateAmount")
            }
            val mTokenBal= mUuidTokenStateinfo.get(mUuidTokenStateinfo.size-1).state.data.amount
            // val bUuidLoanStateinfo=proxy.vaultQueryBy<LoanState>().states.filter { it.state.data.uuid.equals(bUuid1)}
            // val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount
            ResponseEntity.status(HttpStatus.CREATED).body("exchange:${purpose}++avaTokenBalance:${mTokenBal}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

}
