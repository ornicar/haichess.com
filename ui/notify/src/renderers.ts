import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Notification, Renderers } from './interfaces'

// function generic(n: Notification, url: string | undefined, icon: string, content: VNode[]): VNode {
export const renderers: Renderers = {
  genericLink: {
    html: n => generic(n, n.content.url, n.content.icon, [
      h('span', [
        h('strong', n.content.title),
        drawTime(n)
      ]),
      h('span', n.content.text)
    ]),
    text: n => n.content.title || n.content.text
  },
  mention: {
    html: n => generic(n, "/forum/redirect/post/" + n.content.postId, 'd', [
      h('span', [
        h('strong', userFullName(n.content.mentionedBy)),
        drawTime(n)
      ]),
      h('span', ' 在 « ' + n.content.topic + ' » 中提到你')
    ]),
    text: n => userFullName(n.content.mentionedBy) + ' 在 « ' + n.content.topic + ' » 中提到你'
  },
  invitedStudy: {
    html: n => generic(n, "/study/" + n.content.studyId, '4', [
      h('span', [
        h('strong', userFullName(n.content.invitedBy)),
        drawTime(n)
      ]),
      h('span', ' 邀请您加入 « ' + n.content.studyName + ' ».')
    ]),
    text: n => userFullName(n.content.invitedBy) + ' 邀请您加入 « ' + n.content.studyName + ' ».'
  },
  privateMessage: {
    html: n => generic(n, "/inbox/" + n.content.thread.id + '#bottom', 'c', [
      h('span', [
        h('strong', userFullName(n.content.sender)),
        drawTime(n)
      ]),
      h('span', n.content.text)
    ]),
    text: n => userFullName(n.content.sender) + ': ' + n.content.text
  },
  teamJoined: {
    html: n => generic(n, "/team/" + n.content.id, 'f', [
      h('span', [
        h('strong', n.content.name),
        drawTime(n)
      ]),
      h('span', "您现在是俱乐部的一员了")
    ]),
    text: n => "您已加入 « " + n.content.name + "  »"
  },
  teamMadeOwner: {
    html: n => generic(n, "/team/" + n.content.id, 'f', [
      h('span', [
        h('strong', n.content.name),
        drawTime(n)
      ]),
      h('span', "您被指定为俱乐部的管理员")
    ]),
    text: n => "您现在是 « " + n.content.name + "  » 的管理员了"
  },
  u: {
    html: n => generic(n, '/tournament/limited-invitation', 'g', [
      h('span', [
        h('strong', '等级限制锦标赛'),
        drawTime(n)
      ]),
      h('span', '你一定能赢！')
    ]),
    text: n => 'Game with ' + n.content.opponentName + '.'
  },
  titledTourney: {
    html: n => generic(n, '/tournament/' + n.content.id, 'g', [
      h('span', [
        h('strong', 'Haichess 称号锦标赛'),
        drawTime(n)
      ]),
      h('span', n.content.text)
    ]),
    text: n => '与 ' + n.content.opponentName + ' 对局'
  },
  reportedBanned: {
    html: n => generic(n, undefined, '', [
      h('span', [
        h('strong', '您举报的人已经被封禁')
      ]),
      h('span', '谢谢您的支持！')
    ]),
    text: _ => '您举报的人已经被封禁'
  },
  gameEnd: {
    html: n => {
      let result;
      switch (n.content.win) {
        case true:
          result = '恭喜，你赢了！';
          break;
        case false:
          result = '你输了！';
          break;
        default:
          result = "平局";
      }
      return generic(n, "/" + n.content.id, ';', [
        h('span', [
          h('strong', '对局 vs ' + userFullName(n.content.opponent)),
          drawTime(n)
        ]),
        h('span', result)
      ]);
    },
    text: function(n) {
      let result;
      switch (n.content.win) {
        case true:
          result = '胜利';
          break;
        case false:
          result = '失败';
          break;
        default:
          result = '平局';
      }
      return result + ' vs ' + userFullName(n.content.opponent);
    }
  },
  planStart: {
    html: n => generic(n, '/patron', '', [
      h('span', [
        h('strong', 'Thank you!'),
        drawTime(n)
      ]),
      h('span', '您已经注册成为 Haichess 会员')
    ]),
    text: _ => '您已经注册成为 Haichess 会员'
  },
  planExpire: {
    html: n => generic(n, '/patron', '', [
      h('span', [
        h('strong', '会员已过期'),
        drawTime(n)
      ]),
      h('span', '请考虑续签！')
    ]),
    text: _ => '会员已过期'
  },
  coachReview: {
    html: n => generic(n, '/coach/edit', ':', [
      h('span', [
        h('strong', 'New pending review'),
        drawTime(n)
      ]),
      h('span', 'Someone reviewed your coach profile.')
    ]),
    text: _ => 'New pending review'
  },
  ratingRefund: {
    html: n => generic(n, '/player/myself', '', [
      h('span', [
        h('strong', 'You lost to a cheater'),
        drawTime(n)
      ]),
      h('span', 'Refund: ' + n.content.points + ' ' + n.content.perf + ' rating points.')
    ]),
    text: n => 'Refund: ' + n.content.points + ' ' + n.content.perf + ' rating points.'
  },
  corresAlarm: {
    html: n => generic(n, '/' + n.content.id, ';', [
      h('span', [
        h('strong', '时间快到了！'),
        drawTime(n)
      ]),
      h('span', '对局 vs ' + n.content.op)
    ]),
    text: _ => '时间快到了！'
  },
  irwinDone: {
    html: n => generic(n, '/@/' + n.content.user.name + '?mod', '', [
      h('span', [
        h('strong', userFullName(n.content.user)),
        drawTime(n)
      ]),
      h('span', 'Irwin job complete!')
    ]),
    text: n => n.content.user.name + ': Irwin job complete!'
  },
  coachApply: {
    html: n => generic(n, '/coach/mod/detail/' + n.content.user.id, '教', [
      h('span', [
        h('strong', '教练认证申请'),
        drawTime(n)
      ]),
      h('span', n.content.user.name + ': 申请教练认证')
    ]),
    text: n => n.content.user.name + ': 申请教练认证'
  },
  coachApproved: {
    html: n => generic(n, n.content.status == 'approved' ? '/coach/edit' : '/coach/certification', '教', [
      h('span', [
        h('strong', '教练认证' + (n.content.status == 'approved' ? '通过' : '失败')),
        drawTime(n)
      ]),
      h('span', '教练认证' + (n.content.status == 'approved' ? '通过' : '失败'))
    ]),
    text: n => '教练认证' + (n.content.status == 'approved' ? '通过' : '失败')
  },
  teamApply: {
    html: n => generic(n, '/team/mod/detail/' + n.content.teamId, 'f', [
      h('span', [
        h('strong', '俱乐部认证申请'),
        drawTime(n)
      ]),
      h('span', n.content.user.name + ': 申请俱乐部认证')
    ]),
    text: n => n.content.user.name + ': 申请俱乐部认证'
  },
  teamApproved: {
    html: n => generic(n, n.content.status == 'approved' ? ('/team/' + n.content.teamId) : ('/team/' + n.content.teamId + '/certification'), 'f', [
      h('span', [
        h('strong', '俱乐部认证' + (n.content.status == 'approved' ? '通过' : '失败')),
        drawTime(n)
      ]),
      h('span', '俱乐部认证' + (n.content.status == 'approved' ? '通过' : '失败'))
    ]),
    text: n => '俱乐部认证' + (n.content.status == 'approved' ? '通过' : '失败')
  },
  invitedClazz: {
    html: n => generic(n, "/student/accept/" + n.content.clazzId, '班', [
      h('span', [
        h('strong', userFullName(n.content.invitedBy)),
        drawTime(n)
      ]),
      h('span', ' 邀请您加入 « ' + n.content.clazzName + ' ».')
    ]),
    text: n => userFullName(n.content.invitedBy) + ' 邀请您加入 « ' + n.content.clazzName + ' ».'
  },
  inviteTeam: {
    html: n => generic(n, "/team/" + n.content.teamId, 'f', [
      h('span', [
        h('strong', userFullName(n.content.invitedBy)),
        drawTime(n)
      ]),
      h('span', '邀请您加入 « ' + n.content.teamName + ' ».')
    ]),
    text: n => userFullName(n.content.invitedBy) + ' 邀请您加入 « ' + n.content.teamName + ' ».'
  }
};

function generic(n: Notification, url: string | undefined, icon: string, content: VNode[]): VNode {
  return h(url ? 'a' : 'span', {
    class: {
      site_notification: true,
      [n.type]: true,
      'new': !n.read
    },
    attrs: url ? { href: url } : undefined
  }, [
    h('i', {
      attrs: { 'data-icon': icon }
    }),
    h('span.content', content)
  ]);
}

function drawTime(n: Notification) {
  var date = new Date(n.date);
  return h('time.timeago', {
    attrs: {
      title: date.toLocaleString(),
      datetime: n.date
    }
  }, window.lichess.timeago.format(date));
}

function userFullName(u?: LightUser) {
  if (!u) return 'Anonymous';
  return u.title ? u.title + ' ' + u.name : u.name;
}
