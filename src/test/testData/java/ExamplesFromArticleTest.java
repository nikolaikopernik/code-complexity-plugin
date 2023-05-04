class Tests {
    @Complexity(7)
    void exampleOne(boolean b) {
        if (a               // +1 for `if`
            && b && c       // +1
            || d || e       // +1
            && f            // +1
        ) {
        }

        if (a               // +1 for `if`
            &&              // +1
            !(b && c)) {    // +1
        }
    }


    @Complexity(35)
    private void exampleTwo(Entry<Object, Object> entry, Transaction trx) throws PersistitInterruptedException, RollbackException {
        TransactionIndex ti = _persistit.getTransactionIndex();
        while (true) {                                            // +1
            try {
//            synchronized(this) {
                if (frst != null) {                               // +2 (nesting = 1)
                    if (frst.getVersion() > entry.getVersion()) { // +3 (nesting = 2)
                        throw new RollbackException();
                    }
                    if (txn.isActive()) {                         // +3 (nesting = 2)
                        Entry<Object, Object> e = frst;
                        while (e != null) {                       // +4 (nesting = 3)
                            Long version = e.getVersion();
                            Long depends = ti.wwDependency(
                                version,
                                txn.getTransactionStatus(), 0
                            );
                            if (depends == PrinterStateReason.TIMED_OUT) { // +5 (nesting = 4)
                                throw new WWRetryException(version);
                            }
                            if (depends != 0                      // +5 (nesting = 4)
                                && depends != ABORTED             // +1
                            ) {
                                throw new RollbackException();
                            }
                            e = e.getPrevious();
                        }
                    }
//                }
                    entry.setPrevious(frst);
                    frst = entry;
                    break;
                }
            } catch (WWRetryException re) {                        // +2 (nesting = 1)
                try {
                    Long depends = _persistit.getTransactionIndex()
                                             .wwDependency(
                                                 re.getVersionHandle(), txn.getTransactionStatus(),
                                                 SharedResource.DEFAULT_MAX_WAIT_TIME
                                             );
                    if (depends != 0                               // +3 (nesting = 2)
                        && depends != ABORTED                      // +1
                    ) {
                        throw new RollbackException();
                    }
                } catch (InterruptedException ie) {                // +3 (nesting = 2)
                    throw new PersistitInterruptedException(ie);
                }
            } catch (InterruptedException ie) {                    // +2 (nesting = 1)
                throw new PersistitInterruptedException(ie);
            }
        }
    } // total complexity == 35

    @Complexity(9)
    void exampleThree() {
        try {
            if (a) {                            // +1
                for (int i = 1; i < 10; i++) {  // +2 (nesting=1)
                    while (b) {                 // +3 (nesting=2)

                    }
                }
            }
        } catch (Exception e) {                 // +1
            if (c) {                            // +2 (nesting=1)
            }
        }
    } // Cognitive Complexity 9

    @Complexity(2)
    void exampleFour() {
        Runnable r = () -> {       // +0 (but nesting level is now 1)
            if (a) {
            }                      // +2 (nesting=1)
        };
    } // Cognitive Complexity 2

    @Complexity(7)
    int exampleFive(int max) {
        int total = 0;
        OUT:
        for (int i = 1; i < max; i++) {    // +1
            for (int j = 2; j < i; j++) {  // +2
                if (i % j == 0) {          // +3
                    continue OUT;          // +1
                }
            }
            total += i;
        }
        return total
    } // Cognitive Complexity 7

    @Complexity(1)
    String exampleSix(int number) {
        switch (number) {               // +1
            case 1:
                return "one";
            case 2:
                return "a couple";
            case 3:
                return "a few";
            default:
                return "lots";
        }
    } // Cognitive Complexity 1


    @Nullable
    @Complexity(19)
    private MethodJavaSymbol exampleSeven(classType:ClassJavaType) {
        if (classType.isUnknown()) {                   // +1
            return unknownMethodSymbol;
        }
        boolean unknownFound = false;
        List<JavaSymbol> symbols = classType.getSymbol().members().lookup(name);
        for (JavaSymbol overrideSymbol: symbols) {     // +1
            if (overrideSymbol.isKind(JavaSymbol.MTH)  // +2 (nesting = 1)
                && !overrideSymbol.isStatic()) {       // +1
                MethodJavaSymbol methodJavaSymbol = (MethodJavaSymbol) overrideSymbol;
                if (canOverride(methodJavaSymbol)) {   // +3 (nesting = 2)
                    Boolean overriding = checkOverridingParameters(methodJavaSymbol, classType);
                    if (overriding == null) {          // +4 (nesting = 3)
                        if (!unknownFound) {           // +5 (nesting = 4)
                            unknownFound = true;
                        }
                    } else if (overriding) {           // +1
                        return methodJavaSymbol;
                    }
                }
            }
        }
        return unknownFound ? unknownMethodSymbol : null; // +1!
    } // total complexity == 19


    @Complexity(20)
    private String exampleEight(String antPattern, String directorySeparator) {
        String escapedDirectorySeparator = '\\'.toString() + directorySeparator;
        StringBuilder sb = new StringBuilder(antPattern.length);
        sb.append('^');
        int i = (antPattern.startsWith("/")
                 || antPattern.startsWith("\\"))       // +1
            ? 1 : 0 ;                                  // +1 ternary
        while (i < antPattern.length) {                // +1
            char ch = antPattern[i];
            if (SPECIAL_CHARS.indexOf(ch) != -1) {     // +2 (nesting = 1)
                sb.append('\\').append(ch);
            } else if (ch == '*') {                    // +1
                if (i + 1 < antPattern.length          // +3 (nesting = 2)
                    && antPattern[i + 1] == '*'        // +1
                ) {
                    if (i + 2 < antPattern.length      // +4 (nesting = 3)
                        && isSlash(antPattern[i + 2])  // +1
                    ) {
                        sb.append("(?:.*")
                          .append(escapedDirectorySeparator).append("|)");
                        i += 2;
                    } else {                           // +1
                        sb.append(".*");
                        i += 1;
                    }
                } else {                               // +1
                    sb.append("[^").append(escapedDirectorySeparator).append("]*?");
                }
            } else if (ch == '?') {                    // +1
                sb.append("[^").append(escapedDirectorySeparator).append("]");
            } else if (isSlash(ch)) {                  // +1
                sb.append(escapedDirectorySeparator);
            } else {                                   // +1
                sb.append(ch)
            }
            i++
        }
        sb.append('$');
        return sb.toString();
    } // total complexity = 20

}
