const lilaGulp = require('../gulp/tsProject.js');

lilaGulp('LichessPuzzleRush', 'lichess.puzzleRush', __dirname);
require('../gulp/cssProject.js')(__dirname);
