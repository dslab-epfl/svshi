import time


class InternalState:
    ...


def check_time_property(self, frequency, duration, condition, internal_state: InternalState, check_num: int) -> bool:
    check_obj = internal_state.check_condition[check_num]
    internal_time = int(time.mktime(internal_state.date_time))
    if condition:
        if check_obj.condition_was_true:
            # condition was true at last iteration, we need to increase the duration's counter
            if internal_time - check_obj.start_condition_true >= duration.value:
                #duration.value is in seconds
                check_obj.start_frequency = internal_time # condition is true since the given start frequency
        else:
            #start the counter to see how long the condition remains true
            check_obj.condition_was_true = True
            check_obj.start_condition_true = internal_time
    else:
        if check_obj.condition_was_true:
            if internal_time - check_obj.start_condition_true >= duration.value:
                check_obj.start_frequency = internal_time # condition is true since the given start frequency
            check_obj.condition_was_true = False
    frequency_reached = internal_time - check_obj.start_frequency >= frequency.value
    return not frequency_reached
