class BinarySensor{app_name}{instance_name}():
    def is_on(self, physical_state: PhysicalState) -> bool:
        '''
        pre:
        post: physical_state.{formatted_group_address} == __return__
        '''
        return physical_state.{formatted_group_address}
