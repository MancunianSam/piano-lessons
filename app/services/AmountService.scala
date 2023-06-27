package services

import com.google.inject.Inject
import services.AmountService.Prices

class AmountService @Inject() () {
  def getPrices(lessonLength: Int): Prices = {
    lessonLength match {
      case 0  => Prices(0, 0, 0)
      case 30 => Prices(1500, 4500, 7500)
      case 60 => Prices(3000, 9000, 17000)
    }
  }
  def calculateAmount(numOfLessons: Int, lessonLength: Int): Int = {
    getPrices(lessonLength).get(numOfLessons)
  }
}
object AmountService {
  case class Prices(oneLesson: Int, threeLessons: Int, sixLessons: Int) {
    def get(numberOfLessons: Int): Int = numberOfLessons match {
      case 1 => oneLesson
      case 3 => threeLessons
      case 6 => sixLessons
    }
  }
}
