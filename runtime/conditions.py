import os

__CONDITIONS_FILE_PATH = "runtime/verifier/conditions.py"


def __write_conditions_file(data: str):
    os.makedirs(os.path.dirname(__CONDITIONS_FILE_PATH), exist_ok=True)
    with open(__CONDITIONS_FILE_PATH, "w+") as output_file:
        output_file.write(data)


def generate_conditions_file():
    """
    Generates the conditions file given the conditions of all the apps installed in the library.
    """
    apps_dirs = [
        f.name
        for f in os.scandir("app_library")
        if f.is_dir() and f.name != "__pycache__"
    ]
    imports = ""
    imports_code = []
    for app in apps_dirs:
        import_statement = f"from verification.verification_file import {app}_precond\n"
        if import_statement not in imports:
            imports += import_statement
            imports_code.append(f"{app}_precond")

    check_conditions_body = ""
    nb_imports = len(imports_code)
    for i, import_code in enumerate(imports_code):
        suffix = " and " if i < nb_imports - 1 else ""
        check_conditions_body += f"{import_code}(state){suffix}"

    file = f"""
{imports}
def check_conditions(state: PhysicalState) -> bool:
  return {check_conditions_body}
        """.strip()

    __write_conditions_file(file)


def reset_conditions_file():
    """
    Resets the conditions file.
    """
    file = f"""
from verification.verification_file import PhysicalState

def check_conditions(state: PhysicalState) -> bool:
  return True    
    """.strip()

    __write_conditions_file(file)
