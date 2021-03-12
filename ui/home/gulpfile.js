const lilaGulp = require('../gulp/tsProject.js');

lilaGulp('LichessHome', 'lichess.home', __dirname);
require('../gulp/cssProject.js')(__dirname);
