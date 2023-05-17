@complexity(5)
def fibonacci(n) -> int:
    if n == 1:
        return 1
    elif n == 0:
        return 0
    else:
        return fibonacci(n - 1) + fibonacci(n - 2)
