class Tests {
  @complexity(5)
  int fibonacci(int n) {
    if (n == 1)                                // +1 if
      return 1;
    else if (n == 0)                           // +1 ELSE
      return 0;
    else                                       // +1 ELSE
      return fibonacci(n - 1) + fibonacci(n - 2);  // +1 RECURSION, +1 RECURSION
  }
}
