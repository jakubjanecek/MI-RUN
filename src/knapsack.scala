package cz.cvut.fit.run.knapsack

/**
 * Knapsack class.
 * @document    Knapsack.scala
 * @author      Ondřej Drkula (drkulond@fit.cvut.cz)
 */
object Knapsack {

  def capacity = 100
  //def items = Array((18, 114), (42, 136), (88, 192), (3, 223))
  def items = Array((27, 38), (2, 86), (41, 112), (1, 0), (25, 66), (1, 97), 
                    (34, 195), (3, 85), (50, 42), (12, 223))
  
  val n = items.length
  
  /**
   * Converts decimal number to binary number (boolean array).
   */
  def numberToBinary(number : Int) : Array[Boolean] = {
    val binary = new Array[Boolean](n)
    if (number == 0) return binary
    var rest = false
    var div = number
    var i = 0
    while (div > 0) {
      rest = (div % 2 == 1)
      div = div / 2
      binary(n - i - 1) = rest
      i = i + 1
    }
    return binary
  }
  
  /**
   * Converts binary number (boolean array) to string.
   */
  def binaryToString(binary : Array[Boolean]) : String = {
    var string = ""
    for (i <- 0 to binary.length - 1) {
      if (binary(i)) {
        string = string + " 1" 
      } else {
        string = string + " 0" 
      }
    }
    return string
  }
  
  /**
   * Solves the knapsack problem using brute-force algorithm.
   */
  def solveKnapsackProblem() : Unit = {
    val p = scala.math.pow(2, n).toInt
    var bestCombination = new Array[Boolean](n)
    var maxWeight = 0
    var maxPrice = 0
    for (i <- 0 to p - 1) {
      var actualWeight = 0
      var actualPrice = 0
      val actualCombination = numberToBinary(i)
      for (j <- 0 to n - 1) {
        if (actualCombination(j)) {
          val (weight, price) = items(j)
          if (actualWeight + weight < capacity) {
            actualWeight = actualWeight + weight
            actualPrice = actualPrice + price
          }
        }
      }
      if (actualPrice > maxPrice) {
        bestCombination = actualCombination
        maxWeight = actualWeight
        maxPrice = actualPrice
      }
    }
    val result = "kombinace:" + binaryToString(bestCombination) + 
      " | vaha: " + maxWeight + " | cena: " + maxPrice
    println(result)
  }
  
  /**
   * Main method (starts the program).
   */
  def main(args: Array[String]): Unit = {
    solveKnapsackProblem()
  }
}
