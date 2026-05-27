class Tests {
  int a = 0;
  List<int> counts = [];

  @complexity(4)
  void allPossibleLoops() {
    while (true) {           // +1
      a++;
    }

    do {                     // +1
      a++;
    } while (true);

    for (int i = 0; i < 10; i++) {     // +1
      a++;
    }

    for (final i in counts) {          // +1
      a++;
    }

    final tokens = <String>[];
    tokens.forEach((it) => a++);       // closure: +nesting only, no +complexity
  }

  @complexity(5)
  void loopsCreateNesting() {
    while (true) {           // +1
      if (true) {            // +2 (nesting = 1)
        a++;
      } else {               // +1
      }
    }

    for (final i in counts) {  // +1
      a++;
    }
  }

  @complexity(2)
  void lambdaAddsNestingOnly() {
    final tokens = <String>[];
    tokens.where((it) => it.isNotEmpty).forEach((it) {  // nesting = 1
      if (true) {             // +2 (nesting = 1)
        a++;
      }
    });
  }
}
