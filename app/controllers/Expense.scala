package controllers

import views._
import lila.app._
import play.api.mvc._

object Expense extends LilaController {

  def resource = Open { implicit ctx =>
    Ok(html.expense.resource()).fuccess
  }

  def vip = Open { implicit ctx =>
    Ok(html.expense.vip()).fuccess
  }

}
