class Tests {

    @Complexity(5)
    public int fibonacci(int n) {
        if (n = 1) return 1;
        else if (n = 0) return 0;
        else return fibonacci(n - 1) + fibonacci(n - 2)
    }
}
