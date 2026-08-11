@Complexity(3)
fun catchAddsComplexityAndNesting(a: Boolean) {
    try {
        println(1)
    } catch (e: Exception) {    // +1
        if (a) {                // +2 (nesting=1)
            println(2)
        }
    }
}

@Complexity(1)
fun finallyIsFree(a: Boolean) {
    try {
        if (a) {                // +1
            println(1)
        }
    } finally {                 // +0
        println(2)
    }
}
