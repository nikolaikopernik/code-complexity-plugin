@complexity(1)
def simple_match(x):
    match x:                    # +1
        case 1:
            return 1
        case _:
            return 0


@complexity(3)
def match_adds_nesting(x, y):
    match x:                    # +1
        case 1:
            if y:               # +2 (nesting=1)
                return 1
    return 0


@complexity(3)
def match_inside_if(x, y):
    if y:                       # +1
        match x:                # +2 (nesting=1)
            case 1:
                return 1
    return 0


@complexity(1)
def match_with_guard(x, y):
    match x:                    # +1
        case 1 if y:            # +0 (a plain guard is not a separate decision)
            return 1
    return 0


@complexity(2)
def match_with_logical_guard(x, y, z):
    match x:                    # +1
        case 1 if y and z:      # +1 AND
            return 1
    return 0
