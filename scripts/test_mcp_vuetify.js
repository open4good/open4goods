import { spawn } from "child_process";
import path from "path";

const vuetifyMcpPath =
  "/home/goulven/git/open4goods/frontend/node_modules/@vuetify/mcp/dist/index.js";

console.log(`Spawning Vuetify MCP: ${vuetifyMcpPath}`);

const child = spawn("node", [vuetifyMcpPath], {
  stdio: ["pipe", "pipe", "inherit"],
});

child.stdout.on("data", (data) => {
  const msg = data.toString();
  console.log(`[Vuetify] Received: ${msg}`);

  try {
    const json = JSON.parse(msg);
    if (json.id === 1) {
      console.log("✅ Vuetify MCP Initialize success!");
      process.exit(0);
    }
  } catch (e) {
    // Ignore non-json (welcome messages etc)
  }
});

const initMsg = {
  jsonrpc: "2.0",
  method: "initialize",
  params: {
    protocolVersion: "2024-11-05",
    capabilities: {},
    clientInfo: { name: "manual-test", version: "1.0" },
  },
  id: 1,
};

console.log("[Vuetify] Sending initialize...");
child.stdin.write(JSON.stringify(initMsg) + "\n");
