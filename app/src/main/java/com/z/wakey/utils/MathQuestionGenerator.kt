package com.z.wakey.utils

import kotlin.random.Random

data class MathQuestion(
    val operand1: Int,
    val operand2: Int,
    val operator: Char,
    val answer: Int,
    val display: String
)

object MathQuestionGenerator {

    fun generate(): MathQuestion {
        return if (Random.nextBoolean()) {
            val a = Random.nextInt(5, 60)
            val b = Random.nextInt(5, 60)
            MathQuestion(a, b, '+', a + b, "$a + $b")
        } else {
            val a = Random.nextInt(20, 99)
            val b = Random.nextInt(5, a)
            MathQuestion(a, b, '-', a - b, "$a − $b")
        }
    }

    fun generateBatch(count: Int): List<MathQuestion> = List(count) { generate() }
}
