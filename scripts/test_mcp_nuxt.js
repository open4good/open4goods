async function testNuxtMcp() {
  const url = "http://localhost:3000/__mcp/sse";
  console.log(`Connecting to SSE: ${url}`);

  const controller = new AbortController();
  const response = await fetch(url, { signal: controller.signal });

  if (!response.ok) {
    console.error(
      `Failed to connect: ${response.status} ${response.statusText}`,
    );
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let initialized = false;

  // Process stream
  (async () => {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n\n");
      buffer = lines.pop();

      for (const block of lines) {
        const eventLine = block.match(/event: (.*)/);
        const dataLine = block.match(/data: (.*)/);

        if (eventLine && dataLine) {
          const event = eventLine[1].trim();
          const data = dataLine[1].trim();

          if (event === "endpoint") {
            console.log(`[SSE] Endpoint received: ${data}`);
            const postUrl = `http://localhost:3000${data}`;

            // Send Initialize
            console.log("[POST] Sending initialize...");
            await fetch(postUrl, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                jsonrpc: "2.0",
                method: "initialize",
                params: {
                  protocolVersion: "2024-11-05",
                  capabilities: {},
                  clientInfo: { name: "manual-test", version: "1.0" },
                },
                id: 1,
              }),
            });
          } else if (event === "message") {
            console.log(`[SSE] Message received: ${data}`);
            const msg = JSON.parse(data);

            if (msg.id === 1) {
              console.log("✅ Initialize success!");
              const postUrl = `http://localhost:3000/__mcp/messages?sessionId=${msg.sessionId || "unknown"}`;
              // Note: sessionId might not be in the msg, but was in endpoint.
              // Actually we should assume the postUrl is constant or use the one from endpoint.
              // But let's just trigger the tools list now.

              // Re-parse endpoint from log? No, let's just reuse the logic.
              // Simplified: Just waiting for ID 1 to confirm connection works.

              console.log("✅ Nuxt MCP is working!");
              process.exit(0);
            }
          }
        }
      }
    }
  })();
}

testNuxtMcp().catch(console.error);
