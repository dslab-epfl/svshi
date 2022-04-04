import os

random_line_text = (
    "Lorem Ipsum is simply dummy text of the printing and typesetting industry."
)


def get_file_size(path: str) -> int:
    """
    return the size of the file in bytes, -1 if the file does not exist (or another error occured)
    """
    try:
        return os.path.getsize(path)

    except:
        return -1


size = 20 * 1024 * 1024
file_path = "f.txt"
with open(file_path, "w+") as f:
    i = 1
    while get_file_size(file_path) < size:
        f.write(f"{i} {random_line_text}\n")
        i += 1
