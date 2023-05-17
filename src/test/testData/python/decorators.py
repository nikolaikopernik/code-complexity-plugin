@complexity(1)
def a_decorator(a, b):
    def inner(func): # nesting = 0
        if condition: # +1
            print(b)
        func()
    return inner


@complexity(2)
def not_a_decorator(a, b):
    my_var = a*b
    def inner(func): # nesting = 1
        if condition: # +1 structure, +1 nesting
            print(b)
        func()
    return inner


@complexity(1)
def decorator_generator(a):
    def generator(func):
        def decorator(func): # nesting = 0
            if condition: # +1
                print(b)
        return func()
    return decorator
return generator
