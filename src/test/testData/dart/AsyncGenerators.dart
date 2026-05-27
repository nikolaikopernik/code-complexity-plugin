// Dart async / sync generators — verify modifiers do NOT add complexity.
// Plan decision: async, async*, sync*, await, yield, yield* score 0 themselves.

@complexity(0)
Future<int> simpleAsync() async {
  await Future<void>.delayed(Duration.zero);   // await scores 0
  return 42;
}

@complexity(1)
Stream<int> asyncGenerator() async* {
  for (int i = 0; i < 3; i++) {                // +1 LOOP_FOR
    yield i;                                   // yield scores 0
    await Future<void>.delayed(Duration.zero); // await scores 0
  }
}

@complexity(2)
Stream<int> conditionalAsync(bool flag) async* {
  if (flag) {                                  // +1 IF
    yield 1;
  } else {                                     // +1 ELSE
    yield 2;
  }
}

@complexity(1)
Iterable<int> syncGenerator() sync* {
  for (int i = 0; i < 3; i++) {                // +1 LOOP_FOR
    yield i;
  }
}

@complexity(0)
Iterable<int> simpleSyncGen() sync* {
  yield 1;                                     // yield scores 0
  yield 2;
  yield* [3, 4];                               // yield* scores 0
}

@complexity(0)
Future<int> awaitChain(Future<int> a, Future<int> b) async {
  final x = await a;                           // scores 0
  final y = await b;                           // scores 0
  return x + y;
}
