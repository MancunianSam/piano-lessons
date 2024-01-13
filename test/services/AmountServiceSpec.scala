package services

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2, TableFor3}
import org.scalatestplus.play.PlaySpec
import services.AmountService.Prices

class AmountServiceSpec extends PlaySpec with TableDrivenPropertyChecks {

  val priceTable: TableFor2[Int, Prices] = Table(
    ("lessonLength", "prices"),
    (0, Prices(0, 0, 0)),
    (30, Prices(1500, 4500, 7500)),
    (60, Prices(3000, 9000, 17000))
  )

  "AmountService getPrices" should {
    forAll(priceTable) { (lessonLength, prices) =>
      s"return the correct prices for a $lessonLength minute lesson" in {
        new AmountService().getPrices(lessonLength) must equal(prices)
      }
    }
  }

  val amountTable: TableFor3[Int, Int, Int] = Table(
    ("numOfLessons", "lessonLength", "amount"),
    (1, 0, 0),
    (3, 0, 0),
    (6, 0, 0),
    (1, 30, 1500),
    (3, 30, 4500),
    (6, 30, 7500),
    (1, 60, 3000),
    (3, 60, 9000),
    (6, 60, 17000)
  )

  "AmountService calculateAmount" should {
    forAll(amountTable) { (numOfLessons, lessonLength, amount) =>
      s"return the correct amount for $numOfLessons $lessonLength minute lessons" in {
        new AmountService().calculateAmount(numOfLessons, lessonLength) must equal(amount)
      }
    }
  }

}
