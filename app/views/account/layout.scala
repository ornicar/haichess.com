package views.html.account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object layout {

  def apply(
    title: String,
    active: String,
    evenMoreCss: Frag = emptyFrag,
    evenMoreJs: Frag = emptyFrag
  )(body: Frag)(implicit ctx: Context): Frag = views.html.base.layout(
    title = title,
    moreCss = frag(cssTag("account"), evenMoreCss),
    moreJs = frag(jsTag("account.js"), evenMoreJs)
  ) {
      def activeCls(c: String) = cls := active.activeO(c)
      main(cls := "account page-menu")(
        st.nav(cls := "page-menu__menu subnav")(
          lila.pref.PrefCateg.all.map { categ =>
            a(activeCls(categ.slug), href := routes.Pref.form(categ.slug))(
              bits.categName(categ)
            )
          },
          a(activeCls("kid"), href := routes.Account.kid())(
            trans.kidMode()
          ),
          div(cls := "sep"),
          a(activeCls("editProfile"), href := routes.Account.profile(None))(
            trans.editProfile()
          ),
          a(activeCls("member"), href := routes.Member.info)("会员信息"),
          a(activeCls("coach-certify"), href := routes.Coach.certify)("教练认证"),
          isGranted(_.Coach) option a(activeCls("coach-profile"), href := routes.Coach.edit)("教练资料"),
          div(cls := "sep"),
          a(activeCls("password"), href := routes.Account.passwd())(
            trans.changePassword()
          ),
          a(activeCls("email"), href := routes.Account.email())(
            trans.changeEmail()
          ),
          a(activeCls("username"), href := routes.Account.username())(
            trans.changeUsername()
          ),
          a(activeCls("cellphoneConfirm"), href := routes.Account.cellphoneConfirm())("手机绑定"),
          /*          a(activeCls("twofactor"), href := routes.Account.twoFactor())(
            "Two-factor authentication"
          ),*/
          a(activeCls("security"), href := routes.Account.security())(
            trans.security()
          ),

          /*
          div(cls := "sep"),
          a(href := routes.Plan.index)("Patron"),
          div(cls := "sep"),
          a(activeCls("oauth.token"), href := routes.OAuthToken.index)(
            "API Access tokens"
          ),
          ctx.noBot option a(activeCls("oauth.app"), href := routes.OAuthApp.index)("OAuth Apps"),
          */
          div(cls := "sep"),
          a(activeCls("close"), href := routes.Account.close())(
            trans.closeAccount()
          )
        ),
        div(cls := "page-menu__content")(body)
      )
    }
}
