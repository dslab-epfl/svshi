###
### DO NOT TOUCH THIS FILE!!!
###

def multiton(cls):
    instances = {}

    def getinstance(name):
        if name not in instances:
            instances[name] = cls(name)
        return instances[name]

    return getinstance
