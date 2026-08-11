@Complexity(1)
fun simpleDoWhile(a: Boolean) {
    do {                        // +1
        println(1)
    } while (a)
}

@Complexity(3)
fun doWhileNestsItsBody(a: Boolean, b: Boolean) {
    do {                        // +1
        if (b) {                // +2 (nesting=1)
            println(1)
        }
    } while (a)
}

@Complexity(4)
fun labeledBreakExitsOuterLoop(m: Int, n: Int) {
    outer@ for (i in 1..m) {    // +1
        for (j in 1..n) {       // +2 (nesting=1)
            break@outer         // +1
        }
    }
}

@Complexity(3)
fun unlabeledBreakIsFree(m: Int, n: Int) {
    for (i in 1..m) {           // +1
        for (j in 1..n) {       // +2 (nesting=1)
            break               // +0, it only exits the loop it sits in
        }
    }
}

@Complexity(4)
fun labeledContinueSkipsOuterLoop(m: Int, n: Int) {
    outer@ for (i in 1..m) {    // +1
        for (j in 1..n) {       // +2 (nesting=1)
            continue@outer      // +1
        }
    }
}
