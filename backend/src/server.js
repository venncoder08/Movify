const app = require("./app");
const env = require("./config/env");

app.listen(env.port, "0.0.0.0", () => {
  console.log(`Movify backend listening on http://0.0.0.0:${env.port}`);
});
