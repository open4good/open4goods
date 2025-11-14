import { setHeader } from 'h3'

export default defineEventHandler((event) => {
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
        button {
          font-size: 1rem;
          border: none;
          border-radius: 999px;
          padding: 0.75rem 1.5rem;
          background: #00de9f;
          color: #052320;
          font-weight: 600;
          cursor: pointer;
          box-shadow: 0 10px 35px rgba(0, 222, 159, 0.35);
        }
        button:focus-visible {
          outline: 3px solid rgba(0, 222, 159, 0.5);
          outline-offset: 2px;
        }
        button:active {
          transform: translateY(1px);
        }
      </style>
    </head>
    <body>
      <div class="card">
        <h1>You are offline</h1>
        <p id="offline-status">The latest comparison you opened stays available, but live prices need a connection. Please reconnect and refresh Nudger.</p>
        <p>
          <button id="retry-button" type="button">Retry now</button>
        </p>
      </div>
      <script>
        (() => {
          const statusEl = document.getElementById('offline-status')
          const retryButton = document.getElementById('retry-button')
          const reload = () => window.location.reload()

          const showStatus = (message) => {
            if (statusEl) {
              statusEl.textContent = message
            }
          }

          if (navigator.onLine) {
            showStatus('Connection restored. Reloading…')
            reload()
            return
          }

          window.addEventListener('online', () => {
            showStatus('Connection restored. Reloading…')
            setTimeout(reload, 300)
          })

          window.addEventListener('offline', () => {
            showStatus('Still offline. We will reload as soon as the connection is back.')
          })

          retryButton?.addEventListener('click', () => {
            showStatus('Checking your connection…')
            reload()
          })
        })()
      </script>
    </body>
  </html>`
})
