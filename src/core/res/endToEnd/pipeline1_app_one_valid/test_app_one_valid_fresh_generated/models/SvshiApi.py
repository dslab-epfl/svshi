class SvshiApi:
    def __init__(self):
        pass

    def get_hour_of_the_day(self) -> int:
        """
        get the machine's hour in 24 hours format (return is between 0 and 23)
        example : 15 for Wed Apr 13 15:57:17 2022 UTC
        :returns the current hour based on the machine's time
        """
        pass

    def get_minute_in_hour(self) -> int:
        """
        get the machine's minutes in one hour (return is between 0 and 59)
        example : 57 for Wed Apr 13 15:57:17 2022 UTC
        :returns the current minute in the hour based on the machine's time
        """
        pass

    def get_day_of_week(self) -> int:
        """
        get the machine's day of the week, the week starts on Monday (return is between 1 and 7)
        example : 3 for Wed Apr 13 15:57:17 2022 UTC
        :returns the day of the week based on the machine's time
        """
        pass

    def get_day_of_month(self) -> int:
        """
        get the machine's day of the month (return is between 1 and 31)
        example : 13 for Wed Apr 13 15:57:17 2022 UTC
        :returns the day of the month based on the machine's time
        """
        pass

    def get_month_in_year(self) -> int:
        """
        get the machine's month of the year (return is between 1 and 12)
        example : 4 for Wed Apr 13 15:57:17 2022 UTC
        :returns the current month based on the machine's time
        """
        pass

    def get_year(self) -> int:
        """
        get the machine's year
        example : 2022 for Wed Apr 13 15:57:17 2022 UTC
        :returns the current year based on the machine's time
        """
        pass
