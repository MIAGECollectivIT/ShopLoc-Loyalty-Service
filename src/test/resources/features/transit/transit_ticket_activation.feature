@transit
Feature: VFP Free Public Transit Ticket Activation (SLE-47 / SLE-233)

  Scenario: Activate and deliver a 60-minute free transit ticket for an active VFP user
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    And the external transit gateway confirms ticket "TRN-BDD-001" with code "BUS-112233"
    When the user requests a free transit ticket
    Then the HTTP response status should be 201
    And the response JSON field "status" should equal "CONFIRMED"
    And the response JSON field "ticketReference" should equal "TRN-BDD-001"
    And the response JSON field "ticketCode" should equal "BUS-112233"
    And the response JSON field "startedAt" should not be empty
    And the response JSON field "expiresAt" should not be empty
    And a confirmed transit ticket is persisted in database for today

  Scenario: Deny transit ticket activation when caller is not authenticated
    Given an unauthenticated user
    When the user requests a free transit ticket
    Then the HTTP response status should be 401

  Scenario: Deny transit ticket activation when user does not have active VFP status
    Given an authenticated user with role "CLIENT"
    And the user does not have an active VFP status
    When the user requests a free transit ticket
    Then the HTTP response status should be 403

  Scenario: Reject second transit ticket activation on the same day with 409 Conflict
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    And the external transit gateway confirms ticket "TRN-BDD-FIRST" with code "BUS-001122"
    When the user requests a free transit ticket
    Then the HTTP response status should be 201
    When the user requests a free transit ticket
    Then the HTTP response status should be 409

  Scenario: Reject transit ticket activation when parking benefit was already consumed today (SLE-48)
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    And Users Service returns registered vehicle "AA-123-BB" for the user
    And the external parking gateway confirms ticket "PARK-TRANSIT-001" for plate "AA-123-BB"
    When the user requests a free parking session for plate "AA-123-BB"
    Then the HTTP response status should be 201
    When the user requests a free transit ticket
    Then the HTTP response status should be 409
