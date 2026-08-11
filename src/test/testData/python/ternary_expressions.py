@complexity(1)
def simple_ternary(a):
    return 1 if a else 2                    # +1


@complexity(2)
def ternary_with_logical_condition(b, c):
    return 1 if b and c else 2              # +1 ternary, +1 AND


@complexity(3)
def nested_ternary(a, b):
    return (1 if a else 2) if b else 3      # +1 outer, +2 inner (nesting=1)


@complexity(3)
def ternary_inside_if(a, b):
    if b:                                   # +1
        return 1 if a else 2                # +2 (nesting=1)
    return 0
