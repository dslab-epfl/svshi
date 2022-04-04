import datetime
from distutils.log import Log
from inspect import formatannotationrelativeto
import os
import shutil


"""
Log messages in 2 files: telegrams_received.log and execution.log
When a log file exceeds the max file size, it removes the 1000 top lines of the file
"""


class Logger:
    RECEIVED_TELEGRAM_FILE_NAME = "telegrams_received.log"
    EXECUTION_FILE_NAME = "execution.log"
    MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024  # = 20MB
    N_LINES_TO_OMIT_WHEN_TRUNCATING = 1000

    def __init__(self, logs_dir_path: str, buffer_size: int):
        """
        Constructs a new instance of a Logger, creating the folder(s) for the logs if does not exist
        """
        self.logs_dir_path = logs_dir_path
        self.buffer_size = buffer_size

        if not os.path.exists(logs_dir_path):
            os.makedirs(logs_dir_path)

        self.log_received_telegram_file_path = (
            f"{self.logs_dir_path}/{self.RECEIVED_TELEGRAM_FILE_NAME}"
        )
        self.log_execution_file_path = (
            f"{self.logs_dir_path}/{self.EXECUTION_FILE_NAME}"
        )

        self.received_telegram_buffer = []
        self.execution_buffer = []

    def log_received_telegram(self, message: str) -> None:
        self.received_telegram_buffer.append(f"{datetime.datetime.now()} - {message}\n")
        self.__check_and_flush_buffers(
            self.received_telegram_buffer,
            self.log_received_telegram_file_path,
            self.buffer_size,
        )

    def log_execution(self, message: str) -> None:
        self.execution_buffer.append(f"{datetime.datetime.now()} - {message}\n")
        self.__check_and_flush_buffers(
            self.execution_buffer, self.log_execution_file_path, self.buffer_size
        )

    def flush(self):
        self.__check_and_flush_buffers(
            self.received_telegram_buffer,
            self.log_received_telegram_file_path,
            1,
        )
        self.__check_and_flush_buffers(
            self.execution_buffer, self.log_execution_file_path, 1
        )

    def __check_and_flush_buffers(
        self, buffer: list, file_path: str, max_buffer_size: int
    ) -> None:
        if len(buffer) > max_buffer_size:
            if self._get_file_size(file_path) > self.MAX_FILE_SIZE_BYTES:
                self.copy_file_omitting_n_lines(
                    file_path, self.N_LINES_TO_OMIT_WHEN_TRUNCATING
                )
            with open(file_path, "a+") as f:
                f.writelines(buffer)
                buffer.clear()

    def _get_file_size(self, path: str) -> int:
        """
        return the size of the file in bytes, -1 if the file does not exist (or another error occured)
        """
        try:
            return os.path.getsize(path)

        except:
            return -1

    def copy_file_omitting_n_lines(self, file_path: str, n: int) -> None:
        """
        copy the content of the file in a new one, with the same name, removing the n first lines during the process
        """
        new_file_temp_path = f"{file_path}_new"
        with open(file_path, "r+") as src, open(new_file_temp_path, "w+") as dst:
            # Move the file pointer forward by n lines before copying
            for _ in range(n):
                src.readline()
            shutil.copyfileobj(src, dst)
            os.remove(file_path)
            os.rename(new_file_temp_path, file_path)
