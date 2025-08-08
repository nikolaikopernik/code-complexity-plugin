package main

//go:generate complexity 3
func selectCaseStatements(recv, fin <-chan int) int {
	s := 0
	for { //+1
		select { //+1 (nested +1)
		case v := <-recv:
			s += v
		case <-fin:
			return s
		}
	}
}

type MyTypes int

const (
	MyFirstCase MyTypes = iota
	MySecondCase
	MyThirdCase
	MyFourthCase
)

//go:generate complexity 1
func switchCaseStatements(e MyTypes) int {
	switch e {
	case MyFirstCase:
		return int(e)
	case MySecondCase:
		return int(e)
	case MyThirdCase:
		return int(e)
	case MyFourthCase:
		return int(e)
	}
	panic("unreachable")
}
