const headers = {
  'Accept': 'application/vnd.lichess.v3+json'
};

export function nowPlaying() {
  return $.ajax({
    url: '/account/now-playing',
    headers: headers
  }).then(o => o.nowPlaying);
}

export function loadAppts() {
  return $.ajax({
    url: '/appt/mine',
    headers: headers
  }).fail(function(d) {
    alert(d.responseJSON.error)
  });
}

export function accept(id) {
  return $.ajax({
    method: 'post',
    url: `/appt/${id}/acceptXhr`,
    headers: headers
  });
}

export function decline(id: string) {
  return $.ajax({
    method: 'post',
    url: `/challenge/${id}/decline`,
    headers: headers
  });
}

export function cancel(id: string) {
  return $.ajax({
    method: 'post',
    url: `/challenge/${id}/cancel`,
    headers: headers
  });
}

export function loadWeekCalendars(week) {
  return $.ajax({
    url: `/calendar/week?offset=${week}`,
    headers: headers
  }).fail(function(d) {
    alert(d.responseJSON.error)
  });
}

export function loadDayCalendars(day) {
  return $.ajax({
    url: `/calendar/day?offset=${day}`,
    headers: headers
  }).fail(function(d) {
    alert(d.responseJSON.error)
  });
}
