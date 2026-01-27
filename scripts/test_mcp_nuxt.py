import requests
import sseclient
import json
import threading
import time

def test_nuxt_mcp():
    url = "http://localhost:3000/__mcp/sse"
    print(f"Connecting to SSE: {url}")
    
    response = requests.get(url, stream=True)
    client = sseclient.SSEClient(response)
    
    session_id = None
    endpoint = None
    
    for event in client.events():
        print(f"Received event: {event.event}")
        if event.event == 'endpoint':
            endpoint = event.data
            # Extract session ID if needed, or just use the endpoint provided
            print(f"Endpoint: {endpoint}")
            
            # Now we can send the initialize request
            full_post_url = f"http://localhost:3000{endpoint}"
            print(f"POST URL: {full_post_url}")
            
            # 1. Initialize
            init_payload = {
                "jsonrpc": "2.0",
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {"name": "manual-test", "version": "1.0"}
                },
                "id": 1
            }
            res = requests.post(full_post_url, json=init_payload)
            print(f"Initialize Response: {res.status_code} - {res.text}")
            
            # 2. List Tools
            tools_payload = {
                "jsonrpc": "2.0",
                "method": "tools/list",
                "params": {},
                "id": 2
            }
            res = requests.post(full_post_url, json=tools_payload)
            print(f"Tools Response: {res.status_code} - {res.text}")
            
            break # Success, we can stop

if __name__ == "__main__":
    test_nuxt_mcp()
