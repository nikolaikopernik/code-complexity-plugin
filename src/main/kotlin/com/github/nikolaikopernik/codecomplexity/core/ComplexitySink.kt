package com.github.nikolaikopernik.codecomplexity.core

class ComplexitySink {
    private var nesting: Int = 0
    private val points = mutableListOf<ComplexityPoint>()
    fun decreaseNesting() {
        if (nesting > 0) nesting--
    }

    fun increaseNesting() {
        nesting++
    }

    fun increaseComplexity(type: PointType) {
        increaseComplexity(1, type)
    }

    fun increaseComplexity(amount: Int, type: PointType) {
        points.add(
            ComplexityPoint(
                complexity = 1,
                nesting = nesting,
                type = type
            )
        )
    }

    fun increaseComplexityAndNesting(type: PointType) {
        points.add(
            ComplexityPoint(
                complexity = 1 + nesting,
                nesting = nesting++,
                type = type
            )
        )
    }

    fun getComplexity(): Int {
        return points.sumOf { it.complexity }
    }

    fun getNesting(): Int {
        return nesting
    }

    fun getPoints() = points.toList()
}
