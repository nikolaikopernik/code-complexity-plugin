class Tests {
    @Complexity(4)
    public void allPossibleLoops() {
        while (true) {           // +1
            a++;
        }

        do {                     // +1
            a++;
        } while (true)

        for (int i = 0; i++; i < 10) { // +1
            a++;
        }

        for (int i : counts) {    // +1
            a++;
        }

        List<String> tokens = new ArrayList();
        tokens.stream()
              .forEach(it -> a++);
    }

    @Complexity(5)
    public void loopsCreateNesting() {
        while (true) {            // +1
            if (true) {           // +2 (nesting = 1)
                a++
            } else {              // +1

            }
        }

        for (int i : counts) {    // +1
            a++;
        }
    }

    @Complexity(2)
    public void lambdaAddsNestingOnly() {
        List<String> tokens = new ArrayList();
        tokens.stream()
              .filter(it -> it != null)
              .forEach(it -> {         // nesting = 1
                  if (true) {          // +2 (nesting = 1)
                      a++
                  }
              });
    }
}
