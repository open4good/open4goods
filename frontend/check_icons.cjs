const mdi = require('@mdi/js')
const keys = Object.keys(mdi)
const arrows = keys.filter(k => k.toLowerCase().includes('arrowtopright'))
console.log(arrows.join('\n'))
