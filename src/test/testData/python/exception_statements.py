@complexity(2)
def try_multiple_catch():
    try:
        f = open("demofile.txt")
    except NameError:         # +1
        print("a")
    except:                   # +1
        print("b")
    finally:
        f.close()


@complexity(1)
def try_does_not_add_to_complexity_nor_nesting():
    try:
        if True:                        # +1
            parseFile("salary.txt")


@complexity(3)
def except_adds_to_both():
    try:
        f = open("demofile.txt")
    except:                   # +1
        if a:                 # +2
            print("a")
    finally:
        f.close()
