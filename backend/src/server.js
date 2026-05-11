const app = require("./app");
const env = require("./config/env");
const os = require("os");
const { initializeSocketServer } = require("./socket");

// Get the actual IP address
function getLocalIpAddress() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === "IPv4" && !iface.internal) {
        return iface.address;
      }
    }
  }
  return "localhost";
}

const localIp = getLocalIpAddress();

const httpServer = initializeSocketServer(app);

httpServer.listen(env.port, "0.0.0.0", () => {
  console.log(`Movify backend listening on http://localhost:${env.port}`);
  if (localIp !== "localhost") {
    console.log(`Also accessible at http://${localIp}:${env.port}`);
  }
});
