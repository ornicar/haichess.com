package base

import lila.Lilaisms
import ornicar.scalalib._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

case class Cls(v: String)

object PimpedTest extends Lilaisms {

  def f3: Future[Unit] = {
    Thread.sleep(2000)
    println("f3")
    funit
  }

  def main(args: Array[String]): Unit = {

    Future {
      Thread.sleep(2000)
      println("f1")
      new Cls("class 1")
    } >> {
      Future {
        Thread.sleep(2000)
        println("f2")
        new Cls("class 2")
      }
    } >>- f3 onComplete {
      case Success(cls) => println("complete")
      case _ => println("some Exception")
    }
    println("start")
    Thread.sleep(7000)

    /*    val f = Future {

          5
        } andThen {
          case r => sys.error("runtime exception")
        } onComplete {
          case Failure(t) => println(t)
          case Success(v) => println(v)
        }

        println("I am working")
        Thread.sleep(3000)*/
  }

}
