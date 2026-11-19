@parking
Feature: VFP Free Parking Session Acknowledgment (SLE-78 / SLE-261)

  Scenario: Activate and acknowledge a 20-minute free parking session for an active VFP user
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    And Users Service returns registered vehicle "AA-123-BB" for the user
    And the external parking gateway confirms ticket "PARK-BDD-001" for plate "AA-123-BB"
    When the user requests a free parking session for plate "AA-123-BB"
    Then the HTTP response status should be 201
    And the response JSON field "status" should equal "CONFIRMED"
    And the response JSON field "ticketReference" should equal "PARK-BDD-001"
    And the response JSON field "licensePlate" should equal "AA-123-BB"
    And the response JSON field "startedAt" should not be empty
    And the response JSON field "expiresAt" should not be empty
    And a confirmed parking session for plate "AA-123-BB" is persisted in database for today

  Scenario: Recover automatically when external parking gateway fails once with 503
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    And Users Service returns registered vehicle "BB-456-CC" for the user
    And the external parking gateway fails once with 503 before confirming ticket "PARK-RETRY-002" for plate "BB-456-CC"
    When the user requests a free parking session for plate "BB-456-CC"
    Then the HTTP response status should be 201
    And the response JSON field "status" should equal "CONFIRMED"
    And the response JSON field "ticketReference" should equal "PARK-RETRY-002"
    And a confirmed parking session for plate "BB-456-CC" is persisted in database for today

  Scenario: Deny parking session activation when caller is not authenticated
    Given an unauthenticated user
    When the user requests a free parking session for plate "AA-123-BB"
    Then the HTTP response status should be 401

  Scenario: Deny parking session activation when user does not have active VFP status
    Given an authenticated user with role "CLIENT"
    And the user does not have an active VFP status
    When the user requests a free parking session for plate "AA-123-BB"
    Then the HTTP response status should be 403

  Scenario: Reject parking session when license plate is not registered on user profile
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    And Users Service returns no registered vehicles for the user
    When the user requests a free parking session for plate "AA-123-BB"
    Then the HTTP response status should be 400

  Scenario: Reject second parking session activation on the same day with 409 Conflict
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    And Users Service returns registered vehicle "AA-123-BB" for the user
    And the external parking gateway confirms ticket "PARK-FIRST-001" for plate "AA-123-BB"
    When the user requests a free parking session for plate "AA-123-BB"
    Then the HTTP response status should be 201
    When the user requests a free parking session for plate "AA-123-BB"
    Then the HTTP response status should be 409

  Scenario: Return 502 Bad Gateway when external parking gateway is persistently unavailable
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    And Users Service returns registered vehicle "AA-123-BB" for the user
    And the external parking gateway is completely unavailable with 503
    When the user requests a free parking session for plate "AA-123-BB"
    Then the HTTP response status should be 502

  Scenario: Reject malformed license plate with 400 Bad Request
    Given an authenticated user with role "CLIENT"
    When the user requests a free parking session for plate "INVALID-PLATE"
    Then the HTTP response status should be 400

  Scenario: Return daily eligibility as eligible when VFP user has not activated any benefit today
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    When the user checks daily VFP benefit eligibility
    Then the HTTP response status should be 200
    And the response JSON flag "isVfp" should be true
    And the response JSON flag "eligible" should be true
    And the response JSON flag "parkingUsedToday" should be false

  Scenario: Return daily eligibility as ineligible with active session after parking activation
    Given an authenticated user with role "CLIENT"
    And the user has an active VFP status
    And Users Service returns registered vehicle "AA-123-BB" for the user
    And the external parking gateway confirms ticket "PARK-ELIG-001" for plate "AA-123-BB"
    When the user requests a free parking session for plate "AA-123-BB"
    Then the HTTP response status should be 201
    When the user checks daily VFP benefit eligibility
    Then the HTTP response status should be 200
    And the response JSON flag "eligible" should be false
    And the response JSON flag "parkingUsedToday" should be true
    And the response JSON field "consumedBenefitType" should equal "PARKING"
    And the response JSON field "activeParkingSession.ticketReference" should equal "PARK-ELIG-001"

  Scenario: Deny daily eligibility check when caller is not authenticated
    Given an unauthenticated user
    When the user checks daily VFP benefit eligibility
    Then the HTTP response status should be 401
