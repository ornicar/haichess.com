package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.Region
import controllers.routes

object profile {

  def apply(u: lila.user.User, referrer: Option[String], form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = s"${u.username} - ${trans.editProfile.txt()}",
    active = "editProfile",
    evenMoreCss = frag(
      cssTag("profile.level")
    ),
    evenMoreJs = frag(
      flatpickrTag,
      delayFlatpickrStart,
      singleUploaderTag,
      provinceCascadeTag,
      jsTag("profile.level.js")
    )
  ) {
      div(cls := "account box box-pad")(
        h1(trans.editProfile()),
        postForm(cls := "form3", action := routes.Account.profileApply(referrer))(
          form3.split(
            form3.groupNoLabel(form("head"), klass = "head")(form3.singleImage(_, "上传头像")),
            div(cls := "form-group")
          ),
          form3.split(
            form3.group(form("realName"), "姓名", half = true)(form3.input(_)),
            form3.group(form("level"), "当前级别", half = true) { _ =>
              div(
                a(cls := "level", href := routes.Account.levels(referrer), title := "编辑")(h2(u.profileOrDefault.currentLevel.label))
              )
            }
          ),
          form3.split(
            form3.group(form("sex"), "性别", half = true) { f =>
              form3.select(f, lila.user.FormSelect.Sex.list, default = "".some)
            },
            form3.group(form("birthyear"), "出生年份", half = true)(form3.input(_, typ = "number"))
          ),
          form3.split(
            form3.group(form("province"), "省份", half = true) { f =>
              form3.select(f, Region.Province.provinces, default = "".some)
            },
            form3.group(form("city"), "城市", half = true) { f =>
              val empty = form3.select(f, List.empty, default = "".some)
              form("province").value.fold(empty) { v =>
                form3.select(f, Region.City.citys(v), default = "".some)
              }
            }
          ),
          form3.split(
            /*form3.group(form("mobile"), "手机号", half = true)(form3.input(_)),*/
            form3.group(form("wechat"), "微信号", half = true)(form3.input(_))
          ),
          NotForKids {
            form3.group(form("bio"), trans.biography(), help = trans.biographyDescription().some) { f =>
              form3.textarea(f)(rows := 5)
            }
          },
          /*
        form3.group(form("links"), raw("社交链接 "), help = Some(linksHelp)) { f =>
          form3.textarea(f)(rows := 5)
        },*/
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
}
