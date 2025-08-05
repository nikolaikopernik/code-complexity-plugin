package main

//go:generate complexity 3
func allPossibleLoops() {
	a := 0
	for a < 4 { // +1
		a++
	}
	for i := 0; i < 3; i++ { // +1
		a += i
	}
	s := []int{0, 2}
	for i := range s { // +1
		v := s[i]
		a += v
	}
}

type MyLoop struct {
	items []int
}

//go:generate complexity 3
func (l *MyLoop) methodAllPossibleLoops() int {
	s := len(l.items)
	t := 0
	for true { // +1
		t++
		break
	}
	for i := 0; i < s; i++ { // +1
		t++
	}
	for _, i := range l.items { // +1
		t += i
	}
	return t
}

//go:generate complexity 5
func LoopsCreateNesting(items []interface{}) {
	for i := 0; i < 10; i++ { // +1
		if i%2 == 0 { // +1 nesting +1
			i++
			break
		} else { // +1
			i++
		}
	}
	a := 0
	for range items { // +1
		a++
	}
}
