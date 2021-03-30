package views.html.team

import play.api.data.Form
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.Region
import lila.team.{ Certification, Team, Tag, TeamSetting }
import controllers.routes

object forms {

  def create(form: Form[_], captcha: lila.common.Captcha)(implicit ctx: Context) =
    bits.layout(title = trans.newTeam.txt(), evenMoreJs = frag(captchaTag, provinceCascadeTag)) {
      main(cls := "page-menu page-small")(
        bits.menu("form".some),
        div(cls := "page-menu__content box box-pad")(
          h1(trans.newTeam()),
          if (ctx.me.??(_.cellphone.isEmpty)) a(href := routes.Account.cellphoneConfirm)("请先绑定手机")
          else {
            frag(
              p(cls := "is-gold", dataIcon := "")(nbsp, nbsp, "每账号同一时间仅可建立1个俱乐部"),
              postForm(cls := "form3", action := routes.Team.create())(
                form3.globalError(form),
                form3.group(form("name"), trans.name())(form3.input(_)),
                form3.group(form("open"), trans.joiningPolicy()) { f =>
                  form3.select(form("open"), Seq(0 -> trans.aConfirmationIsRequiredToJoin.txt(), 1 -> trans.anyoneCanJoin.txt()))
                },
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
                form3.group(form("description"), "简要介绍")(form3.textarea(_)(rows := 10)),
                views.html.base.captcha(form, captcha),
                div(cls := "form-group")(
                  p(cls := "is-gold", dataIcon := "")(
                    nbsp, nbsp,
                    "您将建立一个棋友之间互助、交流、学习的团队。需要您注意的是：作为俱乐部的管理员，您可以引导成员进行热烈、友好的互动，但如果出现违规信息，您和涉及的成员都会受到相应的处罚。请及时检查、清理互动区的信息，如果发现无法处理的情况，请联系客服。祝您和俱乐部成员能充分利用平台提供的功能，提升学习效率，享受国际象棋的乐趣！"
                  )
                ),
                form3.actions(
                  a(href := routes.Team.home(1))(trans.cancel()),
                  form3.submit(trans.newTeam())
                )
              )
            )
          }
        )
      )
    }

  def setting(t: Team, tags: List[Tag], form: Form[_])(implicit ctx: Context) = {
    bits.layout(
      title = t.name + " 设置",
      evenMoreJs = frag(jsTag("team.setting.js"))
    ) {
        main(cls := "page-menu page-small")(
          bits.menu(none),
          div(cls := "page-menu__content box box-pad setting")(
            h1(a(href := routes.Team.show(t.id))(t.name), nbsp, em("设置")),
            div(cls := "setting-actions")(
              /*a(cls := "button button-empty", href := routes.Team.kick(t.id))("踢人"),*/
              !t.certified option a(cls := "button button-empty", href := routes.Team.changeOwner(t.id))("转移管理员"),
              (isGranted(_.ManageTeam) || ctx.me ?? (u => t.isCreator(u.id))) && t.enabled option frag(
                postForm(cls := "inline", action := routes.Team.disable(t.id))(
                  submitButton(dataIcon := "q", st.title := "关闭后将不可恢复", cls := "text button button-empty button-red confirm")("关闭俱乐部")
                )
              )
            ),
            fieldset(cls := "setting-tag")(
              div(cls := "tag-actions")(
                h2("标签设置"),
                a(cls := "button button-empty tag-add", href := routes.Team.addTagModal(t.id))("添加标签")
              ),
              table(cls := "slist")(
                thead(
                  tr(
                    th("类型"),
                    th("名称"),
                    th("可选值"),
                    th("操作")
                  )
                ),
                tbody(
                  tags.map { tag =>
                    tr(
                      td(tag.typ.name),
                      td(tag.label),
                      td(style := "max-width: 260px;")(tag.value.map { span(_) }.getOrElse(frag("-"))),
                      td(
                        tag.editable option a(cls := "button button-empty tag-edit", href := routes.Team.editTagModal(tag._id))("编辑"),
                        tag.editable option postForm(cls := "inline", action := routes.Team.removeTag(t.id, tag._id))(
                          submitButton(st.title := "删除后成员对应的标签也将删除，是否继续", cls := "button button-empty button-red confirm")("删除")
                        )
                      )
                    )
                  }
                )
              )
            ),
            postForm(cls := "form3", action := routes.Team.settingApply(t.id))(
              form3.group(form("open"), trans.joiningPolicy()) { f =>
                form3.select(f, Seq(0 -> trans.aConfirmationIsRequiredToJoin.txt(), 1 -> trans.anyoneCanJoin.txt()))
              },
              form3.group(form("tagTip"), "接受邀请时补录信息") { f =>
                form3.select(f, Seq(0 -> "否", 1 -> "是"))
              },
              form3.split(
                form3.checkbox(form("ratingSetting.open"), raw("开启俱乐部内部等级分"), half = true, help = frag("点击查看", nbsp, a(target := "_blank", href := routes.Team.ratingRule)("计分规则")).some),
                form3.checkbox(form("ratingSetting.coachSupport"), raw("允许教练创建班级比赛（含线上和线下），计算俱乐部等级分"), half = true)
              ),
              form3.split(
                form3.group(form("ratingSetting.k"), raw("发展系数K"), half = true, help = raw("标识棋手的稳定性").some)(f => form3.select(f, TeamSetting.kList)),
                form3.group(form("ratingSetting.defaultRating"), raw("默认初始等级分"), half = true)(form3.input(_, typ = "number"))
              ),
              h3(b("满足以下条件的线上对局（俱乐部内会员间），自动计算内部等级分")),
              form3.split(
                form3.group(form("ratingSetting.turns"), raw("回合数大于"), half = true)(f => frag(form3.input(f, typ = "number"))),
                form3.group(form("ratingSetting.minutes"), raw("单个棋手用时大于等于（分钟）"), half = true)(f => frag(form3.input(f, typ = "number")))
              ),
              form3.actions(
                a(href := routes.Team.show(t.id), style := "margin-left:20px")(trans.cancel()),
                form3.submit(trans.apply())
              )
            )
          )
        )
      }
  }

