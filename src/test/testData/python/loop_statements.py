@complexity(1)
def generate_data(df: pd.DataFrame):
    for i in df.toList():                                  # +1
        df = df.copy()
        df.loc[:, 'text'] = df.text.map(lambda x: x[0:i])
        df.loc[:, 'ids'] = df.ids.map(lambda x: x[0:i])
        df.loc[:, 'scores'] = df.scores.map(lambda x: x[0:i])
    return df


@complexity(0)
def comprehensions(a: list):
    return [2 * x for x in a]


@complexity(2)
def hello_world_example():
    count = 0
    while count < 3:          # +1
        count = count + 1
        print("Hello Geek")
    else:                     # +1
        print("In Else Block")


@complexity(9)
def controls_in_the_loop():
    for a in 'geeksforgeeks':        # +1
        for b in 'geeksforgeeks':    # +2 (nesting=1)
            if a > b:                # +3 (nesting=2)
                break                # +1
            else:                    # +1
                continue             # +1


