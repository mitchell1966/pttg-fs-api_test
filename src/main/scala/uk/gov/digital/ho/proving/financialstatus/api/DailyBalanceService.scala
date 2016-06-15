package uk.gov.digital.ho.proving.financialstatus.api

import java.math.{BigDecimal => JBigDecimal}
import java.time.LocalDate

import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation._
import uk.gov.digital.ho.proving.financialstatus.acl.BarclaysBankService
import uk.gov.digital.ho.proving.financialstatus.domain.{Account, AccountStatusChecker}

@RestController
class DailyBalanceService @Autowired()(barclaysBankService: BarclaysBankService) {

  // TODO get spring to handle LocalDate objects

  @RequestMapping(value = Array("/pttg/financialstatusservice/v1/accounts/{sortCode}/{accountNumber}/dailybalancestatus"),
    method = Array(RequestMethod.GET),
    produces = Array(MediaType.APPLICATION_JSON_VALUE))
  def dailyBalanceCheck(@PathVariable(value = "sortCode") sortCode: String,
                        @PathVariable(value = "accountNumber") accountNumber: String,
                        @RequestParam(value = "minimum") minimum: JBigDecimal,
                        @RequestParam(value = "toDate") @DateTimeFormat(pattern="yyyy-M-d") toDate: LocalDate,
                        @RequestParam(value = "fromDate") @DateTimeFormat(pattern="yyyy-M-d") fromDate: LocalDate) = {

    val LOGGER: Logger = LoggerFactory.getLogger(classOf[DailyBalanceService])
    LOGGER.info("dailybalancecheck request received")

    val bankAccount = Account(sortCode.replace("-", ""), accountNumber)
    val accountStatusChecker = new AccountStatusChecker(barclaysBankService)
    val dailyAccountBalanceCheck = accountStatusChecker.checkDailyBalancesAreAboveMinimum(
      bankAccount, fromDate, toDate, BigDecimal(minimum).setScale(2, BigDecimal.RoundingMode.HALF_UP)
    )

    new ResponseEntity(AccountDailyBalanceStatusResponse(bankAccount, dailyAccountBalanceCheck, StatusResponse("200", "OK")), HttpStatus.OK)

  }
}