  def addTag(teamId: String, form: Form[_])(implicit ctx: Context) = frag(
    div(cls := "modal-content none")(
      h2("添加标签"),
      postForm(cls := "form3 tag", style := "text-align:left;", action := routes.Team.addTagApply(teamId))(
        form3.group(form("typ"), raw("类型"))(form3.select(_, Tag.Type.list)),
        form3.group(form("label"), raw("名称"))(form3.input(_)(required)),
        form3.group(form("value"), raw("可选值"), help = raw("多个值时使用,分隔").some, klass = "none")(form3.textarea(_)()),
        form3.globalError(form),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交")
        )
      )
    )
  )

  def editTag(tag: Tag, form: Form[_])(implicit ctx: Context) = frag(
    div(cls := "modal-content none")(
      h2("编辑标签"),
      postForm(cls := "form3 tag", style := "text-align:left;", action := routes.Team.editTagApply(tag._id))(
        form3.group(form("typ"), raw("类型"))(f => frag(
          "：",
          tag.typ.name,
          form3.hidden(f)
        )),
        form3.group(form("label"), raw("名称"))(form3.input(_)(required)),
        tag.typ.hasValue option form3.group(form("value"), raw("可选值"), help = raw("多个值时使用,分隔").some)(form3.textarea(_)()),
        form3.globalError(form),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交")
        )
      )
    )
  )

  def edit(t: Team, form: Form[_])(implicit ctx: Context) = {
    bits.layout(title = (t.name + " 资料"), evenMoreJs = frag(
      provinceCascadeTag,
      singleUploaderTag,
      multiUploaderTag,
      jsTag("team.edit.js")
    )) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(a(href := routes.Team.show(t.id))(t.name), nbsp, em("资料")),
          p("编号", "：", t.id),
          postForm(cls := "form3 edit", action := routes.Team.update(t.id))(
            div(cls := "top")(
              form3.group(form("logo"), raw("Logo"), klass = "logo")(form3.singleImage(_, "上传LOGO"))
            ),
            t.certified option form3.hidden(form("name")),
            !t.certified option form3.group(form("name"), "名称", help = frag("认证后将不可修改").some)(form3.input(_)),
            t.certified option form3.split(
              form3.group(form(""), "省份：", half = true) { _ =>
                frag(
                  form3.hidden(form("province")),
                  strong(t.provinceName)
                )
              },
              form3.group(form(""), "城市：", half = true) { _ =>
                frag(
                  form3.hidden(form("city")),
                  strong(t.cityName)
                )
              }
            ),
            !t.certified option form3.split(
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
            form3.group(form("description"), "简要介绍")(form3.textarea(_)(rows := 10)),
            form3.group(form("envPicture"), raw("俱乐部环境（最多上传5张，宽度大于750像素，3:2比例显示最佳）"))(_ => form3.multiImage(form, "envPicture", 5, "点击上传")),
            form3.actions(
              a(href := routes.Team.show(t.id), style := "margin-left:20px")(trans.cancel()),
              form3.submit(trans.apply())
            )
          )
        )
      )
    }
  }

  def certification(t: Team, form: Form[_])(implicit ctx: Context) = {
    val cert = t.certification
    bits.layout(title = (t.name + " 认证"), evenMoreJs = frag(
      smsCaptchaTag,
      singleUploaderTag,
      jsTag("team.certification.js")
    )) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(a(href := routes.Team.show(t.id))(t.name), nbsp, em("认证")),
          postForm(cls := "form3", dataSmsrv := 0, action := routes.TeamCertification.certificationSend(t.id))(
            form3.group(form("addr"), "详细地址")(form3.input(_)),
            form3.group(form("members"), "俱乐部人数")(form3.input(_)),
            form3.group(form("org"), "注册单位名称")(form3.input(_)),
            form3.group(form("businessLicense"), "营业执照")(form3.singleImage(_)),
            form3.group(form("leader"), "负责人")(form3.input(_)),
            views.html.base.smsCaptcha(form, "手机号码", showCode = editable(cert)),
            form3.group(form("message"), "补充信息")(form3.textarea(_)(rows := 6)),
            form3.action(form3.submit(submitName(cert), klass = if (submitDisabled(cert)) "disabled" else "", isDisable = submitDisabled(cert)))
          )
        )
      )
    }
  }

  def editable(cert: Option[Certification]): Boolean = cert match {
    case None => true
    case Some(c) => c.status match {
      case Certification.Status.Applying => false
      case Certification.Status.Approved => false
      case Certification.Status.Rejected => true
    }
  }

  def submitName(cert: Option[Certification]): String = cert match {
    case None => "提交审核"
    case Some(c) => c.status match {
      case Certification.Status.Applying => "审核中"
      case Certification.Status.Approved => "已通过"
      case Certification.Status.Rejected => "提交审核"
    }
  }

  def submitDisabled(cert: Option[Certification]): Boolean = cert match {
    case None => false
    case Some(c) => c.status match {
      case Certification.Status.Applying => true
      case Certification.Status.Approved => true
      case Certification.Status.Rejected => false
    }
  }

}
