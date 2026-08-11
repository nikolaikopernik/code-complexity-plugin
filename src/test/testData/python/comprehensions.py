# Comprehension machinery (the for/if clauses) scores nothing on purpose: a comprehension
# reads as one filter/transform expression, not as branching control flow. Expressions
# *inside* one still score normally.

@complexity(0)
def plain_comprehension(xs):
    return [x for x in xs]                       # +0


@complexity(0)
def filtered_comprehension(xs):
    return [x for x in xs if x > 0]              # +0, the comprehension `if` is not a branch


@complexity(0)
def nested_loops_comprehension(xs, ys):
    return [(x, y) for x in xs for y in ys]      # +0, still one expression


@complexity(0)
def dict_and_generator_comprehensions(items, xs):
    d = {k: v for k, v in items}                 # +0
    g = (x for x in xs)                          # +0
    return d, g


@complexity(1)
def expressions_inside_still_score(xs):
    return [1 if x else 2 for x in xs]           # +1 for the ternary


@complexity(3)
def comprehension_does_not_leak_nesting(xs, a):
    if a:                                        # +1
        ys = [x for x in xs if x]
        if a:                                    # +2 (nesting=1)
            return ys
    return []


@complexity(0)
def logical_operator_in_filter(xs, a, b):
    # Pins current behaviour, NOT a considered decision: a plain `if a and b:` scores the
    # `and`, so this 0 is inconsistent. Tracked as F17 on the test-audit whiteboard.
    return [x for x in xs if a and b]            # +0 today
