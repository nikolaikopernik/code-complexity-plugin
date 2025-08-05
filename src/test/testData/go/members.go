package main

var proc = struct {
	Call func(variables []int) int
}{
	//go:generate complexity 4
	Call: func(variables []int) int {
		m := 0
		for i, v := range variables { //+1
			if i == 0 { //+1 (nested +1)
				m = v
			} else if v < m { //+1
				m = v
			}
		}
		return m
	},
}

//go:generate complexity 1
var myFunc = func(variables []int) int {
	m := 0
	for i, v := range variables { //+1
		if i == 0 { //+1 (nested +1)
			m = v
		} else if v < m { //+1
			m = v
		}
	}
	return m
}

var (
	//go:generate complexity 4
	myFuncInVars = func(variables []int) int {
		m := 0
		for i, v := range variables { //+1
			if i == 0 { //+1 (nested +1)
				m = v
			} else if v < m { //+1
				m = v
			}
		}
		return m
	}
)
