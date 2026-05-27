class Tests {
  @complexity(7)
  void exampleOne(bool a, bool b, bool c, bool d, bool e, bool f) {
    if (a                             // +1 for `if`
        && b && c                     // +1
        || d || e                     // +1
        && f                          // +1
    ) {}

    if (a                             // +1 for `if`
        &&                            // +1
        !(b && c)) {                  // +1
    }
  }

  dynamic frst;
  dynamic ti;

  @complexity(35)
  void exampleTwo(dynamic entry, dynamic txn) {
    const ABORTED = 1;
    while (true) {                                            // +1
      try {
        if (frst != null) {                                   // +2 (nesting = 1)
          if (frst.version > entry.version) {                 // +3 (nesting = 2)
            throw StateError('Rollback');
          }
          if (txn.isActive) {                                 // +3 (nesting = 2)
            var e = frst;
            while (e != null) {                               // +4 (nesting = 3)
              final version = e.version;
              final depends = ti.wwDependency(version, txn.status, 0);
              if (depends == 'TIMED_OUT') {                   // +5 (nesting = 4)
                throw StateError('WWRetry');
              }
              if (depends != 0                                // +5 (nesting = 4)
                  && depends != ABORTED                       // +1
              ) {
                throw StateError('Rollback');
              }
              e = e.previous;
            }
          }
          entry.previous = frst;
          frst = entry;
          break;
        }
      } on StateError catch (re) {                            // +2 (nesting = 1)
        try {
          final depends = ti.wwDependency(re.message, txn.status, 0);
          if (depends != 0                                    // +3 (nesting = 2)
              && depends != ABORTED                           // +1
          ) {
            throw StateError('Rollback');
          }
        } on FormatException catch (ie) {                     // +3 (nesting = 2)
          throw StateError('Interrupted');
        }
      } on FormatException catch (ie) {                       // +2 (nesting = 1)
        throw StateError('Interrupted');
      }
    }
  } // total complexity == 35

  @complexity(9)
  void exampleThree(bool a, bool b, bool c) {
    try {
      if (a) {                                // +1
        for (int i = 1; i < 10; i++) {        // +2 (nesting=1)
          while (b) {                         // +3 (nesting=2)
          }
        }
      }
    } on Exception {                          // +1
      if (c) {                                // +2 (nesting=1)
      }
    }
  } // Cognitive Complexity 9

  @complexity(2)
  void exampleFour(bool a) {
    Function r = () {        // +0 (nesting becomes 1)
      if (a) {               // +2 (nesting=1)
      }
    };
  } // Cognitive Complexity 2

  @complexity(7)
  int exampleFive(int max) {
    int total = 0;
    OUT:
    for (int i = 1; i < max; i++) {    // +1
      for (int j = 2; j < i; j++) {    // +2
        if (i % j == 0) {              // +3
          continue OUT;                // +1 (labeled)
        }
      }
      total += i;
    }
    return total;
  } // Cognitive Complexity 7

  @complexity(1)
  String exampleSix(int number) {
    switch (number) {                // +1
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

  @complexity(19)
  dynamic exampleSeven(dynamic classType, String name) {
    final unknownMethodSymbol = Object();
    if (classType.isUnknown) {                    // +1
      return unknownMethodSymbol;
    }
    bool unknownFound = false;
    final symbols = classType.symbol.members.lookup(name) as List;
    for (final overrideSymbol in symbols) {       // +1
      if (overrideSymbol.isMethod                 // +2 (nesting=1)
          && !overrideSymbol.isStatic) {          // +1
        final methodJavaSymbol = overrideSymbol;
        if (canOverride(methodJavaSymbol)) {      // +3 (nesting=2)
          final overriding =
              checkOverridingParameters(methodJavaSymbol, classType);
          if (overriding == null) {               // +4 (nesting=3)
            if (!unknownFound) {                  // +5 (nesting=4)
              unknownFound = true;
            }
          } else if (overriding) {                // +1 ELSE
            return methodJavaSymbol;
          }
        }
      }
    }
    return unknownFound ? unknownMethodSymbol : null;  // +1 ternary
  } // total complexity == 19

  bool canOverride(dynamic s) => true;
  dynamic checkOverridingParameters(dynamic m, dynamic c) => null;

  @complexity(20)
  String exampleEight(String antPattern, String directorySeparator) {
    const SPECIAL_CHARS = ".+";
    final escapedDirectorySeparator = '\\' + directorySeparator;
    final sb = StringBuffer();
    sb.write('^');
    int i = (antPattern.startsWith("/")
            || antPattern.startsWith("\\"))      // +1 OR
        ? 1
        : 0;                                     // +1 ternary
    while (i < antPattern.length) {              // +1
      final ch = antPattern[i];
      if (SPECIAL_CHARS.indexOf(ch) != -1) {     // +2 (nesting = 1)
        sb.write('\\');
        sb.write(ch);
      } else if (ch == '*') {                    // +1
        if (i + 1 < antPattern.length            // +3 (nesting = 2)
            && antPattern[i + 1] == '*'          // +1
        ) {
          if (i + 2 < antPattern.length          // +4 (nesting = 3)
              && isSlash(antPattern[i + 2])      // +1
          ) {
            sb.write("(?:.*");
            sb.write(escapedDirectorySeparator);
            sb.write("|)");
            i += 2;
          } else {                               // +1
            sb.write(".*");
            i += 1;
          }
        } else {                                 // +1
          sb.write("[^");
          sb.write(escapedDirectorySeparator);
          sb.write("]*?");
        }
      } else if (ch == '?') {                    // +1
        sb.write("[^");
        sb.write(escapedDirectorySeparator);
        sb.write("]");
      } else if (isSlash(ch)) {                  // +1
        sb.write(escapedDirectorySeparator);
      } else {                                   // +1
        sb.write(ch);
      }
      i++;
    }
    sb.write('\$');
    return sb.toString();
  } // total complexity = 20

  bool isSlash(String c) => c == '/' || c == '\\';
}
