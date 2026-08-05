const fs = require('fs');
const path = process.argv[2];
const content = process.argv[3];
fs.writeFileSync(path, content, { encoding: 'utf8' });
console.log('OK', path, content.length, 'chars');