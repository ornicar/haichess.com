// do NOT set mobile API headers here
// they trigger a compat layer
export function round(puzzleId, win, seconds, homework, lines) {
  let data = {
    win: win ? 1 : 0,
    seconds: seconds
  };
  if (homework) {
    data['homeworkId'] = homework.id;
  }
  lines.forEach(function (ln, i) {
    data['lines[' + i + '].uci'] = ln.uci;
    data['lines[' + i + '].san'] = ln.san;
    data['lines[' + i + '].fen'] = ln.fen;
  });
  return $.ajax({
    method: 'POST',
    url: '/training/' + puzzleId + '/round2',
    data: data
  });
}

export function vote(puzzleId, v) {
  return $.ajax({
    method: 'POST',
    url: '/training/' + puzzleId + '/vote',
    data: {
      vote: v ? 1 : 0
    }
  });
}
export function nextPuzzle() {
  return $.ajax({
    url: '/training/new'
  });
}

export function nextThemePuzzle(puzzleId) {
  return $.ajax({
    url: '/training/theme/' + puzzleId +  '/new' + location.search
  });
}

export function nextErrorPuzzle(puzzleId, puzzleErrors, isDelete) {
  let u_1 = replaceParamVal(location.search, 'rating', puzzleErrors.rating);
  let u_2 = replaceParamVal(u_1, 'time', puzzleErrors.time);
  return $.ajax({
    url: '/training/errors/' + puzzleId + '/new' + u_2 + (isDelete ? '&d=' + isDelete : '')
  });
}

export function nextCapsulePuzzle(capsuleId, puzzleId) {
  return $.ajax({
    url: '/training/capsule/' + capsuleId +  '/new?lastPlayed=' + puzzleId
  });
}

export function nextHomeworkPuzzle(homeworkId, puzzleId) {
  return $.ajax({
    url: '/training/homework/' + homeworkId +  '/new?lastPlayed=' + puzzleId
  });
}

export function like(puzzleId, v) {
  return $.ajax({
    method: 'POST',
    url: '/training/' + puzzleId + '/like',
    data: {
      like: v
    }
  });
}
export function setTag(puzzleId, v) {
  return $.ajax({
    method: 'POST',
    url: '/training/' + puzzleId + '/setTag',
    data: {
      tags: v
    }
  });
}

export function isPuzzleContinue() {
  return $.ajax({
    url: '/training/continue'
  });
}

export function isThemePuzzleContinue() {
  return $.ajax({
    url: '/training/theme/continue'
  });
}


function replaceParamVal(url, paramName, replaceWith) {
  let regx = eval('/(' + paramName + '=)([^&]*)/gi');
  let newUrl = url.replace(regx, paramName + '=' + replaceWith);
  return newUrl;
}