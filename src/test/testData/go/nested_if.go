package main

//go:generate complexity 5
func NestedIf(vs []int) int {
	if 0 < len(vs) { // +1
		v := vs[0]
		if 2 < v { // +1(nested +1)
			return v
		} else { // +1
			return len(vs)
		}
	} else { // +1
		return 0
	}
}

//go:generate complexity 8
func labelBreak(m, n int, s ...int) {
	for i := 0; i < m; i++ { //+1
	sub:
		for j := i + 1; j < n; j++ { //+1 (nested +1)
			if s[i] == s[j] { //+1 (nested +2)
				break sub //+1
			} else if s[j] == 0 { //+1
				break
			}
		}
	}
}

//go:generate complexity 8
func labelContinue(m, n int, s ...int) {
root:
	for i := 0; i < m; i++ { //+1
		for j := i + 1; j < n; j++ { //+1 (nested +1)
			if s[i] == s[j] { //+1 (nested +2)
				continue root //+1
			} else if s[j] == 0 { //+1
				continue
			}
		}
	}
}
