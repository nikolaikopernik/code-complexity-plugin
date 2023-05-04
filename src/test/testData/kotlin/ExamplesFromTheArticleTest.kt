@Complexity(7)
fun testBinaryExpr(b: Boolean) {
    if (a               // +1 for `if`
        && b && c       // +1
        || d || e       // +1
        && f            // +1
    ) {
    }

    if (a               // +1 for `if`
        &&              // +1
        !(b && c)
    ) {    // +1
    }
}


@Complexity(35)
@kotlin.Throws(PersistitInterruptedException::class, RollbackException::class)
private fun addVersion(entry: Entry<*, *>, txn: Transaction) {
    val ti: TransactionIndex = _persistit.getTransactionIndex()
    while (true) { // +1
        try {
//            synchronized(this) {
            if (frst != null) { // +2 (nesting = 1)
                if (frst.getVersion() > entry.getVersion()) { // +3 (nesting = 2)
                    throw RollbackException()
                }
                if (txn.isActive()) { // +3 (nesting = 2)
                    var e: Entry<*, *> = frst
                    while (e != null) {                                 // +4 (nesting = 3)
                        val version: Long = e.getVersion()
                        val depends: Long = ti.wwDependency(
                            version,
                            txn.getTransactionStatus(), 0
                        )
                        if (depends == PrinterStateReason.TIMED_OUT) { // +5 (nesting = 4)
                            throw WWRetryException(version)
                        }
                        if (depends != 0 // +5 (nesting = 4)
                            && depends != ABORTED // +1
                        ) {
                            throw RollbackException()
                        }
                        e = e.getPrevious()
                    }
                }
//                }
                entry.setPrevious(frst)
                frst = entry
                break
            }
        } catch (re: WWRetryException) { // +2 (nesting = 1)
            try {
                val depends: Long = _persistit.getTransactionIndex()
                    .wwDependency(
                        re.getVersionHandle(), txn.getTransactionStatus(),
                        SharedResource.DEFAULT_MAX_WAIT_TIME
                    )
                if (depends != 0 // +3 (nesting = 2)
                    && depends != ABORTED // +1
                ) {
                    throw RollbackException()
                }
            } catch (ie: InterruptedException) { // +3 (nesting = 2)
                throw PersistitInterruptedException(ie)
            }
        } catch (ie: InterruptedException) { // +2 (nesting = 1)
            throw PersistitInterruptedException(ie)
        }
    }
} // total complexity == 35


@Complexity(9)
fun myMethod() {
    try {
        if (a) {                       // +1
            for (i in 1..10) {         // +2 (nesting=1)
                while (b) {            // +3 (nesting=2)

                }
            }
        }
    } catch (Exception e) {            // +1
        if (c) {
        }                     // +2 (nesting=1)
    }
} // Cognitive Complexity 9


@Complexity(2)
fun myMethod2() {
    val r = Runnable {       // +0 (but nesting level is now 1)
        if (a) {             // +2 (nesting=1)
        }
    }
} // Cognitive Complexity 2


@Complexity(7)
fun sumOfPrimes(max: Int): Int {
    var total = 0
    OUT@ for (i in 1..max) {           // +1
        for (j in 2 until i) {         // +2
            if (i % j == 0) {          // +3
                continue@OUT           // +1
            }
        }
        total += i
    }
    return total
} // Cognitive Complexity 7


@Nullable
@Complexity(20)
private fun overriddenSymbolFrom(classType: ClassJavaType): MethodJavaSymbol? {
    if (classType.isUnknown()) { // +1
        return unknownMethodSymbol
    }
    var unknownFound = false
    val symbols: List<JavaSymbol> = classType.getSymbol().members().lookup(name)
    for (overrideSymbol in symbols) { // +1
        if (overrideSymbol.isKind(JavaSymbol.MTH) // +2 (nesting = 1)
            && !overrideSymbol.isStatic()  // +1
        ) {
            val methodJavaSymbol: MethodJavaSymbol = overrideSymbol as MethodJavaSymbol
            if (canOverride(methodJavaSymbol)) { // +3 (nesting = 2)
                val overriding: Boolean = checkOverridingParameters(
                    methodJavaSymbol,
                    classType
                )
                if (overriding == null) { // +4 (nesting = 3)
                    if (!unknownFound) { // +5 (nesting = 4)
                        unknownFound = true
                    }
                } else if (overriding) { // +1
                    return methodJavaSymbol
                }
            }
        }
    }
    return if (unknownFound) { // +1
        unknownMethodSymbol
    } else null                // +1!
}


@Complexity(1)
fun getWords(int number): String {
    when (number) {         // +1
        1 -> return "one";
        2 -> return "a couple";
        3 -> return "a few";
        else -> return "lots";
    }
} // Cognitive Complexity 1


@Complexity(21)
private fun toRegexp(antPattern: String,
                     directorySeparator: String): String? {
    val escapedDirectorySeparator = '\\'.toString() + directorySeparator
    val sb = StringBuilder(antPattern.length)
    sb.append('^')
    var i = if (antPattern.startsWith("/") ||  // +1
        antPattern.startsWith("\\")            // +1
    ) 1 else 0 // +1
    while (i < antPattern.length) { // +1
        val ch = antPattern[i]
        if (SPECIAL_CHARS.indexOf(ch) !== -1) { // +2 (nesting = 1)
            sb.append('\\').append(ch)
        } else if (ch == '*') { // +1
            if (i + 1 < antPattern.length // +3 (nesting = 2)
                && antPattern[i + 1] == '*'       // +1
            ) {
                i += if (i + 2 < antPattern.length // +4 (nesting = 3)
                    && isSlash(antPattern[i + 2])  // +1
                ) {
                    sb.append("(?:.*")
                        .append(escapedDirectorySeparator).append("|)")
                    2
                } else { // +1
                    sb.append(".*")
                    1
                }
            } else { // +1
                sb.append("[^").append(escapedDirectorySeparator).append("]*?")
            }
        } else if (ch == '?') { // +1
            sb.append("[^").append(escapedDirectorySeparator).append("]")
        } else if (isSlash(ch)) { // +1
            sb.append(escapedDirectorySeparator)
        } else { // +1
            sb.append(ch)
        }
        i++
    }
    sb.append('$')
    return sb.toString()
} // total complexity = 21

