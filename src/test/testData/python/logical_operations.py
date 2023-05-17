@complexity(4)
def simple_statements(a, b, d):
    if a != b:
        return "no conditions"

    if a == b:
        return "simple equal"

    if a == b and b == d:
        return "still simple"
    return "exit"


@complexity(2)
def simple_and(a, b):
    if a and b:  # +1 if
        # +1 AND
        return


@complexity(2)
def simple_or(a, b):
    if a or b:  # +1 if
        # +1 OR
        return


@complexity(2)
def single_long_group(a, b, c, d):
    if a or b or c or d:  # +1 if
        # +1 OR GROUP
        return


@complexity(3)
def two_groups(a, b, c, d):
    if a or b or c and d:
        return


@complexity(3)
def parenthesis_create_new_group_anyway(a, b, c, d):
    if a or b or (c or d):  # +1 OR separate
        return


@complexity(4)
def parenthesis_in_center_split_the_group(a, b, c, d, e, f):
    if a or b or not (c or d) or e or f:
        return


@complexity(1)
def does_support_operation():
    return exists and support(op)
