@complexity(4)
String simpleStatements(int a, int b, int d) {
  if (a != b)                          // +1 if
    return "no conditions";
  if (a == b) {                        // +1 if
    return "simple equal";
  }
  if (a == b && b == d) {              // +1 if, +1 AND group
    return "still simple";
  }
  return "exit";
}

@complexity(2)
void simpleAnd(bool a, bool b) {
  if (a && b) return;                  // +1 if, +1 AND
}

@complexity(2)
void simpleOr(bool a, bool b) {
  if (a || b) return;                  // +1 if, +1 OR
}

@complexity(2)
void singleLongGroup(bool a, bool b, bool c, bool d) {
  if (a || b || c || d) return;        // +1 if, +1 OR group
}

@complexity(3)
void twoGroups(bool a, bool b, bool c, bool d) {
  if (a || b || c && d) return;        // +1 if, +1 OR, +1 AND
}

@complexity(3)
void parenthesisCreateNewGroupAnyway(bool a, bool b, bool c, bool d) {
  if (a || b || (c || d)) return;      // +1 if, +1 OR, +1 OR separate
}

@complexity(4)
void parenthesisInCenterSplitTheGroup(
    bool a, bool b, bool c, bool d, bool e, bool f) {
  if (a || b || !(c || d) || e || f)   // +1 if, +1 OR, +1 OR separate, +1 OR new
    return;
}

@complexity(1)
bool doesSupportOperation(bool exists, dynamic op) {
  return exists && support(op);        // +1 AND
}

bool support(dynamic op) => true;
