class Switch{app_name}{instance_name}():
    def on(self, physical_state: PhysicalState):
        '''
        pre: 
        post: physical_state.{formatted_group_address}  == True
        '''
        physical_state.{formatted_group_address} = True
        

    def off(self, physical_state: PhysicalState):
        '''
        pre: 
        post: physical_state.{formatted_group_address}  == False
        '''
        physical_state.{formatted_group_address} = False

    def is_on(self, physical_state: PhysicalState) -> bool:
        '''
        pre: 
        post: physical_state.{formatted_group_address}  == __return__
        '''
        return physical_state.{formatted_group_address}

