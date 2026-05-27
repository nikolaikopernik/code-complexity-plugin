// Dart-specific: ??, ??=, !, ?. — verify scoring matches plan decisions.

@complexity(1)
int? singleNullCoalesce(int? a, int? b) {
  return a ?? b;                         // +1 (?? scores like an OR sequence)
}

@complexity(1)
int? chainedNullCoalesce(int? a, int? b, int? c) {
  return a ?? b ?? c;                    // +1 (consecutive ?? = single sequence)
}

@complexity(2)
int? mixedOrAndCoalesce(int? a, int? b, bool? cond) {
  if (cond ?? false) {                   // +1 IF, +1 ?? sequence
    return a;
  }
  return b;
}

@complexity(0)
void compoundNullAssign(Map<String, int> m) {
  m["x"] ??= 0;                          // ??= is assignment, not a binary expression
}

@complexity(0)
void bangOperator(int? a) {
  print(a!);                             // ! null-assertion, not a decision
}

@complexity(0)
String? questionDotChain(String? s) {
  return s?.toLowerCase();               // ?. null-aware access, not a decision
}

@complexity(3)
int? guardWithCoalesce(int? a, int? b, int? c) {
  if (a == null && b == null) {          // +1 IF, +1 AND
    return c;
  }
  return a ?? b ?? c;                    // +1 ?? sequence
}
