package uk.gov.digital.ho.proving.financialstatus.api

import java.util.Optional

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.digital.ho.proving.financialstatus.audit.AuditActions
import uk.gov.digital.ho.proving.financialstatus.audit.AuditActions.nextId
import uk.gov.digital.ho.proving.financialstatus.audit.AuditEventPublisher
import uk.gov.digital.ho.proving.financialstatus.audit.AuditEventType
import uk.gov.digital.ho.proving.financialstatus.audit.configuration.DeploymentDetails
import uk.gov.digital.ho.proving.financialstatus.authentication.Authentication
import uk.gov.digital.ho.proving.financialstatus.domain.ConditionCodesCalculationResult
import uk.gov.digital.ho.proving.financialstatus.domain.ConditionCodesCalculatorProvider
import uk.gov.digital.ho.proving.financialstatus.domain.StudentTypeChecker
import uk.gov.digital.ho.proving.financialstatus.domain.UserProfile

@RestController
@ControllerAdvice
@RequestMapping(value = Array(ConditionCodesServiceTier4.ConditionCodeTier4Url))
class ConditionCodesServiceTier4  @Autowired()(val auditor: AuditEventPublisher,
                                               val authenticator: Authentication,
                                               val deploymentConfig: DeploymentDetails,
                                               val conditionCodesCalculatorProvider: ConditionCodesCalculatorProvider,
                                               val studentTypeChecker: StudentTypeChecker
                                              ) extends FinancialStatusBaseController {

  private val LOGGER = LoggerFactory.getLogger(classOf[ConditionCodesServiceTier4])

  @RequestMapping(method = Array(RequestMethod.GET))
  def calculateConditionCodes(@RequestParam(value = "studentType") studentType: Optional[String],
                              @RequestParam(value = "applicationType") applicationType: Optional[String],
                              @RequestParam(value = "dependants") dependants: Optional[Int],
                              @CookieValue(value = "kc-access") kcToken: Optional[String]
                             ): ResponseEntity[ConditionCodesResponse] = {

    // TODO: Validation
    val accessToken: Option[String] = kcToken

    val userProfile: Option[UserProfile] = accessToken.flatMap(authenticator.getUserProfileFromToken)

    val validatedStudentType = studentTypeChecker.getStudentType(studentType.getOrElse("Unknown"))
    val result: ConditionCodesCalculationResult = withAudit(userProfile) {
      val calculator = conditionCodesCalculatorProvider.provide(validatedStudentType)
      calculator.calculateConditionCodes()
    }
    val conditionCodesResponse = conditionCodesResultResponseConverter(result)
    new ResponseEntity[ConditionCodesResponse](conditionCodesResponse, HttpStatus.OK)
  }

  def withAudit(userProfile: Option[UserProfile])(calculateConditionCodes: => ConditionCodesCalculationResult): ConditionCodesCalculationResult = {
    // TODO: Unit test this
    auditor.publishEvent(generateRequestAuditEvent(userProfile))
    val result = calculateConditionCodes
    auditor.publishEvent(generateResultAuditEvent(userProfile))
    result
  }

  private def generateRequestAuditEvent(userProfile: Option[UserProfile]) = {
    val auditRequestParamsData = Map[String, AnyRef]() // TODO: Real params
    AuditActions.auditEvent(
      deploymentConfig = deploymentConfig,
      principal = userProfile.map(_.id).getOrElse("anonymous"),
      auditEventType = AuditEventType.CONDITION_CODES_REQUEST,
      id = nextId,
      data = auditRequestParamsData
    )
  }

  private def generateResultAuditEvent(userProfile: Option[UserProfile]) = {
    val auditResponseParamsData = Map[String, AnyRef]() // TODO: Real params
    AuditActions.auditEvent(
      deploymentConfig = deploymentConfig,
      principal = userProfile.map(_.id).getOrElse("anonymous"),
      auditEventType = AuditEventType.CONDITION_CODES_RESULT,
      id = nextId,
      data = auditResponseParamsData
    )
  }

  def conditionCodesResultResponseConverter(result: ConditionCodesCalculationResult): ConditionCodesResponse =
    ConditionCodesResponse(result.applicant.map(_.value), result.partner.map(_.value),result.child.map(_.value))

}
object ConditionCodesServiceTier4 {
  final val ConditionCodeTier4Url = "/pttg/financialstatus/v1/t4/maintenance/conditionCode"
}
