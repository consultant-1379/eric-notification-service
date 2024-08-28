Feature: Subscription deletion

  Background:
    Given create new subscription

  @delete-subscription-ok-test
  Scenario: delete request OK
    Given creation response code should be 201
    And send deletion request ok

    Then delete response code should be 204

  @delete-subscription-not-found-test
  Scenario: delete request NOT_FOUND
    Given send deletion request not found

    Then delete response code should be 404

  @delete-subscription-uuid-validation-test
  Scenario: delete request UUID validation
    Given send deletion request wrong uuid

    Then delete response code should be 400
    And check delete response payload