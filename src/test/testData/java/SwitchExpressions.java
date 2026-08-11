class Tests {
    @Complexity(1)
    int arrowSwitchExpression(int x) {
        return switch (x) {          // +1
            case 1 -> 1;
            default -> 0;
        };
    }

    @Complexity(1)
    int yieldSwitchExpression(int x) {
        return switch (x) {          // +1
            case 1: yield 1;
            default: yield 0;
        };
    }

    @Complexity(3)
    int switchExpressionAddsNesting(int x, boolean b) {
        return switch (x) {          // +1
            case 1 -> {
                if (b) {             // +2 (nesting=1)
                    yield 1;
                }
                yield 0;
            }
            default -> 0;
        };
    }

    @Complexity(3)
    int switchExpressionInsideIf(int x, boolean b) {
        if (b) {                     // +1
            return switch (x) {      // +2 (nesting=1)
                case 1 -> 1;
                default -> 0;
            };
        }
        return 0;
    }
}
