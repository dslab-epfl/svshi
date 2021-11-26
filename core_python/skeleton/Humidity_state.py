class HumiditySensor{app_name}{instance_name}():
    def read(self, physical_state: PhysicalState) -> float:
        '''
        pre:
        post: physical_state.{formatted_group_address} == __return__
        '''
        return physical_state.{formatted_group_address}
