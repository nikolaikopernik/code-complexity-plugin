// Mirrors dart/NullSafety.dart: elvis scores like Dart's ??, as an OR-style operator sequence.

@Complexity(1)
fun singleElvis(a: String?): String = a ?: "d"                  // +1

@Complexity(1)
fun chainedElvis(a: String?, b: String?): String = a ?: b ?: "d" // +1 (consecutive ?: = one sequence)

@Complexity(2)
fun elvisInsideIf(cond: Boolean?): String {
    if (cond ?: false) {                                        // +1 IF, +1 elvis
        return "y"
    }
    return "n"
}

@Complexity(2)
fun elvisThenOr(a: Boolean?, b: Boolean): Boolean = (a ?: false) || b // +1 elvis group, +1 OR

@Complexity(0)
fun safeCallChainIsFree(a: String?): Int? = a?.length?.dec()    // +0 (?. is not a decision)
