class Tests {
  @complexity(1)
  void tryDoesNotAddToComplexityNorNesting() {
    try {
      if (true) {                       // +1
        parseFile("salary.txt");
      }
    } on Exception {
      // empty - no points if no actual handler body branching
    }
  }

  @complexity(4)
  void catchAddsToBoth(dynamic log) {
    try {
      parseFile("salary.txt");
    } on RangeError catch (e) {           // +1 catch
      if (e.toString() == "Not found") {  // +2 (nesting=1)
        log.warn(e.toString());
      }
    } on Exception catch (e) {            // +1 catch
      log.error(e.toString());
    }
  }

  @complexity(1)
  void catchWithGenericException() {
    try {
      rethrowSomething("abc");
    } on Object catch (e) {               // +1 catch
      print(e);
    }
  }
}

void parseFile(String path) {}
void rethrowSomething(String s) {}
