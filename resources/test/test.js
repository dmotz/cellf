var system = require('system');
var page   = require('webpage').create();
var url    = system.args[1];

page.onConsoleMessage = function(s) {
    console.log(s);
};

page.open(url, function() {
    page.evaluate(function(){
        cellf.test.run();
    });
    phantom.exit(0);
});
