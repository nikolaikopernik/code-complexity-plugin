package main

//go:generate complexity 5
func fibonacci(n int) int {
	if n == 0 { // +1 <1>
		return 1
	} else if n == 1 { // +1 <2>
		return 1
	} else { // +1 <3>
		return fibonacci(n-1) + // +1
			fibonacci(n-2) // +1
	}
}

type Recursion struct {
}

//go:generate complexity 5
func (r Recursion) fib(n int) int {
	if n == 0 {
		return 1
	} else if n == 1 {
		return 1
	} else {
		return r.fib(n-1) + r.fib(n-2)
	}
}
