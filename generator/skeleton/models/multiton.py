# Default file, will be overwritten while running

def multiton(cls):
    instances = {}
    allowed_names = []
    def getinstance(name):
        if name not in allowed_names:
            raise ValueError(f"Name '{name}' not allowed")
        if name not in instances:
            instances[name] = cls(name)
        return instances[name]  
    return getinstance