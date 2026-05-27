// Dart 3 switch expressions — distinct PSI from switch statements.
// Plan decision: score same as switch statement (+1 + nesting).

@complexity(1)
String basicSwitchExpression(int n) {
  return switch (n) {                     // +1 SWITCH
    1 => 'one',
    2 => 'two',
    _ => 'other',
  };
}

@complexity(3)
String switchExpressionWithCondition(int n, bool flag) {
  if (flag) {                              // +1 IF (nesting becomes 1)
    return switch (n) {                    // +2 SWITCH (nesting=1, point=2)
      _ => 'fallback',
    };
  }
  return 'none';
}

@complexity(1)
String switchExpressionWithGuards(int n) {
  return switch (n) {                     // +1 SWITCH (when guards score 0)
    int x when x > 0 => 'pos',
    int x when x < 0 => 'neg',
    _ => 'zero',
  };
}

@complexity(3)
String nestedSwitchExpressions(int n, int m) {
  return switch (n) {                     // +1 SWITCH (nesting=0, point=1)
    0 => switch (m) {                     // +2 SWITCH (nesting=1, point=2)
          _ => 'inner',
        },
    _ => 'outer',
  };
}
