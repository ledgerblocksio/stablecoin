package com.ledgerblocks.poc.server

import com.google.firebase.messaging.FirebaseMessaging
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

import com.google.firebase.messaging.Message
import com.ledgerblocks.poc.state.*
import net.corda.core.contracts.filterStatesOfType
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.io.*

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
     * Displays all amounts states that exist in the node's vault.
     */
    @GetMapping(value = ["identityStates"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getidentityStates(): ResponseEntity<List<StateAndRef<IdentityState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<IdentityState>().states)
    }


    /**
     * Displays all amounts states that exist in the node's vault.
     */
    @GetMapping(value = ["loanStates"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getloanStates(): ResponseEntity<List<StateAndRef<LoanState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<LoanState>().states)
    }

    /**
     * Displays all amounts states that exist in the node's vault.
     */
    @GetMapping(value = ["tokenStates"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTokenStates(): ResponseEntity<List<StateAndRef<TokenState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<TokenState>().states)
    }

    /**
     * createIdentity
     */
    @PostMapping(value = ["identity"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createIdentity(request: HttpServletRequest): ResponseEntity<Any?> {
        val name = request.getParameter("name")
        val mobileToken = request.getParameter("fcmToken")
        val imei = request.getParameter("imei").toString()
        val type = request.getParameter("type")
        //val currentDirectory = System.getProperty("user.dir")
        //val lbPropDirectory = currentDirectory+"lbuuid.properties"
        //val fis = FileInputStream("/home/user/Desktop/samples/linearlb/clients/src/main/kotlin/com/ledgerblocks/poc/server/lbuuid.properties")
        //val fis = FileInputStream(lbPropDirectory)
        //val properties = Properties()
        //properties.load(fis)
        //val lbUUID =  properties.getProperty("lbuuid")
        //val lbUUID1 = UUID.fromString(lbUUID)
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
    @PostMapping(value = ["updatedAccountInfo"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updatedAccountInfo(request: HttpServletRequest): ResponseEntity<Any?> {
        val uuid = request.getParameter("uuid")
        val uuid1 = UUID.fromString(uuid)
        val mobileToken = request.getParameter("mobileToken")


        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'name' must not be null.\n")
        }
        if (mobileToken == null) {
            return ResponseEntity.badRequest().body("Query parameter 'mobileToken' must not be null.\n")
        }

        return try {
            val sign = proxy.startTrackedFlow(::UpdateAccountInfoFlow, uuid1 , mobileToken).returnValue.get()
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
            val finalLoanDecision=loan.coreTransaction.outRefsOfType<LoanState>().single().state.data.loanDecision

            ResponseEntity.status(HttpStatus.CREATED).body("decision:${finalLoanDecision}")

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
            val purchaseValue =purchase.coreTransaction.outRefsOfType<PurchaseState>().single().state.data.purchase
            println("purchaseValue=$purchaseValue")
            val bUuidTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(bUuid1)}
            bUuidTokenStateinfo.forEach{tokenState:StateAndRef<TokenState> ->
                val stateAmount = tokenState.state.data.txAmount
                println("amount=$stateAmount")
            }
            val borrowerTokenBal= bUuidTokenStateinfo.get(bUuidTokenStateinfo.size-1).state.data.tokenBalance
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
                //@todo the above 2 lines are resulting in exception; they are execpted to retrieve FCM Token of the merchant from identity state


                println("FCM Borrower: {$borrowrName}")
                println("FCM Token: {$mFcmToken}")
                println("Merchant FCM Token: {${mIdentityStateinfo.get(0).state.data.fcmToken}}")
                println("Merchant FCM Token: {${mIdentityStateinfo.get(0).state.data.fcmToken}}")
                /*
                var message = Message.builder()
                        .putData("borrower", bIdentityStateinfo.get(0).state.data.name)
                        .putData("tokens", purchaseAmt.toString())
                        .setToken(mIdentityStateinfo.get(0).state.data.mobileToken)
                        .build()
                var response = FirebaseMessaging.getInstance().send(message)
                println("FCM response: {$response}")
                 */
            } catch (e: FirebaseMessagingException){
                logger.error(e.message, e)
            }
        }

    }



    /**
     * Dashboard
     */

   /* @GetMapping(value = ["dashboard"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun dashBoardApi(request: HttpServletRequest): ResponseEntity<Any> {
        val uuid = request.getParameter("uuid")
        val Uuid1 = UUID.fromString(uuid)
        val type = request.getParameter("type")
        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        if (type == null) {
            return ResponseEntity.badRequest().body("Query parameter 'type' must not be null.\n")
        }
        val mIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(Uuid1)}
        val bIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(Uuid1)}
        val bTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(Uuid1)}
        val bLoanStateinfo=proxy.vaultQueryBy<LoanState>().states.filter {it.state.data.uuid.equals(Uuid1)}
        val result=if(type.equals('m')) "mName:${mIdentityStateinfo.get(0).state.data.name}+tokenBalance:${75000}" else  "name:${bIdentityStateinfo.get(bIdentityStateinfo.size-1).state.data.name}+loanAmount:${bLoanStateinfo.get(0).state.data.loanAmount}+avaBlance:${bTokenStateinfo.get(bTokenStateinfo.size-1).state.data.txAmount}"
        return ResponseEntity.ok(result)
    }
*/


    /**
     * Dashboard
     */

    @GetMapping(value = ["dashboard"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun dashBoardApi(request: HttpServletRequest): ResponseEntity<Any> {
        val uuid = request.getParameter("uuid")
        val Uuid1 = UUID.fromString(uuid)
        val type = request.getParameter("type")
        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        if (type == null) {
            return ResponseEntity.badRequest().body("Query parameter 'type' must not be null.\n")
        }
        val response = JSONObject()
        val array = JSONArray()
        var arrayElement = JSONObject()
        if(type.equals("b")){
            println("Type: borrower")
            var subsidy = "no"
            var avaBalance = "no"

            val fTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(Uuid1)}

            if(fTokenStateinfo.isNotEmpty()){
                subsidy = (fTokenStateinfo.get(0).state.data.txAmount).toString()
                response.put("subsidyAmt", subsidy)
                val avalBalance = fTokenStateinfo.get(fTokenStateinfo.size-1).state.data.tokenBalance
                response.put("avaBalance", avalBalance)

            }


            fTokenStateinfo.forEach(){
                var mUUID = it.state.data.toAccountId

                var mState = proxy.vaultQueryBy<IdentityState>().states.findLast { it.state.data.uuid.equals(mUUID) }
                println("mState=$mState")
                var mName = mState!!.state.data.name
                var purpose: String
                var amount = it.state.data.txAmount*-1
                if(mUUID!!.equals(Uuid1)) {
                    println("Subsidy")
                    mName = "Loan"
                    amount = amount*-1
                }

                arrayElement = createElement(it.state.data.date, mName,amount)

                array.add(arrayElement)
                println("Array : $array")

            }
            response.put("transactions", array)
        }
        if(type.equals("m")){
            println("Type: merchnat")
            var tokenBalance = "no"
            var mTokenStateInfo = proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.toAccountId!!.equals(Uuid1) || it.state.data.fromAccountId!!.equals(Uuid1)}
            println("mTokenStateInfo= $mTokenStateInfo")
            println("No of states: + ${mTokenStateInfo.size}")
            if(mTokenStateInfo.isNotEmpty()) {
                tokenBalance = (mTokenStateInfo!!.get(mTokenStateInfo.size-1).state.data.tokenBalance).toString()
                println("Merchant Token Balance: ${tokenBalance}")
                response.put("tokenBalance", tokenBalance)
            }
            // these will account for tokens transferred to merchnats from farmers
            mTokenStateInfo.forEach(){
                if(it.state.data.fromAccountId!!.equals(Uuid1)){
                    println("Exchange branch: $mTokenStateInfo")
                    arrayElement = createElement(it.state.data.date, "Exchange", (it.state.data.txAmount)*-1)
                    array.add(arrayElement)
                } else {
                    var fUUID = it.state.data.fromAccountId
                    var fState = proxy.vaultQueryBy<IdentityState>().states.findLast { it.state.data.uuid.equals(fUUID) }
                    var fName = fState!!.state.data.name

                    arrayElement = createElement(it.state.data.date, fName, it.state.data.txAmount)
                    array.add(arrayElement)
                }

            }
            // this will account for tokens exchanged
            /*
            mTokenStateInfo = proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(Uuid1)}
            if(mTokenStateInfo.isNotEmpty()){
                mTokenStateInfo.forEach(){
                    println("Exchange branch: $mTokenStateInfo")
                    arrayElement = createElement(it.state.data.date, "Exchange", (it.state.data.txAmount)*-1)
                    array.add(arrayElement)
                }

            }
             */
            response.put("transactions", array)
        }
        return ResponseEntity.ok(response)
    }

    fun createElement(date: String, name: String, amount: Int): JSONObject {
        var element = JSONObject()
        element.put("date", date)
        element.put("name", name)
        element.put("amount", amount)
        println("Inside function : " + element)
        return element
    }



    /**
     * FCM Token update and check if there is a new build
     */
    @PostMapping(value= ["fcmUpdate"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fcmUpdate(request: HttpServletRequest): ResponseEntity<String>{
        val uuid = request.getParameter("uuid")
        val fcmToken = request.getParameter("fcmToken")
        var updated: String = "no"
        return try{
            val identityStateinfo = proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(uuid)}
            if(identityStateinfo.isNotEmpty()){
                // @todo Hanuman - update identity state with the new fcmToken
                updated = "yes"
            }
            ResponseEntity.status(HttpStatus.CREATED).body("updated:{$updated}")
        }catch (ex: Throwable){
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
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
            val paymentValue=payment.coreTransaction.outRefsOfType<PayLoanState>().single().state.data.payment
            val buuidLoanStateinfo=proxy.vaultQueryBy<LoanState>().states.filter { it.state.data.uuid.equals(uuid1)}
            println("loanstates=$buuidLoanStateinfo")
            val bBalLoanAmount= buuidLoanStateinfo.get(buuidLoanStateinfo.size-1).state.data.loanAmount
            ResponseEntity.status(HttpStatus.CREATED).body("payment:${paymentValue}+balLoanAmt:${bBalLoanAmount}")
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
        val emailFile = System.getProperty("user.dir")+"/src/main/resources/email.csv"
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
            fileReader = BufferedReader(FileReader(System.getProperty("user.dir")+"/src/main/resources/email.csv"))
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
            val paymentValue=payment.coreTransaction.outRefsOfType<PayLoanState>().single().state.data.payment
            val mIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(mUUID1)}

            val mTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(mUUID1)}
val merchantTokenBalance=mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.tokenBalance
            println("balance:${mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.txAmount}")

            ResponseEntity.status(HttpStatus.CREATED).body("payment:${paymentValue}+balance:${merchantTokenBalance}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
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
            // val mIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(mUUID1)}

            //val mTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter {it.state.data.accountId.equals(mUUID1)}

            // println("balance:${mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.amount}")

            ResponseEntity.status(HttpStatus.CREATED).body("deducted")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }

    }



//    /**
//     * AddTokensToMerchantFlow
//     */
//    @PostMapping(value = ["addTokens"], produces = [MediaType.APPLICATION_JSON_VALUE])
//    fun addTokens(request: HttpServletRequest): ResponseEntity<String> {
//        val bUUID = request.getParameter("bUUID")
//        val bUUID1 = UUID.fromString(bUUID)
//        val mUUID = request.getParameter("mUUID")
//        val mUUID1 = UUID.fromString(mUUID)
//        // val mUUID = request.getParameter("mUUID")
//        // val mUUID1 = UUID.fromString(mUUID)
//        val amtToPay = request.getParameter("amtToPay").toInt()
//
//        // val lbUUID = request.getParameter("lbUUID")
//        //   val lbUUID1 = UUID.fromString(lbUUID)
//        //  val currentDirectory = System.getProperty("user.dir")
//        //   val fis = FileInputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
//        //  val properties = Properties()
//        //  properties.load(fis)
//        //  val lbUUID =  properties.getProperty("lbuuid")
//        //  val lbUUID1 = UUID.fromString(lbUUID)
//        if (mUUID == null) {
//            return ResponseEntity.badRequest().body("Query parameter 'mUUID' must not be null.\n")
//        }
//        /*if (mUUID1 == null) {
//            return ResponseEntity.badRequest().body("Query parameter 'mUUID1' must not be null.\n")
//        }*/
//        /*if (lbUUID == null) {
//            return ResponseEntity.badRequest().body("Query parameter 'lbUUID' must not be null.\n")
//        }*/
//        if (amtToPay <= 0) {
//            return ResponseEntity.badRequest().body("Query parameter ' amtToPay' must be non-negative..\n")
//        }
//
//        return try {
//            val payment = proxy.startTrackedFlow(::AddTokensToMerchantFlow, bUUID1, mUUID1, amtToPay).returnValue.get()
//            // val mIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(mUUID1)}
//
//            //val mTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter {it.state.data.accountId.equals(mUUID1)}
//
//            // println("balance:${mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.amount}")
//
//            ResponseEntity.status(HttpStatus.CREATED).body("deducted")
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            ResponseEntity.badRequest().body(ex.message!!)
//        }
//
//    }




    /**
     * exchangeToken
     */
    @PostMapping(value = ["exchange"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun exchangeToken(request: HttpServletRequest): ResponseEntity<String> {
        val uuid = request.getParameter("uuid")
        val uuid1 = UUID.fromString(uuid)
        val lbUUID = request.getParameter("lbUUID")
        val lbUUID1 = UUID.fromString(lbUUID)
        val tokensToExc = request.getParameter("tokensToExc").toInt()

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
            val mUuidTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.fromAccountId!!.equals(uuid1)}
            mUuidTokenStateinfo.forEach{tokenState:StateAndRef<TokenState> ->
                val stateAmount = tokenState.state.data.txAmount
                println("amount=$stateAmount")
            }
            val mTokenBal= mUuidTokenStateinfo.get(mUuidTokenStateinfo.size-1).state.data.tokenBalance
            // val bUuidLoanStateinfo=proxy.vaultQueryBy<LoanState>().states.filter { it.state.data.uuid.equals(bUuid1)}
            // val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount
            ResponseEntity.status(HttpStatus.CREATED).body("exchange:success++avaTokenBalance:${mTokenBal}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

}
