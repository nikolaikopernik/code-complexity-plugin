package main

//go:generate complexity 7
func complex(
	a, b, c, d, e, f, g, h, i, j bool,
) bool {
	if        // +1
	a || b && // +1
		c || d && // +1
		e || f && // +1
		g || h && // +1
		i || j { // +1
		return true
	} else { // +1
		return false
	}
}

//go:generate complexity 4
func simpleStatements(b bool, v int) int {
	if b { // +1
		return 1
	}
	if v < 0 { // +1
		return 2
	}
	if v%3 == 0 && // +1
		v%5 == 0 { // +1
		return 15
	}
	return v
}

//go:generate complexity 3
func twoGroups(a, b, c, d bool) bool {
	if        // +1
	a || b || // +1 OR
		!(c || d) { // +1 OR separate
		return true
	}
	return false
}

type Aa struct {
	a, b, c, d bool
}

//go:generate complexity 3
func (a Aa) parenthesisCreateNewGroupAnyway() bool {
	if            //+1 if
	a.a || a.b || //+1 OR
		(a.c || a.d) { // +1 OR group
		return true
	}
	return false
}

//go:generate complexity 4
func (a Aa) parenthesisInCenterSplitTheGroup(e, f bool) bool {
	if            //+1 if
	a.a || a.b || //+1 OR
		!(a.c || // +1 OR group
			a.d) || e || f { // +1 new OR
		return true
	}
	return false
}

func (a Aa) call() bool {
	return true
}

//go:generate complexity 1
func (a Aa) doesSupportOperation() bool {
	return a.a && a.call()
}

//go:generate complexity 2
func (a Aa) doesSupportOperationInStatement() bool {
	r := a.a && a.call() //+1
	r = r && a.call()    //+1
	return r
}

type MayErr interface {
	runAction() error
	getMap() map[string]string
	consumeSomething(v string)
}

//go:generate complexity 1
func goErrorIfStatement(m MayErr) error {
	if err := m.runAction(); err != nil { // +1
		return err
	}
	return nil
}

//go:generate complexity 1
func goSpecificIfStatement(k string, m MayErr) {
	f := m.getMap()
	if v, ok := f[k]; ok { // +1
		m.consumeSomething(v)
	}
}
