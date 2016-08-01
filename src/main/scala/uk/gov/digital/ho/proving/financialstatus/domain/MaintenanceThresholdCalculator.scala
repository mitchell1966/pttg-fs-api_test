package uk.gov.digital.ho.proving.financialstatus.domain

import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import uk.gov.digital.ho.proving.financialstatus.api.CappedValues

@Service
class MaintenanceThresholdCalculator @Autowired()(@Value("${inner.london.accommodation.value}") val innerLondon: Int,
                                                  @Value("${non.inner.london.accommodation.value}") val nonInnerLondon: Int,
                                                  @Value("${maximum.accommodation.value}") val maxAccommodation: Int,
                                                  @Value("${inner.london.dependant.value}") val innerLondonDependants: Int,
                                                  @Value("${non.inner.london.dependant.value}") val nonInnerLondonDependants: Int,
                                                  @Value("${non.doctorate.minimum.course.length}") val nonDoctorateMinCourseLength: Int,
                                                  @Value("${non.doctorate.maximum.course.length}") val nonDoctorateMaxCourseLength: Int,
                                                  @Value("${doctorate.minimum.course.length}") val doctorateMinCourseLength: Int,
                                                  @Value("${doctorate.maximum.course.length}") val doctorateMaxCourseLength: Int
                                                 ) {

  val INNER_LONDON_ACCOMMODATION = BigDecimal(innerLondon).setScale(2, BigDecimal.RoundingMode.HALF_UP)
  val NON_INNER_LONDON_ACCOMMODATION = BigDecimal(nonInnerLondon).setScale(2, BigDecimal.RoundingMode.HALF_UP)
  val MAXIMUM_ACCOMMODATION = BigDecimal(maxAccommodation).setScale(2, BigDecimal.RoundingMode.HALF_UP)

  val INNER_LONDON_DEPENDANTS = BigDecimal(innerLondonDependants).setScale(2, BigDecimal.RoundingMode.HALF_UP)
  val NON_INNER_LONDON_DEPENDANTS = BigDecimal(nonInnerLondonDependants).setScale(2, BigDecimal.RoundingMode.HALF_UP)

  def accommodationValue(innerLondon: Boolean): BigDecimal = if (innerLondon) INNER_LONDON_ACCOMMODATION else NON_INNER_LONDON_ACCOMMODATION

  def dependantsValue(innerLondon: Boolean): BigDecimal = if (innerLondon) INNER_LONDON_DEPENDANTS else NON_INNER_LONDON_DEPENDANTS


  def calculateNonDoctorate(innerLondon: Boolean, courseLengthInMonths: Int,
                            tuitionFees: BigDecimal, tuitionFeesPaid: BigDecimal,
                            accommodationFeesPaid: BigDecimal,
                            dependants: Int
                           ): (BigDecimal, Option[CappedValues]) = {

    val (courseLength, courseLengthCapped) = if (courseLengthInMonths > nonDoctorateMaxCourseLength) (nonDoctorateMaxCourseLength, Some(nonDoctorateMaxCourseLength)) else (courseLengthInMonths, None)
    val (accommodationFees, accommodationFeesCapped) = if (accommodationFeesPaid > MAXIMUM_ACCOMMODATION) (MAXIMUM_ACCOMMODATION, Some(MAXIMUM_ACCOMMODATION)) else (accommodationFeesPaid, None)

    val amount = ((accommodationValue(innerLondon) * courseLength)
      + (tuitionFees - tuitionFeesPaid).max(0)
      + (dependantsValue(innerLondon) * courseLength * dependants)
      - accommodationFees).max(0)

    if (courseLengthCapped.isDefined || accommodationFeesCapped.isDefined)
      (amount, Some(CappedValues(accommodationFeesCapped, courseLengthCapped)))
    else (amount, None)
  }

  def calculateDoctorate(innerLondon: Boolean, courseLengthInMonths: Int, accommodationFeesPaid: BigDecimal,
                         dependants: Int): (BigDecimal, Option[CappedValues]) = {

    val (courseLength, courseLengthCapped) = if (courseLengthInMonths > doctorateMaxCourseLength) (doctorateMaxCourseLength, Some(doctorateMaxCourseLength)) else (courseLengthInMonths, None)
    val (accommodationFees, accommodationFeesCapped) = if (accommodationFeesPaid > MAXIMUM_ACCOMMODATION) (MAXIMUM_ACCOMMODATION, Some(MAXIMUM_ACCOMMODATION)) else (accommodationFeesPaid, None)

    val amount = ((accommodationValue(innerLondon) * courseLength)
      + (dependantsValue(innerLondon) * courseLength * dependants)
      - accommodationFees).max(0)

    if (courseLengthCapped.isDefined || accommodationFeesCapped.isDefined)
      (amount, Some(CappedValues(accommodationFeesCapped, courseLengthCapped)))
    else (amount, None)

  }

  def parameters: String = {
    s"""
       | ---------- External parameters values ----------
       |     inner.london.accommodation.value = $innerLondon
       | non.inner.london.accommodation.value = $nonInnerLondon
       |          maximum.accommodation.value = $maxAccommodation
     """.stripMargin
  }
}
