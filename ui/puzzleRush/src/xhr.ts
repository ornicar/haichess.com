// do NOT set mobile API headers here
// they trigger a compat layer
export function start(mode, condition) {
  return $.ajax({
    method: 'POST',
    url: '/training/rush/start?mode=' + mode,
    data: condition
  }).fail(function(res) {
    if(res.status === 406) {
      window.lichess.memberIntro();
    } else {
      handleError(res)
    }
  });
}

export function begin(rushId) {
  return $.ajax({
    method: 'POST',
    url: '/training/rush/' + rushId + '/begin'
  }).fail(function(res) {
    handleError(res);
  });
}

export function finish(rushId, status) {
  return $.ajax({
    method: 'POST',
    url: '/training/rush/' + rushId + '/finish',
    data: {
      status: status
    }
  }).fail(function(res) {
    handleError(res);
  });
}

export function sendResult(rushId, puzzleId, win, seconds, lines, timeout) {
  let data = {
    win: win ? 1 : 0,
    seconds: seconds
  };
  if(timeout) {
    data['timeout'] = timeout;
  }
  if(!timeout) {
    lines.forEach(function (ln, i) {
      data['lines[' + i + '].uci'] = ln.uci;
      data['lines[' + i + '].san'] = ln.san;
      data['lines[' + i + '].fen'] = ln.fen;
    });
  }

  return $.ajax({
    method: 'POST',
    url: '/training/rush/' + rushId + '/' + puzzleId + '/round',
    data: data
  }).fail(function(res) {
    handleError(res);
  });
}

export function nextPuzzle(rushId, last, condition) {
  return $.ajax({
    url: '/training/rush/' + rushId + (last ? '/last' : '/next'),
    data: condition
  }).fail(function(res) {
    handleError(res);
  });
}

export function loadRank(mode, rankScope, rankRange) {
  return $.ajax({
    url: '/training/rush/rank?mode=' + mode + '&scope=' + rankScope + '&range=' + rankRange
  }).fail(function(res) {
    handleError(res);
  });
}

function handleError(res) {
  let json = res.responseJSON;
  if (json) {
    if (json.error) {
      if(typeof json.error === 'string') {
        alert(json.error);
      } else alert(JSON.stringify(json.error));
    } else alert(res.responseText);
  } else {
    if(res.responseText) {
      alert(res.responseText);
    } else {
      alert('发生错误');
    }
  }
}