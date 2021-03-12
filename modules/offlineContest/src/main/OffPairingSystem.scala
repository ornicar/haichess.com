package lila.offlineContest

import java.io.{ File, PrintWriter }
import scala.sys.process._
import scala.concurrent.blocking

final class OffPairingSystem(trf: OffSwissTrf) {

  import OffPairingSystem._

  val sep = System.getProperty("line.separator")

  def apply(contest: OffContest): Fu[List[ByeOrPending]] =
    trf(contest).map { trfContent =>
      println(s"==============TrfContent $contest 第${contest.currentRound}轮 ================")
      println(trfContent)
      val pairResult = invoke(trfContent)
      println(s"==============PairResult $contest 第${contest.currentRound}轮 ================")
      println(pairResult.mkString(sep))
      pairResult
        .drop(1) // first line is the number of pairings
        .map(_ split ' ')
        .collect {
          case Array(p, "0") => Left(Bye(p.toInt))
          case Array(w, b) => Right(Pending(w.toInt, b.toInt))
        }
    }

  private def invoke(trfContent: String): List[String] =
    withTempFile(trfContent) { file =>
      val command = s"java -jar ./bin/javafo.jar ${file.getAbsolutePath}  -p"
      val stdout = new collection.mutable.ListBuffer[String]
      val stderr = new StringBuilder
      val status = blocking {
        command ! ProcessLogger(stdout append _, stderr append _)
      }
      if (status != 0) {
        val error = stderr.toString
        if (error contains "No valid pairing exists") Nil
        else throw OffPairingSystem.BBPairingException(error, trfContent)
      } else if (stdout.headOption.??(_.contains("Exception"))) {
        throw OffPairingSystem.BBPairingException(stdout.toList.mkString(sep), trfContent)
      } else stdout.toList
    }

  def withTempFile[A](content: String)(f: File => A): A = {
    val file = File.createTempFile("haichess-", "-offlineContest")
    val writer = new PrintWriter(file)
    writer.print(content)
    writer.flush()
    writer.close()
    val res = f(file)
    file.delete()
    res
  }
}

object OffPairingSystem {

  type ByeOrPending = Either[Bye, Pending]

  case class Pending(
      white: OffPlayer.No,
      black: OffPlayer.No
  )
  case class Bye(player: OffPlayer.No)

  case class BBPairingException(val message: String, val trfContent: String) extends lila.base.LilaException
}
