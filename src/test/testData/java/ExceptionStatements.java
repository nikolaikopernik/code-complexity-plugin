class Tests {
    @Complexity(1)
    void tryDoesNotAddToComplexityNorNesting() {
        try {
            if (true) {                    // +1
                parseFile("salary.txt");
            }
        }
    }

    @Complexity(4)
    void catchAddsToBoth() {
        try {
            parseFile("salary.txt");
        } catch (RuntimeException e) {       // +1
            if (e.message == "Not found") {  // +2 (nesting=1)
                log.warn(e.getMessage())
            }
        } catch (Exception e) {              // +1
            log.error(e.getMessage())
        }
    }

    @Complexity(1)
    void catchWithMultipleExceptions() {
        try {
            rethrow("abc");
        } catch (FirstException | SecondException | ThirdException e) {  // +1
            //below assignment will throw compile time exception since e is final
            //e = new Exception();
            System.out.println(e.getMessage());
        }
    }
}
