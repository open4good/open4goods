import type { EditorCleanup, InContextEditDetail } from './types'

type EditorModule = typeof import('./github-editor')

let editorModulePromise: Promise<EditorModule> | undefined
let activeCleanup: EditorCleanup

async function loadModule(): Promise<EditorModule> {
  editorModulePromise ??= import('./github-editor')
  return editorModulePromise
}

export async function openEditor(detail: InContextEditDetail): Promise<EditorCleanup> {
  const module = await loadModule()

  if (activeCleanup) {
    await Promise.resolve(activeCleanup())
    activeCleanup = undefined
  }

  if (typeof module.openEditor !== 'function') {
    return undefined
  }

  const session = await module.openEditor(detail)
  const cleanup: EditorCleanup = typeof session === 'function'
    ? session
    : session && typeof session === 'object' && 'close' in session && typeof session.close === 'function'
      ? () => session.close()
      : undefined

  if (cleanup) {
    activeCleanup = cleanup
  }

  return cleanup
}

export async function shutdownEditor(): Promise<void> {
  if (activeCleanup) {
    const cleanup = activeCleanup
    activeCleanup = undefined
    await Promise.resolve(cleanup())
  }

  if (!editorModulePromise) {
    return
  }

  const module = await editorModulePromise
  editorModulePromise = undefined

  if (typeof module.shutdownEditor === 'function') {
    await module.shutdownEditor()
  }
}
