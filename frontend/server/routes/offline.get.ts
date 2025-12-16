import { setHeader } from 'h3'

export default defineEventHandler(event => {
  setHeader(event, 'Content-Type', 'text/html; charset=UTF-8')

  return `<!doctype html>
  <html lang="en">
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <title>Nudger – Offline</title>
      <style>
        body {
          font-family: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
          margin: 0;
          min-height: 100vh;
          background: #f3fffb;
          color: #0f172a;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 24px;
          text-align: center;
        }
        .card {
          max-width: 540px;
          border-radius: 24px;
          padding: 32px;
          background: white;
          box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
          border: 1px solid rgba(0, 222, 159, 0.2);
        }
        h1 {
          font-size: 1.8rem;
          margin-bottom: 0.5rem;
        }
        p {
          margin: 0.65rem 0;
          line-height: 1.5;
        }
      </style>
    </head>
    <body>
      <div class="card">
        <h1>You are offline</h1>
        <p>The latest comparison you opened stays available, but live prices need a connection. Please reconnect and refresh Nudger.</p>
      </div>
    </body>
  </html>`
})
