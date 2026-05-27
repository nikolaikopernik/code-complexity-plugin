// Dart 3 pattern matching: destructuring, `when` guards, `||` patterns.
// Plan decision: when guards and logical-or patterns inside cases score 0.
// The switch itself is the only decision point.

sealed class Shape {}
class Circle extends Shape {
  final double r;
  Circle(this.r);
}
class Square extends Shape {
  final double side;
  Square(this.side);
}

@complexity(1)
String destructurePair(Object o) {
  switch (o) {                            // +1 SWITCH
    case (int x, int y) when x > 0:        // when guard scores 0
      return 'pair pos';
    case (int x, _):                       // record pattern, no extra
      return 'pair any';
    default:
      return 'unknown';
  }
}

@complexity(1)
String orPatternCase(int x) {
  switch (x) {                            // +1 SWITCH
    case 1 || 2 || 3:                     // logical-OR pattern scores 0
      return 'small';
    case int n when n < 0:                // type pattern + when, scores 0
      return 'neg';
    default:
      return 'big';
  }
}

@complexity(1)
String objectPattern(Shape s) {
  switch (s) {                            // +1 SWITCH
    case Circle(r: var r) when r > 0:      // object pattern + when, scores 0
      return 'circle';
    case Square(side: _):                  // object pattern, no extra
      return 'square';
    default:
      return 'unknown';
  }
}

@complexity(3)
String switchInsideIf(Object o, bool flag) {
  if (flag) {                             // +1 IF
    switch (o) {                          // +2 SWITCH (nesting=1)
      case int():
        return 'int';
      default:
        return 'other';
    }
  }
  return 'none';
}
