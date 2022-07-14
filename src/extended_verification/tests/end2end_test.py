import pytest

from extended_verification.verification_functions import VerificationFunctions

from ..main import run_extended_module_with_verification_file,UnsatError
from . import expected_verification_file

TESTS_DIRECTORY = "tests"

verif_functions= VerificationFunctions()

def test_all():
    with pytest.raises(UnsatError) as exc_info:
        run_extended_module_with_verification_file(expected_verification_file, verif_functions=verif_functions)

    exception_raised = exc_info.value.__str__()
    assert "ERROR: unsat for invariant first_app_invariant" in exception_raised

