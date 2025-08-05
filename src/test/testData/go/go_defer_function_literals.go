package main

//go:generate complexity 3
func goGoFunctionAddsOnlyNesting(i int) <-chan int {
	c := make(chan int)
	go func() {
		if i%2 == 0 { //+1 (nested +1)
			c <- 1
		} else { //+1
			c <- 2
		}
		close(c)
	}()
	return c
}

//go:generate complexity 3
func deferFunctionAddsOnlyNesting(i int, c chan int) {
	defer func() {
		if i%2 == 0 { //+1 (nested +1)
			c <- 1
		} else { //+1
			c <- 2
		}
		close(c)
	}()
	c <- i
}
