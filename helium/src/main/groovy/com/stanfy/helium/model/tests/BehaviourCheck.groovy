package com.stanfy.helium.model.tests

import com.stanfy.helium.model.Descriptionable
import org.joda.time.Duration

/**
 * Specification check result.
 */
class BehaviourCheck extends Descriptionable {

  Result result = Result.PENDING

  Duration time = Duration.ZERO

  /** Specification check result. */
  enum Result {
    PASSED, FAILED, PENDING
  }

}
