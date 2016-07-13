@wiremock
Feature: Not Pass - Total Funds Required Calculation - Tier 4 (General) student (single current account and no dependants)

  Requirement to meet Tier 4 pass
  Applicant does not have required closing balance every day for a consecutive 28 day period from the date of the Maintenance End Date


  Scenario: Shelly is a general student and does not have sufficient financial funds

  Application Raised Date 1st of June
  She has < than the Total Fund Required of £2350 for the previous 28 days


    Given a Service is consuming Financial Status API
    Given the test data for account 23568498
    When the Financial Status API is invoked with the following:
      | To Date        | 2016-06-01 |
      | From Date      | 2016-05-05 |
      | Minimum        | 2530.00    |
      | Sort code      | 135609     |
      | Account number | 23568498   |
    #  | Applicant Date of Birth     | Value |  *** Need to confirm with Barclay's ***


    Then The Financial Status API provides the following results:
      | HTTP Status    | 200        |
      | Pass           | false      |
      | Minimum        | 2530.00    |
      | From Date      | 2016-05-05 |
      | To Date        | 2016-06-01 |
      | Sort code      | 135609     |
      | Account number | 23568498   |


  Scenario: Brian is general student and does not have sufficient financial funds

  Application Raised Date 4th of July
  He has < than the Total Funds Required of £2030 for the previous 28 days

    Given a Service is consuming Financial Status API
    Given the test data for account 01078911
    When the Financial Status API is invoked with the following:
      | To Date        | 2016-07-01 |
      | From Date      | 2016-06-04 |
      | Minimum        | 2030.00    |
      | Sort code      | 149302     |
      | Account number | 01078911   |

     # | Applicant Date of Birth    |Value | *** Need to confirm with Barclay's ***

    Then The Financial Status API provides the following results:
      | HTTP Status    | 200        |
      | Pass           | false      |
      | Minimum        | 2030.00    |
      | From Date      | 2016-06-04 |
      | To Date        | 2016-07-01 |
      | Sort code      | 149302     |
      | Account number | 01078911   |


  Scenario: David is general student and does not have sufficient financial funds

  Application Raised Date 4th of July
  He has < than the Total Funds Required of £2537.48 for the previous 28 days


    Given a Service is consuming Financial Status API
    Given the test data for account 17926767
    When the Financial Status API is invoked with the following:
      | To Date        | 2016-07-01 |
      | From Date      | 2016-06-04 |
      | Minimum        | 2537.48    |
      | Sort code      | 139302     |
      | Account number | 17926767   |

     # | Applicant Date of Birth    |Value | *** Need to confirm with Barclay's ***

    Then The Financial Status API provides the following results:
      | HTTP Status    | 200        |
      | Pass           | false      |
      | Minimum        | 2537.48    |
      | From Date      | 2016-06-04 |
      | To Date        | 2016-07-01 |
      | Sort code      | 139302     |
      | Account number | 17926767   |