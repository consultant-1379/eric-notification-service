Feature: Subscription creation

  @create-subscription-test
  Scenario: request OK
    Given valid creation request was sent

    Then response code should be 201
    And response body contains subscription params from a request plus UUID

  @create-invalid-address-subscription-test
  Scenario: request BAD REQUEST (address)
    Given invalid address in creation request was sent

    Then response code should be 400
    And  response body contains an address error details

  @create-no-event-type-subscription-test
  Scenario: request BAD REQUEST (eventType)
    Given no eventType in creation request was sent

    Then response code should be 400
    And  response body contains an eventType error details

  @create-invalid-filter-criteria-subscription-test
  Scenario: request BAD REQUEST (filterCriteria)
    Given invalid filterCriteria in creation request was sent

    Then response code should be 400
    And  response body contains a filterCriteria error details

  @create-invalid-fields-subscription-test
  Scenario: request BAD REQUEST (fields)
    Given invalid fields in creation request was sent

    Then response code should be 400
    And  response body contains a fields error details

  @create-duplicate-subscription-test
  Scenario: request CONFLICT
    Given specific creation request was sent
    And specific creation request was sent

    Then response code should be 409
