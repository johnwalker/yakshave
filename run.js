try {
    require("source-map-support").install();
} catch(err) {
}
require("./out/goog/bootstrap/nodejs.js");
require("./out/io/johnwalker/yakshave.js");
require("./out/io/johnwalker/yakshave/core.js");
goog.require("io.johnwalker.yakshave.core");
goog.require("cljs.nodejscli");
