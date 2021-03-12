const lilaGulp = require('../gulp/tsProject.js');

lilaGulp('LichessRecall', 'lichess.recall', __dirname);
require('../gulp/cssProject.js')(__dirname);
