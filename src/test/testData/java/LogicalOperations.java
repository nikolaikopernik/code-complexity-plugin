class Tests {
    @Complexity(4)
    public String simpleStatements() {
        if (true)                      // +1 if
            return "no conditions"

        if (a == b) {                  // +1 if
            return "simple equal"
        }

        if (                           // +1 if
            a == b && b == d) {        // +1 AND group
            return "still simple"
        }
        return "exit"
    }

    @Complexity(2)
    public void simpleAnd() {
        if (                           // +1 if
            a && b)                    // +1 AND
            return
    }


    @Complexity(2)
    public void simpleOr() {
        if (                           // +1 if
            a || b) {                  // +1 OR
            return
        }
    }


    @Complexity(2)
    public void singleLongGroup() {
        if (                          // +1 if
            a || b || c || d) {       // +1 OR GROUP
            return
        }
    }

    @Complexity(3)
    public void twoGroups() {
        if (                          // +1 if
            a || b ||                 // +1 OR
                c && d) {             // +1 AND
            return
        }
    }


    @Complexity(3)
    public void parenthesisCreateNewGroupAnyway() {
        if (                          // +1 if
            a || b ||                 // +1 OR
                (c || d)) {           // +1 OR separate
            return
        }
    }

    @Complexity(4)
    public void parenthesisInCenterSplitTheGroup() {
        if (                          // +1 if
            a || b ||                 // +1 OR
                !(c || d)             // +1 OR separate
                    || e || f) {      // +1 new OR
            return
        }
    }

    @Complexity(1)
    public boolean doesSupportOperation() {
        return exists && support(op)
    }
}
