class DateObj():
    """
    inv: self.value>=0
    """
    descr = ""
    min_v= 0
    max_v= 0
    internal_state_variable_names = ["time_min",
                                     "time_hour",
                                     "time_day",
                                     "time_weekday",
                                     "time_month",
                                     "time_year"
                                     ]
    def __init__(self, v: int):
        assert v >= 0, "time must be positive"
        self.value = v

    def get_descr(self):
        return self.descr
    def get_value(self) :
        return self.value


class Day(DateObj):
    descr = "time_day"
    min_v= 1
    max_v= 7
class Hour(DateObj):
    descr = "time_hour"
    min_v= 0
    max_v= 23

class Week(DateObj):
    descr = "time_weekday"
    min_v= 1
    max_v= 4



class Month(DateObj):
    descr = "time_month"
    min_v= 1
    max_v= 12

class Minute(DateObj):
    descr = "time_min"
    min_v= 0
    max_v= 59
